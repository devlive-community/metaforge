package org.devlive.metaforge.test.metaforge.support

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

/**
 * 夹在驱动与真实数据库容器之间的 TCP 转发，用来制造真实的网络故障：[dropAll] 切断所有现有连接，
 * [down] 期间新连接一进来就被关掉。驱动看到的就是生产环境里断网或数据库重启时的那种错误。
 *
 * A TCP forwarder between a driver and a real database container: [dropAll] cuts every open connection
 * and new connections are closed at once while [down], so the driver sees real network failures.
 */
@SuppressFBWarnings(value = ["RV_RETURN_VALUE_IGNORED_BAD_PRACTICE", "UNENCRYPTED_SOCKET", "UNENCRYPTED_SERVER_SOCKET"])
class ChaosProxy(private val host: String, private val targetPort: Int) : AutoCloseable
{
    private val server = ServerSocket(0)
    private val sockets: MutableSet<Socket> = ConcurrentHashMap.newKeySet()

    @Volatile
    var down: Boolean = false

    /** 当前正被转发的连接数 / the connections currently being forwarded */
    val openConnections: Int
        get() = sockets.size

    val port: Int
        get() = server.localPort

    init
    {
        thread(isDaemon = true, name = "chaos-proxy-$port") {
            while (!server.isClosed)
            {
                val client = try
                {
                    server.accept()
                }
                catch (ex: java.io.IOException)
                {
                    break
                }
                if (down)
                {
                    client.close()
                    continue
                }
                val upstream = try
                {
                    Socket(host, targetPort)
                }
                catch (ex: java.io.IOException)
                {
                    client.close()
                    continue
                }
                client.tcpNoDelay = true
                upstream.tcpNoDelay = true
                sockets.add(client)
                sockets.add(upstream)
                pump(client, upstream)
                pump(upstream, client)
            }
        }
    }

    private fun pump(from: Socket, to: Socket)
    {
        thread(isDaemon = true) {
            try
            {
                val buffer = ByteArray(64 * 1024)
                val input = from.getInputStream()
                val output = to.getOutputStream()
                while (true)
                {
                    val read = input.read(buffer)
                    if (read < 0)
                    {
                        break
                    }
                    output.write(buffer, 0, read)
                    output.flush()
                }
            }
            catch (ex: java.io.IOException)
            {
                // 连接被切断 / the connection was cut
            }
            closeQuietly(from)
            closeQuietly(to)
        }
    }

    /** 切断所有现有连接 / cut every open connection */
    fun dropAll()
    {
        sockets.toList().forEach { closeQuietly(it) }
    }

    private fun closeQuietly(socket: Socket)
    {
        sockets.remove(socket)
        try
        {
            socket.close()
        }
        catch (ex: java.io.IOException)
        {
            // 已经关闭 / already closed
        }
    }

    override fun close()
    {
        server.close()
        dropAll()
    }
}
