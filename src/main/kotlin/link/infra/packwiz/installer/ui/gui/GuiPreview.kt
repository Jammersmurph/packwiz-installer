@file:JvmName("GuiPreview")

package link.infra.packwiz.installer.ui.gui

import link.infra.packwiz.installer.ui.data.InstallProgress
import link.infra.packwiz.installer.ui.data.IOptionDetails
import link.infra.packwiz.installer.ui.data.ExceptionDetails
import link.infra.packwiz.installer.ui.IUserInterface
import java.awt.EventQueue
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.util.concurrent.CompletableFuture
import javax.swing.*

/** A standalone, offline showcase: never invokes UpdateManager or writes pack files. */
fun main() {
	val handler = GUIHandler()
	EventQueue.invokeLater {
		val window = handler.window
		window.title = "Overworld Adventures · Preview"
		val mods = listOf(
			previewOption("Sodium", "A faster rendering engine for smoother exploration.", true),
			previewOption("Iris Shaders", "Bring your world to life with shader-pack support.", true),
			previewOption("JourneyMap", "Map your travels and mark your favorite places.", false),
			previewOption("AppleSkin", "See hunger and saturation information before your next expedition.", true)
		)
		val controls = JFrame("Packwiz · Preview controls (no files changed)").apply {
			defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
			add(JPanel(FlowLayout()).apply {
				add(JButton("Optional mods").apply {
					addActionListener { OptionsSelectWindow(mods, CompletableFuture<Boolean>(), window).isVisible = true }
				})
				add(JButton("Download errors").apply {
					addActionListener {
						ExceptionListWindow(listOf(ExceptionDetails("Example mod", java.io.IOException("Simulated connection timeout; no download was attempted."))),
							CompletableFuture<IUserInterface.ExceptionListResult>(), 24, true, window).isVisible = true
					}
				})
			}, BorderLayout.CENTER)
			pack()
			setLocation(window.x, window.y + window.height + 16)
		}
		var step = 0
		val timer = Timer(450, null)
		timer.addActionListener {
			if (handler.cancelButtonPressed) {
				timer.stop()
				window.dispose()
				controls.dispose()
				return@addActionListener
			}
			// Swing timers also run inside modal dialogs; pause the simulation while choosing mods.
			if (window.ownedWindows.any { it.isVisible }) return@addActionListener
			if (handler.optionsButtonPressed) {
				handler.optionsButtonPressed = false
				OptionsSelectWindow(mods, CompletableFuture<Boolean>(), window).isVisible = true
			}
			if (step >= 24) return@addActionListener
			step++
			window.displayProgress(InstallProgress(
				if (step < 24) "Installing ${mods[(step - 1) % mods.size].name}…" else "Your pack is ready. Happy exploring!", step, 24))
			if (step == 24) {
				timer.stop()
				window.dispose()
				controls.dispose()
			}
		}
		window.addWindowListener(object : java.awt.event.WindowAdapter() {
			override fun windowClosed(e: java.awt.event.WindowEvent) { timer.stop(); controls.dispose() }
		})
		window.isVisible = true
		controls.isVisible = true
		timer.start()
	}
}

private fun previewOption(label: String, description: String, enabled: Boolean) = object : IOptionDetails {
	override val name = label
	override val optionDescription = description
	override var optionValue = enabled
}
