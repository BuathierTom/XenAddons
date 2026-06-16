package xenus.xenaddons.render

import org.lwjgl.system.MemoryUtil
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.file.Files

class XenImage(
    val identifier: String,
    val isSvg: Boolean = identifier.endsWith(".svg", true),
    private val streamSupplier: () -> InputStream = { openStream(identifier) }
) {
    private var buffer: ByteBuffer? = null

    fun buffer(): ByteBuffer {
        val existing = buffer
        if (existing != null) return existing

        val bytes = streamSupplier().use { it.readBytes() }
        val created = MemoryUtil.memAlloc(bytes.size).put(bytes).flip() as ByteBuffer
        buffer = created
        return created
    }

    fun openFreshStream(): InputStream = streamSupplier()

    override fun equals(other: Any?): Boolean = other is XenImage && identifier == other.identifier

    override fun hashCode(): Int = identifier.hashCode()

    companion object {
        private fun openStream(path: String): InputStream {
            val trimmed = path.trim()
            val file = File(trimmed)
            if (file.exists() && file.isFile) {
                return Files.newInputStream(file.toPath())
            }

            return XenImage::class.java.getResourceAsStream(trimmed)
                ?: throw FileNotFoundException(trimmed)
        }
    }
}
