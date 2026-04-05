package xenus.xenaddons.gui.odin.ui.rendering

import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Font(
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
        other is Font && name == other.name
}
