package xenus.xenaddons.render

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class XenFont(
    val name: String,
    inputStream: InputStream
) {
    private val cachedBytes: ByteArray = inputStream.use { it.readBytes() }

    fun buffer(): ByteBuffer {
        return ByteBuffer.allocateDirect(cachedBytes.size)
            .order(ByteOrder.nativeOrder())
            .put(cachedBytes)
            .flip() as ByteBuffer
    }

    override fun hashCode(): Int = name.hashCode()

    override fun equals(other: Any?): Boolean =
        other is XenFont && name == other.name
}
