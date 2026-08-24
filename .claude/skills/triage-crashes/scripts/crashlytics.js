#!/usr/bin/env node
// Bridges the Firebase MCP server's Crashlytics tools to the command line.
//
//   node crashlytics.js <repo-root> <calls.json>
//
// <calls.json> is an array of { "name": "<tool>", "arguments": { ... } } objects,
// executed in order against one long-lived `firebase experimental:mcp` process.

const { spawn } = require('child_process');
const fs = require('fs');

const cwd = process.argv[2];
const callsFile = process.argv[3];

if (!cwd || !callsFile) {
  console.error('usage: node crashlytics.js <repo-root> <calls.json>');
  process.exit(2);
}

const calls = JSON.parse(fs.readFileSync(callsFile, 'utf8'));
const isWindows = process.platform === 'win32';
const child = isWindows
  ? spawn(process.env.ComSpec || 'cmd.exe', ['/c', 'firebase', 'experimental:mcp', '--only', 'crashlytics'], { cwd })
  : spawn('firebase', ['experimental:mcp', '--only', 'crashlytics'], { cwd });

let failed = false;
let buffer = '';
let index = 0;

const sendNextCall = () => {
  if (index >= calls.length) {
    child.kill();
    process.exit(failed ? 1 : 0);
  }
  const call = calls[index];
  console.log('\n===== ' + call.name + ' ' + JSON.stringify(call.arguments || {}));
  child.stdin.write(JSON.stringify({ jsonrpc: '2.0', id: 100 + index, method: 'tools/call', params: call }) + '\n');
};

child.stdout.on('data', (chunk) => {
  buffer += chunk.toString();
  let newline;
  while ((newline = buffer.indexOf('\n')) >= 0) {
    const line = buffer.slice(0, newline).trim();
    buffer = buffer.slice(newline + 1);
    if (!line) continue;

    let message;
    try {
      message = JSON.parse(line);
    } catch {
      continue;
    }

    if (message.id === 1) {
      child.stdin.write(JSON.stringify({ jsonrpc: '2.0', method: 'notifications/initialized' }) + '\n');
      sendNextCall();
    } else if (message.id >= 100) {
      const result = message.result || message.error;
      if (message.error || (result && result.isError)) failed = true;
      const text = result && result.content
        ? result.content.map((part) => part.text).join('\n')
        : JSON.stringify(result, null, 2);
      console.log(text);
      index++;
      sendNextCall();
    }
  }
});

child.stderr.on('data', (chunk) => process.stderr.write(chunk));
child.on('error', (error) => {
  console.error('failed to start the Firebase CLI: ' + error.message);
  process.exit(2);
});

child.stdin.write(JSON.stringify({
  jsonrpc: '2.0',
  id: 1,
  method: 'initialize',
  params: { protocolVersion: '2024-11-05', capabilities: {}, clientInfo: { name: 'crashlytics-skill', version: '1' } },
}) + '\n');

setTimeout(() => {
  console.error('timed out waiting for the Firebase MCP server');
  child.kill();
  process.exit(2);
}, 180000);
