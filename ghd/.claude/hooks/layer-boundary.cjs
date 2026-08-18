#!/usr/bin/env node
'use strict';

/**
 * PostToolUse hook for Edit/Write — enforces the GHD layer boundary from CLAUDE.md §3:
 * controllers/** must never import repositories.* directly (must go through a Service).
 * Verified against the codebase when this kit was built: 0 existing violations, so this
 * is a real, currently-true invariant, not an aspirational one.
 *
 * PostToolUse cannot undo the write, but it can stop the turn and hand the violation
 * back to Claude as a hard interrupt (`continue: false` + `systemMessage`), so it can't
 * be silently missed — the agent must fix it before moving on.
 */

const fs = require('fs');

function readStdin() {
  const chunks = [];
  const buf = Buffer.alloc(65536);
  while (true) {
    let bytesRead;
    try {
      bytesRead = fs.readSync(0, buf, 0, buf.length, null);
    } catch (e) {
      if (e.code === 'EAGAIN') continue;
      break;
    }
    if (bytesRead <= 0) break;
    chunks.push(Buffer.from(buf.subarray(0, bytesRead)));
  }
  return Buffer.concat(chunks).toString('utf8');
}

function pass() {
  process.exit(0);
}

function violate(reason) {
  const payload = {
    hookSpecificOutput: { hookEventName: 'PostToolUse' },
    systemMessage: reason,
    continue: false,
  };
  process.stdout.write(JSON.stringify(payload));
  process.exit(0);
}

function main() {
  const raw = readStdin();
  let input;
  try {
    input = JSON.parse(raw);
  } catch (e) {
    return pass();
  }

  const toolName = input.tool_name || input.toolName;
  if (toolName !== 'Edit' && toolName !== 'Write') return pass();

  const filePath = (input.tool_input && input.tool_input.file_path) || '';
  const isController = /[\\/]controllers[\\/][\s\S]*\.java$/.test(filePath);
  if (!isController) return pass();

  let content;
  try {
    content = fs.readFileSync(filePath, 'utf8');
  } catch (e) {
    return pass();
  }

  const repoImportMatch = content.match(/^import\s+guru\.springframework\.ghd\.repositories\.[A-Za-z0-9_.]+;/m);
  if (repoImportMatch) {
    return violate(
      `GHD layer-boundary: ${filePath} now imports "${repoImportMatch[0]}". ` +
      'Controllers must not depend on repositories directly (CLAUDE.md §3, ' +
      '.claude/skills/spring-boot-layering/SKILL.md). Move the repository call into the ' +
      'relevant Service/ServiceImpl and have the controller call the Service instead, then ' +
      'remove this import.'
    );
  }

  return pass();
}

main();
