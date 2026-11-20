package link.infra.packwiz.installer.ui.gui

import org.junit.Assert.*
import org.junit.Test
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class PackIconDecoderTest {
	@Test fun decodesPngJpegGifAndBmpByContent() {
		for (format in listOf("png", "jpeg", "gif", "bmp")) {
			val image = BufferedImage(16, 24, BufferedImage.TYPE_INT_RGB)
			val output = ByteArrayOutputStream()
			assertTrue("No writer for $format", ImageIO.write(image, format, output))
			val decoded = decodePackIcon(output.toByteArray())
			assertNotNull("Could not decode $format", decoded)
			assertEquals(16, decoded!!.width)
			assertEquals(24, decoded.height)
		}
	}

	@Test fun decodesWebpWithoutRequiringPngContents() {
		val data = javaClass.getResourceAsStream("/icons/test.webp")!!.use { it.readBytes() }
		val decoded = decodePackIcon(data)
		assertNotNull(decoded)
		assertEquals(16, decoded!!.width)
		assertEquals(24, decoded.height)
	}
}
