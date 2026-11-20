package link.infra.packwiz.installer.ui.gui

import link.infra.packwiz.installer.ui.IUserInterface
import link.infra.packwiz.installer.ui.data.ExceptionDetails
import link.infra.packwiz.installer.ui.data.IOptionDetails
import link.infra.packwiz.installer.ui.data.InstallProgress
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Test
import java.awt.Component
import java.awt.Container
import java.awt.EventQueue
import java.awt.GraphicsEnvironment
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import java.util.concurrent.CompletableFuture
import javax.swing.*

class GuiTest {
	private fun components(root: Component): List<Component> = listOf(root) +
		if (root is Container) root.components.flatMap { components(it) } else emptyList()

	private fun onUi(test: (GUIHandler) -> Unit) {
		assumeFalse("GUI tests require a display", GraphicsEnvironment.isHeadless())
		val handler = GUIHandler()
		try {
			EventQueue.invokeAndWait { test(handler) }
		} finally {
			handler.dispose()
		}
	}

	@Test fun progressAndActions() = onUi { handler ->
		val window = handler.window
		assertFalse(window.isResizable)
		assertNotNull(window.iconImage)
		assertEquals(InstallerTheme.logo.getWidth(null), InstallerTheme.logo.getHeight(null))
		assertTrue(components(window).filterIsInstance<JLabel>().any {
			it.icon != null && it.accessibleContext.accessibleName == "Packwiz Updater logo"
		})
		window.displayProgress(InstallProgress("Downloading test mod", 3, 10))
		val bar = components(window).filterIsInstance<JProgressBar>().single()
		assertFalse(bar.isIndeterminate)
		assertEquals(10, bar.maximum)
		assertEquals(3, bar.value)
		window.displayProgress(InstallProgress("Checking metadata"))
		assertTrue(bar.isIndeterminate)
		window.showOk(true)
		val buttons = components(window).filterIsInstance<JButton>()
		assertFalse(buttons.single { it.text == "Cancel" }.isVisible)
		buttons.single { it.text == "Continue" }.doClick()
		assertTrue(handler.okButtonPressed)
		window.hideOk()
		assertTrue(buttons.single { it.text == "Cancel" }.isVisible)
	}

	@Test fun completedProgressDoesNotRequireConfirmation() = onUi { handler ->
		val window = handler.window
		window.displayProgress(InstallProgress("Pack updated", 10, 10))
		val buttons = components(window).filterIsInstance<JButton>()
		assertFalse(buttons.single { it.text == "Continue" }.isVisible)
		assertFalse(handler.okButtonPressed)
	}

	@Test fun packTitleUpdatesHeaderAndWindowTitle() {
		assumeFalse("GUI tests require a display", GraphicsEnvironment.isHeadless())
		val handler = GUIHandler()
		try {
			handler.title = "Create VC"
			EventQueue.invokeAndWait {
				assertEquals("Create VC", handler.window.title)
				assertTrue(components(handler.window).filterIsInstance<JLabel>().any { it.text == "Create VC" })
			}
		} finally { handler.dispose() }
	}

	@Test fun packIconReplacesHeaderAndWindowIconWithoutStretching() {
		assumeFalse("GUI tests require a display", GraphicsEnvironment.isHeadless())
		val handler = GUIHandler()
		try {
			val image = BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB)
			val bytes = ByteArrayOutputStream().apply { ImageIO.write(image, "png", this) }.toByteArray()
			handler.setPackIcon(bytes)
			EventQueue.invokeAndWait {
				assertEquals(32, handler.window.iconImage.getWidth(null))
				val logo = components(handler.window).filterIsInstance<JLabel>().single {
					it.accessibleContext.accessibleName == "Modpack icon"
				}
				assertEquals(logo.icon.iconWidth, logo.icon.iconHeight)
			}
		} finally { handler.dispose() }
	}

	@Test fun invalidAndHugeIconsKeepDefaultLogo() {
		assumeFalse("GUI tests require a display", GraphicsEnvironment.isHeadless())
		val handler = GUIHandler()
		try {
			handler.setPackIcon("not a PNG".toByteArray())
			val huge = BufferedImage(2049, 1, BufferedImage.TYPE_INT_ARGB)
			handler.setPackIcon(ByteArrayOutputStream().apply { ImageIO.write(huge, "png", this) }.toByteArray())
			EventQueue.invokeAndWait {
				assertSame(InstallerTheme.logo, handler.window.iconImage)
				assertTrue(components(handler.window).filterIsInstance<JLabel>().any {
					it.accessibleContext.accessibleName == "Packwiz Updater logo"
				})
			}
		} finally { handler.dispose() }
	}

	@Test fun filteredSelectionSavesCorrectMod() = onUi { handler ->
		fun option(label: String) = object : IOptionDetails {
			override val name = label
			override val optionDescription = "$label description"
			override var optionValue = false
		}
		val mods = listOf(option("Sodium"), option("Iris [Shaders]"))
		val result = CompletableFuture<Boolean>()
		val dialog = OptionsSelectWindow(mods, result, handler.window)
		try {
			val children = components(dialog)
			children.filterIsInstance<JTextField>().single().text = "[Shaders]"
			val table = children.filterIsInstance<JTable>().single()
			assertEquals(1, table.rowCount)
			table.setRowSelectionInterval(0, 0)
			assertTrue(children.filterIsInstance<JTextArea>().single().text.contains("Iris"))
			table.setValueAt(true, 0, 0)
			children.filterIsInstance<JButton>().single { it.text == "Save selection" }.doClick()
			assertFalse(result.get())
			assertFalse(mods[0].optionValue)
			assertTrue(mods[1].optionValue)
		} finally { dialog.dispose() }
	}

	@Test fun errorsShowDetailsAndContinue() = onUi { handler ->
		val result = CompletableFuture<IUserInterface.ExceptionListResult>()
		val dialog = ExceptionListWindow(listOf(ExceptionDetails("Test mod", Exception("Test failure"))), result, 5, false, handler.window)
		try {
			val children = components(dialog)
			children.filterIsInstance<JList<*>>().single().selectedIndex = 0
			assertTrue(children.filterIsInstance<JTextArea>().single().text.contains("Test failure"))
			assertFalse(children.filterIsInstance<JButton>().single { it.text == "Ignore update" }.isEnabled)
			children.filterIsInstance<JButton>().single { it.text == "Continue" }.doClick()
			assertEquals(IUserInterface.ExceptionListResult.CONTINUE, result.get())
		} finally { dialog.dispose() }
	}
}
