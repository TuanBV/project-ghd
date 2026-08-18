#!/usr/bin/env node
'use strict';

/**
 * PreToolUse hook for the Bash tool — real enforcement of GHD's git workflow rules
 * (see CLAUDE.md section 4), not just documentation. Reads the pending command from
 * stdin JSON and denies it before it runs when it violates one of the rules below.
 *
 * Rules enforced here (in order):
 *   1. No `git push --force` / `-f` / `--force-with-lease` — ever, no bypass.
 *   2. No `git commit` while checked out on main/master — must use a feature branch.
 *   3. No `git push` that targets main/master directly — merge via PR instead.
 *   4. No `git commit --amend` / `git rebase` on a commit already reachable from an
 *      `origin/*` ref (rewriting published history).
 *   5. Commit subject must follow Conventional Commits
 *      (feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert(scope)?: ...).
 *   6. Commit message must not contain a Claude/Anthropic Co-Authored-By line.
 *
 * If the commit message can't be confidently extracted from the command string
 * (unusual quoting), rules 5/6 are skipped for that call rather than false-denying —
 * this is a known limitation, not a silent bypass: it only widens what's *allowed*,
 * never what's *denied*.
 */

const { execSync } = require('child_process');

function readStdin() {
  const chunks = [];
  const fd = 0;
  const buf = Buffer.alloc(65536);
  const fs = require('fs');
  while (true) {
    let bytesRead;
    try {
      bytesRead = fs.readSync(fd, buf, 0, buf.length, null);
    } catch (e) {
      if (e.code === 'EAGAIN') continue;
      break;
    }
    if (bytesRead <= 0) break;
    chunks.push(Buffer.from(buf.subarray(0, bytesRead)));
  }
  return Buffer.concat(chunks).toString('utf8');
}

function deny(reason) {
  process.stdout.write(JSON.stringify({
    hookSpecificOutput: {
      hookEventName: 'PreToolUse',
      permissionDecision: 'deny',
      permissionDecisionReason: reason,
    },
  }));
  process.exit(0);
}

function allow() {
  process.exit(0);
}

function git(cwd, args) {
  try {
    return execSync(`git ${args}`, { cwd, encoding: 'utf8', stdio: ['ignore', 'pipe', 'ignore'] }).trim();
  } catch (e) {
    return null;
  }
}

function currentBranch(cwd) {
  return git(cwd, 'rev-parse --abbrev-ref HEAD');
}

function isAlreadyPublished(cwd, ref) {
  const remoteRefsRaw = git(cwd, "for-each-ref --format=%(refname) refs/remotes");
  if (!remoteRefsRaw) return false;
  const remoteRefs = remoteRefsRaw.split('\n').filter(Boolean);
  for (const remoteRef of remoteRefs) {
    const result = git(cwd, `merge-base --is-ancestor ${ref} ${remoteRef} && echo YES`);
    if (result === 'YES') return true;
  }
  return false;
}

// Extract the commit subject line from a `git commit ...` shell command, handling
// both `-m "..."` and the heredoc pattern (`-m "$(cat <<'EOF' ... EOF)")`) that
// Claude Code itself is instructed to use for multi-line commit messages.
function extractCommitSubject(command) {
  const heredocMatch = command.match(/<<-?\s*['"]?EOF['"]?\s*\n([\s\S]*?)\nEOF/);
  if (heredocMatch) {
    const body = heredocMatch[1].split('\n').map((l) => l.trim()).filter(Boolean);
    if (body.length > 0) return body[0];
  }
  const dq = command.match(/-m\s+"((?:[^"\\]|\\.)*)"/);
  if (dq) return dq[1].split('\\n')[0].trim();
  const sq = command.match(/-m\s+'([^']*)'/);
  if (sq) return sq[1].split('\n')[0].trim();
  return null;
}

function extractFullMessage(command) {
  const heredocMatch = command.match(/<<-?\s*['"]?EOF['"]?\s*\n([\s\S]*?)\nEOF/);
  if (heredocMatch) return heredocMatch[1];
  const dq = command.match(/-m\s+"((?:[^"\\]|\\.)*)"/);
  if (dq) return dq[1];
  const sq = command.match(/-m\s+'([^']*)'/);
  if (sq) return sq[1];
  return command;
}

const CONVENTIONAL_RE = /^(feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert)(\([\w.\/-]+\))?!?:\s+\S.+/;
const AI_ATTRIBUTION_RE = /co-authored-by:.*(claude|anthropic)/i;

function main() {
  const raw = readStdin();
  let input;
  try {
    input = JSON.parse(raw);
  } catch (e) {
    return allow();
  }

  const toolName = input.tool_name || input.toolName;
  if (toolName !== 'Bash') return allow();

  const command = (input.tool_input && input.tool_input.command) || '';
  if (!/\bgit\b/.test(command)) return allow();

  const cwd = input.cwd || process.cwd();

  const isPush = /\bgit\s+push\b/.test(command);
  const isCommit = /\bgit\s+commit\b/.test(command);
  const isAmend = /\bgit\s+commit\b[^\n]*--amend\b/.test(command);
  const isRebase = /\bgit\s+rebase\b/.test(command) && !/--abort\b/.test(command);

  // Rule 1: force-push, no exceptions.
  if (isPush && /(--force\b|--force-with-lease\b|(^|\s)-f(\s|$))/.test(command)) {
    return deny(
      'GHD git-safety: force-push is blocked unconditionally in this repo (CLAUDE.md §4). ' +
      'If history genuinely needs correcting, ask the user how they want to handle it.'
    );
  }

  // Rule 3: push straight to main/master.
  if (isPush) {
    const targetsMain = /\borigin\s+main\b|\borigin\s+master\b|HEAD:main\b|HEAD:master\b/.test(command);
    const branch = currentBranch(cwd);
    const bareGitPush = /^\s*git\s+push\s*$/.test(command.trim()) || /^\s*git\s+push\s+origin\s*$/.test(command.trim());
    if (targetsMain || (bareGitPush && (branch === 'main' || branch === 'master'))) {
      return deny(
        'GHD git-safety: direct push to main/master is blocked (CLAUDE.md §4). ' +
        'Push a feature branch and open a Pull Request instead (e.g. `gh pr create`).'
      );
    }
  }

  // Rule 2: commit directly on main/master.
  if (isCommit) {
    const branch = currentBranch(cwd);
    if (branch === 'main' || branch === 'master') {
      return deny(
        `GHD git-safety: direct commit on "${branch}" is blocked (CLAUDE.md §4). ` +
        'Create a feature branch first, e.g. `git checkout -b feat/short-description`.'
      );
    }
  }

  // Rule 4: rewriting already-published history.
  if (isAmend || isRebase) {
    if (isAlreadyPublished(cwd, 'HEAD')) {
      return deny(
        'GHD git-safety: this would rewrite a commit that is already published on a remote ' +
        'branch (CLAUDE.md §4). Rewriting published history is blocked — use a new commit instead, ' +
        'or ask the user how they want to proceed.'
      );
    }
  }

  // Rules 5/6 only apply to commit invocations where we can confidently read the message.
  if (isCommit) {
    const subject = extractCommitSubject(command);
    const fullMessage = extractFullMessage(command);

    if (fullMessage && AI_ATTRIBUTION_RE.test(fullMessage)) {
      return deny(
        'GHD git-safety: commit message contains a Claude/Anthropic Co-Authored-By line, which ' +
        'this project blocks (CLAUDE.md §4). Remove that line and commit again.'
      );
    }

    if (subject && !CONVENTIONAL_RE.test(subject)) {
      return deny(
        'GHD git-safety: commit subject does not follow Conventional Commits (CLAUDE.md §4). ' +
        `Got: "${subject}". Expected: type(scope?): description, type one of ` +
        'feat|fix|docs|style|refactor|perf|test|build|ci|chore|revert.'
      );
    }
  }

  return allow();
}

main();
