package link.infra.packwiz.installer.ui.gui

import link.infra.packwiz.installer.ui.data.MAX_PACK_ICON_BYTES
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import javax.imageio.stream.MemoryCacheImageInputStream

/** Check dimensions before decoding so highly compressed, huge images cannot exhaust memory. */
internal fun decodePackIcon(data: ByteArray): BufferedImage? {
	if (data.isEmpty() || data.size > MAX_PACK_ICON_BYTES) return null
	return MemoryCacheImageInputStream(ByteArrayInputStream(data)).use { input ->
		val readers = ImageIO.getImageReaders(input)
		if (!readers.hasNext()) return null
		val reader = readers.next()
		try {
			if (!reader.formatName.equals("png", ignoreCase = true)) return null
			reader.input = input
			if (reader.getWidth(0) !in 1..2048 || reader.getHeight(0) !in 1..2048) return null
			reader.read(0)
		} finally {
			reader.dispose()
		}
	}
}
