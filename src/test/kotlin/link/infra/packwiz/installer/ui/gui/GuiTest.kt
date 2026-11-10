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
