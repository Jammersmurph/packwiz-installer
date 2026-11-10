package link.infra.packwiz.installer.ui.gui

import com.formdev.flatlaf.FlatDarkLaf
import java.awt.Color
import java.awt.Font
import javax.swing.*
import javax.swing.border.EmptyBorder

internal object InstallerTheme {
	val background = Color(22, 27, 32)
	val surface = Color(31, 38, 44)
	val accent = Color(132, 204, 118)
	val muted = Color(165, 178, 187)

	fun install() {
		FlatDarkLaf.setup()
		UIManager.put("Panel.background", background)
		UIManager.put("Button.arc", 12)
		UIManager.put("Component.arc", 12)
		UIManager.put("ProgressBar.arc", 12)
		UIManager.put("ProgressBar.foreground", accent)
		UIManager.put("Component.focusColor", accent)
		UIManager.put("Table.rowHeight", 32)
	}

	fun heading(text: String, size: Float = 24f) = JLabel(text).apply {
		font = font.deriveFont(Font.BOLD, size)
	}

	fun caption(text: String) = JLabel(text).apply { foreground = muted }

	fun primary(button: JButton): JButton = button.apply {
		background = accent
		foreground = Color(22, 27, 32)
		font = font.deriveFont(Font.BOLD)
	}

	fun padding(amount: Int) = EmptyBorder(amount, amount, amount, amount)
}
