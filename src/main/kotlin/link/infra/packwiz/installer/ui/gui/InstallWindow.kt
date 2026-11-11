package link.infra.packwiz.installer.ui.gui

import link.infra.packwiz.installer.ui.data.InstallProgress
import java.awt.*
import javax.swing.*

class InstallWindow(private val handler: GUIHandler) : JFrame() {
	private val lblProgresslabel = JLabel("Preparing your modpack…")
	private val progressBar = JProgressBar().apply { isIndeterminate = true }
	private val progressCount = InstallerTheme.caption("Getting ready")
	private val packTitle = InstallerTheme.heading("Modpack Updater")
	private val activity = JTextArea().apply {
		isEditable = false
		font = Font(Font.MONOSPACED, Font.PLAIN, 12)
		background = InstallerTheme.surface
		border = InstallerTheme.padding(12)
	}
	private val activityPane = JScrollPane(activity).apply {
		preferredSize = Dimension(600, 170)
		isVisible = false
	}
	private val btnOptions = JButton("Optional mods…").apply {
		addActionListener {
			text = "Loading options…"
			isEnabled = false
			handler.optionsButtonPressed = true
		}
	}
	private val btnCancel = JButton("Cancel").apply {
		addActionListener {
			isEnabled = false
			handler.cancelButtonPressed = true
		}
	}
	private val btnOk = InstallerTheme.primary(JButton("Continue")).apply {
		isVisible = false
		addActionListener { handler.okButtonPressed = true }
	}

	init {
		defaultCloseOperation = EXIT_ON_CLOSE
		// Restore the original installer's fixed-size hints for tiling window managers.
		isResizable = false
		minimumSize = Dimension(680, 370)
		contentPane = JPanel(BorderLayout(0, 24)).apply {
			border = InstallerTheme.padding(28)
			add(JPanel(BorderLayout(18, 0)).apply {
				add(object : JComponent() {
					init { preferredSize = Dimension(64, 64) }
					override fun paintComponent(g: Graphics) {
						super.paintComponent(g)
						g.color = InstallerTheme.accent
						g.fillRect(4, 4, 52, 16)
						g.color = Color(87, 120, 75)
						g.fillRect(4, 20, 52, 36)
						g.color = Color(57, 78, 53)
						g.fillRect(16, 28, 12, 12)
						g.fillRect(40, 44, 16, 12)
					}
				}, BorderLayout.WEST)
				add(JPanel(GridLayout(3, 1, 0, 4)).apply {
					add(InstallerTheme.caption("PACKWIZ  /  MODPACK INSTALLER"))
					add(packTitle)
					add(InstallerTheme.caption("A fresh world starts with an up-to-date pack."))
				}, BorderLayout.CENTER)
			}, BorderLayout.NORTH)
			add(JPanel(BorderLayout(0, 12)).apply {
				add(JPanel(BorderLayout(0, 12)).apply {
					background = InstallerTheme.surface
					border = InstallerTheme.padding(20)
					add(lblProgresslabel, BorderLayout.NORTH)
					add(progressBar.apply { preferredSize = Dimension(500, 10) }, BorderLayout.CENTER)
					add(progressCount, BorderLayout.SOUTH)
				}, BorderLayout.NORTH)
				add(activityPane, BorderLayout.CENTER)
			}, BorderLayout.CENTER)
			add(JPanel(BorderLayout()).apply {
				add(JButton("Show activity").apply {
					addActionListener {
						activityPane.isVisible = !activityPane.isVisible
						text = if (activityPane.isVisible) "Hide activity" else "Show activity"
						if (activityPane.isVisible && this@InstallWindow.height < 520) {
							this@InstallWindow.setSize(this@InstallWindow.width, 540)
						}
						this@InstallWindow.revalidate()
					}
				}, BorderLayout.WEST)
				add(JPanel(FlowLayout(FlowLayout.RIGHT, 8, 0)).apply {
					add(btnOptions)
					add(btnCancel)
					add(btnOk)
				}, BorderLayout.EAST)
			}, BorderLayout.SOUTH)
		}
		addPropertyChangeListener("title") {
			packTitle.text = if (title == "packwiz-installer") "Modpack Updater" else title
		}
		pack()
		setSize(740, 380)
		setLocationRelativeTo(null)
	}

	fun displayProgress(progress: InstallProgress) {
		progressBar.isIndeterminate = !progress.hasProgress
		if (progress.hasProgress) {
			progressBar.maximum = progress.progressTotal
			progressBar.value = progress.progress
			progressCount.text = "${progress.progress} / ${progress.progressTotal} files"
		} else {
			progressCount.text = "Working on it…"
		}
		lblProgresslabel.text = progress.message
		// Keep the UI log bounded on large packs.
		if (activity.lineCount > 500) activity.replaceRange("", 0, activity.getLineEndOffset(100))
		activity.append(progress.message + "\n")
		activity.caretPosition = activity.document.length
	}

	fun disableOptionsButton(hasOptions: Boolean) {
		btnOptions.text = if (hasOptions) "Optional mods…" else "No optional mods"
		btnOptions.isEnabled = false
	}

	fun showOk(hideCancel: Boolean) {
		btnOk.text = "Continue"
		btnOk.isVisible = true
		btnCancel.isVisible = !hideCancel
		rootPane.defaultButton = btnOk
	}

	fun hideOk() {
		btnOk.isVisible = false
		btnCancel.isVisible = true
		rootPane.defaultButton = null
	}

	fun timeoutOk(remaining: Long) {
		btnOk.text = "Continue ($remaining)"
	}
}
