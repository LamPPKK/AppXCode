package dev.appxcode.ide.language

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
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

    @Test
    fun performsInitializeHandshake() {
        val root = Files.createTempDirectory("appxcode-lsp-init")
        val server = root.resolve("fake-lsp.py")
        Files.writeString(server, """#!/usr/bin/env python3
import json, sys
def read_message():
    length = None
    while True:
        line = sys.stdin.buffer.readline()
        if not line: return None
        line = line.decode('ascii').strip()
        if not line: break
        if line.lower().startswith('content-length:'): length = int(line.split(':',1)[1])
    return None if length is None else json.loads(sys.stdin.buffer.read(length))
def send(payload):
    data = json.dumps(payload,separators=(',',':')).encode()
    sys.stdout.buffer.write(f'Content-Length: {len(data)}\r\n\r\n'.encode()+data); sys.stdout.buffer.flush()
while True:
    m=read_message()
    if m is None: break
    if m.get('method') == 'initialize': send({'jsonrpc':'2.0','id':m['id'],'result':{'capabilities':{}}})
    if m.get('method') == 'exit': break
""")
        server.toFile().setExecutable(true)
        LspProcessManager(LspServerConfig(server, root)).use { manager ->
            assertTrue(manager.initialize(root.toUri().toASCIIString()))
        }
    }

    @Test
    fun dispatchesServerNotificationsToListeners() {
        val root = Files.createTempDirectory("appxcode-lsp-notify")
        val server = root.resolve("fake-lsp.py")
        Files.writeString(server, """#!/usr/bin/env python3
import json, sys
def read_message():
    length = None
    while True:
        line = sys.stdin.buffer.readline()
        if not line: return None
        line = line.decode('ascii').strip()
        if not line: break
        if line.lower().startswith('content-length:'): length = int(line.split(':',1)[1])
    return None if length is None else json.loads(sys.stdin.buffer.read(length))
def send(payload):
    data = json.dumps(payload,separators=(',',':')).encode()
    sys.stdout.buffer.write(f'Content-Length: {len(data)}\\r\\n\\r\\n'.encode()+data); sys.stdout.buffer.flush()
while True:
    m=read_message()
    if m is None: break
    if m.get('method') == 'initialize':
        send({'jsonrpc':'2.0','id':m['id'],'result':{'capabilities':{}}})
        send({'jsonrpc':'2.0','method':'window/logMessage','params':{'type':3,'message':'ready'}})
    if m.get('method') == 'exit': break
""")
        server.toFile().setExecutable(true)
        LspProcessManager(LspServerConfig(server, root)).use { manager ->
            val methods = java.util.concurrent.CopyOnWriteArrayList<String>()
            manager.onNotification { method, _ -> methods += method }.use {
                assertTrue(manager.initialize(root.toUri().toASCIIString()))
                repeat(100) {
                    if (methods.isNotEmpty()) return@repeat
                    Thread.sleep(5)
                }
                assertEquals(listOf("window/logMessage"), methods.toList())
            }
        }
    }
}
