package link.infra.packwiz.installer.ui.gui

import com.formdev.flatlaf.FlatDarkLaf
import java.awt.Color
import java.awt.Font
import java.awt.Image
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.EmptyBorder

internal object InstallerTheme {
	val background = Color(26, 23, 38)
	val surface = Color(39, 34, 55)
	val accent = Color(189, 161, 255)
	val muted = Color(185, 175, 207)
	val logo: Image by lazy {
		ImageIO.read(requireNotNull(InstallerTheme::class.java.getResource("/branding/packupdater.png")) {
			"Missing bundled PackUpdater logo"
		})
	}

	fun logoLabel() = JLabel(ImageIcon(logo.getScaledInstance(180, 101, Image.SCALE_SMOOTH))).apply {
		accessibleContext.accessibleName = "Packwiz Updater logo"
	}

	fun install() {
		// The bootstrapper loads this JAR in a child URLClassLoader, while Swing's
		// event thread keeps the parent loader. Explicitly tell UIDefaults where
		// to resolve FlatLaf's UI delegates, including widgets created later.
		UIManager.put("ClassLoader", InstallerTheme::class.java.classLoader)
		val theme = FlatDarkLaf().apply {
			setExtraDefaults(mapOf(
				"@accentColor" to "#bda1ff",
				"@background" to "#1a1726",
				"@foreground" to "#f1ecff",
				"@selectionBackground" to "#514176",
				"@selectionForeground" to "#ffffff"
			))
		}
		UIManager.setLookAndFeel(theme)
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
		foreground = InstallerTheme.background
		font = font.deriveFont(Font.BOLD)
	}

	fun padding(amount: Int) = EmptyBorder(amount, amount, amount, amount)
}
