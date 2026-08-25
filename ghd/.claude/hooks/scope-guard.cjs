#!/usr/bin/env node
'use strict';

/**
 * PreToolUse hook — chặn Read/Glob/Grep/Edit/Write/Bash đọc/ghi DỮ LIỆU NGOÀI cây thư
 * mục `ghd/` (CLAUDE_PROJECT_DIR). Lý do: git repo root thật nằm ở thư mục cha
 * (`project-ghd/`), chứa project `CPMAP/` hoàn toàn không liên quan (xem CLAUDE.md
 * "Lưu ý phạm vi repo") — bộ kit này chỉ được phép hoạt động trong `ghd/`, không được
 * đọc/ghi bất kỳ dữ liệu nào khác trên máy, kể cả project anh em cùng cấp.
 *
 * NGOẠI LỆ (hạ tầng vận hành của Claude Code, không phải "dữ liệu dự án" theo nghĩa
 * người dùng muốn chặn):
 *   - Thư mục temp hệ điều hành (os.tmpdir()) - nơi scratchpad/task-output của session
 *     này nằm, bắt buộc phải đọc/ghi được để các tool khác (Bash run_in_background,
 *     Write vào scratchpad...) hoạt động bình thường.
 *   - `~/.claude/` (settings, plans, memory, skill cache...) - hạ tầng cấu hình Claude
 *     Code của người dùng, không phải dữ liệu dự án khác.
 *
 * GIỚI HẠN ĐÃ BIẾT: với Bash, hook chỉ quét các token trông giống đường dẫn trong chuỗi
 * lệnh bằng regex đơn giản (không parse shell thật) - đây là lớp phòng vệ hợp lý, không
 * tuyệt đối (1 script phức tạp tự dựng đường dẫn từ biến môi trường có thể lách qua).
 * Với Read/Edit/Write/Glob/Grep, chặn dựa trên field path chính xác nên đáng tin cậy.
 */

const fs = require('fs');
const path = require('path');
const os = require('os');

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

function allow() {
  process.exit(0);
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

// baseDir dùng để resolve token tương đối - CỐ Ý truyền tường minh (cwd của lệnh Bash/
// tool call, không phải process.cwd() ngầm định của chính hook) để không phụ thuộc vào
// giả định "hook luôn chạy với cwd = project root" (dù thực tế thường vậy).
function normalize(p, baseDir) {
  return path.resolve(baseDir, p).replace(/\\/g, '/').toLowerCase();
}

function isWithin(candidate, root, baseDir) {
  const c = normalize(candidate, baseDir);
  const r = normalize(root, baseDir);
  return c === r || c.startsWith(r.endsWith('/') ? r : r + '/');
}

function getAllowedRoots(projectRoot) {
  return [projectRoot, os.tmpdir(), path.join(os.homedir(), '.claude')];
}

function isPathAllowed(candidate, allowedRoots, baseDir) {
  return allowedRoots.some((root) => isWithin(candidate, root, baseDir));
}

// Windows drive-letter (C:\..., C:/...) hoặc Git-Bash style (/c/...), hoặc "../"/"./"
// tương đối - đủ rộng để bắt các đường dẫn thường gặp trong lệnh Bash thật của agent.
//
// (?<![A-Za-z]) trước phần drive-letter: không có nó, "http://..."/"https://..." bị
// nhận nhầm thành path Windows vì ký tự cuối của scheme ("p" trong "http") + ":" + "/"
// khớp đúng pattern [A-Za-z]:[\\/] (vd "p://localhost:18080" khớp như thể "p:" là ổ đĩa)
// - phát hiện thật khi curl gọi API local trong lúc test Redis Test Lab bị chặn nhầm.
// Lookbehind này đảm bảo ký tự trước "X:" không phải là 1 chữ cái khác, nên "http:"/
// "https:" (nhiều chữ cái liền trước ":") không còn khớp, còn "C:\..." đứng đầu token
// (không có chữ cái nào trước nó) vẫn khớp như cũ.
const PATH_TOKEN_RE = /(?:(?<![A-Za-z])[A-Za-z]:[\\/][^\s"']*|\/[a-zA-Z][\\/][^\s"']*|\.\.?\/[^\s"']*)/g;
const QUOTED_RE = /"([^"]*)"|'([^']*)'/g;
// Bắt riêng target của "cd" kể cả không có dấu "/" theo sau (vd "cd ..", "cd ../..")
// - PATH_TOKEN_RE ở trên yêu cầu có "/" sau dấu chấm nên bỏ sót trường hợp này.
const CD_TARGET_RE = /\bcd\s+([^\s&|;"']+)/g;

// QUAN TRỌNG: đường dẫn thật của chính project này có khoảng trắng ("Máy tính") - nếu
// không tách chuỗi trong ngoặc kép/đơn ra trước, path-token-regex sẽ cắt đường dẫn tại
// khoảng trắng và luôn báo sai "nằm ngoài phạm vi" cho MỌI lệnh Bash bình thường. Lấy
// nguyên vẹn nội dung trong ngoặc trước (giữ khoảng trắng), rồi mới quét phần còn lại
// (không có ngoặc, theo quy ước shell không có khoảng trắng) bằng regex path-token.
function extractPathTokens(command) {
  const tokens = [];
  let match;
  while ((match = QUOTED_RE.exec(command)) !== null) {
    const inner = match[1] !== undefined ? match[1] : match[2];
    if (inner) tokens.push(inner);
  }
  const withoutQuotes = command.replace(QUOTED_RE, ' ');
  const unquoted = withoutQuotes.match(PATH_TOKEN_RE) || [];
  tokens.push(...unquoted);

  let cdMatch;
  while ((cdMatch = CD_TARGET_RE.exec(withoutQuotes)) !== null) {
    tokens.push(cdMatch[1]);
  }
  return tokens;
}

// Quy đổi kiểu Git-Bash "/c/Users/..." về "C:/Users/..." để so sánh nhất quán.
function mingwToWindows(p) {
  const m = p.match(/^\/([a-zA-Z])\/(.*)$/);
  if (m) {
    return `${m[1]}:/${m[2]}`;
  }
  return p;
}

function main() {
  const raw = readStdin();
  let input;
  try {
    input = JSON.parse(raw);
  } catch (e) {
    return allow();
  }

  const toolName = input.tool_name || input.toolName;
  const projectRoot = process.env.CLAUDE_PROJECT_DIR || input.cwd || process.cwd();
  const baseDir = input.cwd || projectRoot;
  const allowedRoots = getAllowedRoots(projectRoot);

  if (toolName === 'Read' || toolName === 'Edit' || toolName === 'Write' || toolName === 'NotebookEdit') {
    const filePath = input.tool_input && input.tool_input.file_path;
    if (filePath && !isPathAllowed(filePath, allowedRoots, baseDir)) {
      return deny(
        `scope-guard: "${filePath}" nằm ngoài phạm vi ghd/ - bộ kit này chỉ được phép ` +
        'đọc/ghi trong cây thư mục ghd/ (xem CLAUDE.md "Lưu ý phạm vi repo"). Nếu thật ' +
        'sự cần truy cập file này, hỏi người dùng trước.'
      );
    }
    return allow();
  }

  if (toolName === 'Glob' || toolName === 'Grep') {
    const searchPath = input.tool_input && input.tool_input.path;
    if (searchPath && !isPathAllowed(searchPath, allowedRoots, baseDir)) {
      return deny(
        `scope-guard: đường dẫn tìm kiếm "${searchPath}" nằm ngoài phạm vi ghd/ - ` +
        'không được phép quét thư mục khác (vd project CPMAP/ cùng cấp).'
      );
    }
    return allow();
  }

  if (toolName === 'Bash') {
    const command = (input.tool_input && input.tool_input.command) || '';
    const tokens = extractPathTokens(command);
    for (const rawToken of tokens) {
      const token = mingwToWindows(rawToken);
      if (!isPathAllowed(token, allowedRoots, baseDir)) {
        return deny(
          `scope-guard: lệnh có vẻ truy cập đường dẫn "${rawToken}" nằm ngoài phạm vi ` +
          'ghd/ - bộ kit này chỉ được phép thao tác trong cây thư mục ghd/ (xem CLAUDE.md ' +
          '"Lưu ý phạm vi repo"). Nếu thật sự cần, hỏi người dùng trước thay vì lách qua.'
        );
      }
    }
    return allow();
  }

  return allow();
}

main();
