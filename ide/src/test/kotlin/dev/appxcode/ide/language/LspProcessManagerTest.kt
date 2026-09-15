package dev.appxcode.ide.language

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files

class LspProcessManagerTest {
    @Test
    fun exchangesFramedJsonRpcRequests() {
        val root = Files.createTempDirectory("appxcode-lsp-process")
        val server = root.resolve("fake-lsp.py")
        Files.writeString(server, """#!/usr/bin/env python3
import json, sys

def read_message():
    length = None
    while True:
        line = sys.stdin.buffer.readline()
        if not line:
            return None
        line = line.decode('ascii').strip()
        if not line:
            break
        if line.lower().startswith('content-length:'):
            length = int(line.split(':', 1)[1].strip())
    if length is None:
        return None
    return json.loads(sys.stdin.buffer.read(length).decode('utf-8'))

def send(payload):
    data = json.dumps(payload, separators=(',', ':')).encode('utf-8')
    sys.stdout.buffer.write(f'Content-Length: {len(data)}\\r\\n\\r\\n'.encode('ascii'))
    sys.stdout.buffer.write(data)
    sys.stdout.buffer.flush()

while True:
    message = read_message()
    if message is None:
        break
    if 'id' in message:
        send({'jsonrpc':'2.0','id':message['id'],'result':{'method':message.get('method')}})
    if message.get('method') == 'exit':
        break
""")
        server.toFile().setExecutable(true)

        LspProcessManager(LspServerConfig(server, root)).use { manager ->
            val response = manager.request("workspace/symbol", "{\"query\":\"Account\"}", 2_000)
            assertNotNull(response)
            assertTrue(response!!.contains("workspace/symbol"))
            assertTrue(manager.isAlive())
            assertTrue(manager.notify("textDocument/didOpen", "{}"))
        }
    }
}
