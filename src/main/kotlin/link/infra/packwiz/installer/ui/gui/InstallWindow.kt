package link.infra.packwiz.installer.ui.gui

import link.infra.packwiz.installer.Msgs
import link.infra.packwiz.installer.ui.data.InstallProgress
import java.awt.AlphaComposite
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.Graphics
import java.awt.Graphics2D
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.Insets
import java.awt.RenderingHints
import javax.swing.*
import javax.swing.border.EmptyBorder

class InstallWindow(private val handler: GUIHandler) : JFrame() {
    private var lblProgresslabel: JLabel
    private var progressBar: JProgressBar
    private var btnOptions: JButton
    private val btnCancel: JButton
    private val btnOk: JButton
    private val buttonsPanel: JPanel
    private val gifLabel: JLabel
    private val contentPanel: JPanel

    private val backgroundImage: ImageIcon
    private val brandingImage: ImageIcon
    private val gifIcon: ImageIcon

    private val windowWidth = 800
    private val windowHeight = 600

    init {
        backgroundImage = ImageIcon(InstallWindow::class.java.getResource("/CreateVC_SMP.png"))
        brandingImage = ImageIcon(InstallWindow::class.java.getResource("/IMG_4295.PNG"))
        gifIcon = ImageIcon(InstallWindow::class.java.getResource("/CVC_animated.gif"))

        setBounds(100, 100, windowWidth, windowHeight)
        isResizable = false
        defaultCloseOperation = EXIT_ON_CLOSE
        setLocationRelativeTo(null)

        val layeredPane = JLayeredPane()
        layeredPane.preferredSize = Dimension(windowWidth, windowHeight)
        setContentPane(layeredPane)

        // Background layer
        val bgPanel = object : JPanel() {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
                g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
                g2d.drawImage(backgroundImage.image, 0, 0, width, height, this)
            }
        }
        bgPanel.setBounds(0, 0, windowWidth, windowHeight)
        bgPanel.setOpaque(true)
        layeredPane.add(bgPanel, JLayeredPane.DEFAULT_LAYER)

        // Content panel (semi-transparent overlay + branding + gif + progress)
        contentPanel = object : JPanel(null) {
            override fun paintComponent(g: Graphics) {
                super.paintComponent(g)
                val g2d = g as Graphics2D
                // Semi-transparent dark overlay for readability
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f))
                g2d.color = Color.BLACK
                g2d.fillRect(0, 0, width, height)
                g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f))
            }
        }
        contentPanel.setBounds(0, 0, windowWidth, windowHeight)
        contentPanel.setOpaque(false)
        layeredPane.add(contentPanel, JLayeredPane.PALETTE_LAYER)

        // Branding logo
        val logoLabel = JLabel()
        logoLabel.setIcon(ImageIcon(brandingImage.image.getScaledInstance(500, -1, java.awt.Image.SCALE_SMOOTH)))
        logoLabel.setHorizontalAlignment(SwingConstants.CENTER)
        val logoWidth = 500
        val logoHeight = (logoWidth.toFloat() / brandingImage.iconWidth.toFloat() * brandingImage.iconHeight.toFloat()).toInt()
        logoLabel.setBounds((windowWidth - logoWidth) / 2, 30, logoWidth, logoHeight)
        contentPanel.add(logoLabel)

        // Animated GIF (replaces neoforge fox)
        gifLabel = JLabel(gifIcon)
        gifLabel.setHorizontalAlignment(SwingConstants.CENTER)
        val gifWidth = 256
        val gifHeight = (gifWidth.toFloat() / gifIcon.iconWidth.toFloat() * gifIcon.iconHeight.toFloat()).toInt()
        gifLabel.setBounds((windowWidth - gifWidth) / 2, 100 + logoHeight, gifWidth, gifHeight)
        contentPanel.add(gifLabel)

        // Progress bar with styling
        progressBar = JProgressBar().apply {
            isIndeterminate = true
            setBounds(100, 490, 600, 24)
        }
        contentPanel.add(progressBar)

        // Status label
        lblProgresslabel = JLabel(Msgs.hintMore(Msgs.loading()), SwingConstants.CENTER)
        lblProgresslabel.setForeground(Color.WHITE)
        lblProgresslabel.font = Font("SansSerif", Font.PLAIN, 14)
        lblProgresslabel.setBounds(100, 520, 600, 20)
        contentPanel.add(lblProgresslabel)

        // Buttons panel at bottom-right
        buttonsPanel = JPanel()
        buttonsPanel.setLayout(GridBagLayout())
        buttonsPanel.setOpaque(false)
        buttonsPanel.setBounds(windowWidth - 300, 550, 280, 40)

        btnOptions = JButton(Msgs.hintMore(Msgs.optionalMods())).apply {
            alignmentX = Component.CENTER_ALIGNMENT
            addActionListener {
                text = Msgs.hintMore(Msgs.loading())
                isEnabled = false
                handler.optionsButtonPressed = true
            }
        }
        buttonsPanel.add(btnOptions, GridBagConstraints().apply {
            gridx = 0
            gridy = 0
            insets = Insets(0, 0, 0, 5)
        })

        btnCancel = JButton(Msgs.cancel()).apply {
            addActionListener {
                isEnabled = false
                handler.cancelButtonPressed = true
            }
        }
        buttonsPanel.add(btnCancel, GridBagConstraints().apply {
            gridx = 1
            gridy = 0
        })

        btnOk = JButton(Msgs.continueText()).apply {
            addActionListener {
                handler.okButtonPressed = true
            }
        }

        contentPanel.add(buttonsPanel)
    }

    fun displayProgress(progress: InstallProgress) {
        if (progress.hasProgress) {
            progressBar.isIndeterminate = false
            progressBar.value = progress.progress
            progressBar.maximum = progress.progressTotal
        } else {
            progressBar.isIndeterminate = true
            progressBar.value = 0
        }
        lblProgresslabel.text = progress.message
    }

    fun disableOptionsButton(hasOptions: Boolean) {
        btnOptions.apply {
            text = if (hasOptions) Msgs.hintMore(Msgs.optionalMods()) else Msgs.noOptionalMods()
            isEnabled = false
        }
    }

    fun showOk(hideCancel: Boolean) {
        if (hideCancel) {
            buttonsPanel.add(btnOk, GridBagConstraints().apply {
                gridx = 1
                gridy = 0
            })
            buttonsPanel.remove(btnCancel)
        } else {
            buttonsPanel.add(btnOk, GridBagConstraints().apply {
                gridx = 2
                gridy = 0
            })
        }
        buttonsPanel.revalidate()
        buttonsPanel.repaint()
    }

    fun hideOk() {
        buttonsPanel.remove(btnOk)
        if (!buttonsPanel.components.contains(btnCancel)) {
            buttonsPanel.add(btnCancel, GridBagConstraints().apply {
                gridx = 1
                gridy = 0
            })
        }
        buttonsPanel.revalidate()
        buttonsPanel.repaint()
    }

    fun timeoutOk(remaining: Long) {
        btnOk.text = Msgs.countdown(Msgs.continueText(), remaining)
    }
}
