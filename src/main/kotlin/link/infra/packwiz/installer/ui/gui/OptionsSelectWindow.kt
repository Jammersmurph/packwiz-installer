package link.infra.packwiz.installer.ui.gui

import link.infra.packwiz.installer.ui.data.IOptionDetails
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.*
import java.util.concurrent.CompletableFuture
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableRowSorter
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import java.awt.Dimension

class OptionsSelectWindow internal constructor(optList: List<IOptionDetails>, future: CompletableFuture<Boolean>, parentWindow: JFrame?) : JDialog(parentWindow, "Select optional mods...", true), ActionListener {
	private val lblOptionDescription: JTextArea
	private val tableModel: OptionTableModel
	private val future: CompletableFuture<Boolean>

	private class OptionTableModel(givenOpts: List<IOptionDetails>) : AbstractTableModel() {
		private val opts: List<OptionTempHandler>

		init {
			val mutOpts = ArrayList<OptionTempHandler>()
			for (opt in givenOpts) {
				mutOpts.add(OptionTempHandler(opt))
			}
			opts = mutOpts
		}

		override fun getRowCount() = opts.size
		override fun getColumnCount() = 2

		private val columnNames = arrayOf("Enabled", "Mod name")
		private val columnTypes = arrayOf(Boolean::class.javaObjectType, String::class.java)
		private val columnEditables = booleanArrayOf(true, false)

		override fun getColumnName(columnIndex: Int) = columnNames[columnIndex]
		override fun getColumnClass(columnIndex: Int) = columnTypes[columnIndex]
		override fun isCellEditable(rowIndex: Int, columnIndex: Int) = columnEditables[columnIndex]

		override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
			val opt = opts[rowIndex]
			return if (columnIndex == 0) opt.optionValue else opt.name
		}

		override fun setValueAt(aValue: Any, rowIndex: Int, columnIndex: Int) {
			if (columnIndex == 0) {
				val opt = opts[rowIndex]
				opt.optionValue = aValue as Boolean
				fireTableCellUpdated(rowIndex, columnIndex)
			}
		}

		fun getDescription(rowIndex: Int) = opts[rowIndex].optionDescription

		fun finalise() {
			for (opt in opts) {
				opt.finalise()
			}
		}
	}

	override fun actionPerformed(e: ActionEvent) {
		if (e.actionCommand == "OK") {
			tableModel.finalise()
			future.complete(false)
			dispose()
		} else if (e.actionCommand == "Cancel") {
			future.complete(true)
			dispose()
		}
	}

	/**
	 * Create the dialog.
	 */
	init {
		tableModel = OptionTableModel(optList)
		this.future = future

		setBounds(100, 100, 720, 460)
		minimumSize = Dimension(600, 360)
		setLocationRelativeTo(parentWindow)

		contentPane.apply {
			layout = BorderLayout()
			val search = JTextField().apply {
				putClientProperty("JTextField.placeholderText", "Search optional mods…")
				toolTipText = "Filter by mod name"
			}
			add(JPanel(BorderLayout(0, 12)).apply {
				border = InstallerTheme.padding(20)
				add(InstallerTheme.heading("Make it your own"), BorderLayout.NORTH)
				add(InstallerTheme.caption("Choose the optional mods for your adventure."), BorderLayout.CENTER)
				add(search, BorderLayout.SOUTH)
			}, BorderLayout.NORTH)
			add(JPanel().apply {
				border = EmptyBorder(0, 20, 16, 20)
				layout = BorderLayout(0, 0)

				add(JSplitPane().apply {
					resizeWeight = 0.5

					lblOptionDescription = JTextArea("Select an option...").apply {
						background = UIManager.getColor("List.background")
						isOpaque = true
						wrapStyleWord = true
						lineWrap = true
						isEditable = false
						isFocusable = false
						font = UIManager.getFont("Label.font")
						border = EmptyBorder(10, 10, 10, 10)
					}

					leftComponent = JScrollPane(JTable().apply {
						showVerticalLines = false
						showHorizontalLines = false
						setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
						setShowGrid(false)
						model = tableModel
						val sorter = TableRowSorter(tableModel)
						rowSorter = sorter
						search.document.addDocumentListener(object : DocumentListener {
							private fun filter() {
								sorter.rowFilter = RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(search.text), 1)
							}
							override fun insertUpdate(e: DocumentEvent) = filter()
							override fun removeUpdate(e: DocumentEvent) = filter()
							override fun changedUpdate(e: DocumentEvent) = filter()
						})
						columnModel.getColumn(0).resizable = false
						columnModel.getColumn(0).preferredWidth = 36
						columnModel.getColumn(0).maxWidth = 36
						columnModel.getColumn(1).resizable = false
						selectionModel.addListSelectionListener {
							val i = selectedRow
							if (i > -1) {
								lblOptionDescription.text = tableModel.getDescription(convertRowIndexToModel(i))
								lblOptionDescription.caretPosition = 0
							} else {
								lblOptionDescription.text = "Select an option..."
							}
						}
						tableHeader = null
					}).apply {
						viewport.background = UIManager.getColor("List.background")
						horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
					}

					rightComponent = JScrollPane(lblOptionDescription)
				})

				add(JPanel().apply {
					layout = FlowLayout(FlowLayout.RIGHT)

					add(InstallerTheme.primary(JButton("Save selection")).apply {
						actionCommand = "OK"
						addActionListener(this@OptionsSelectWindow)

						this@OptionsSelectWindow.rootPane.defaultButton = this
					})

					add(JButton("Cancel").apply {
						actionCommand = "Cancel"
						addActionListener(this@OptionsSelectWindow)
					})
				}, BorderLayout.SOUTH)
			}, BorderLayout.CENTER)
		}

		addWindowListener(object : WindowAdapter() {
			override fun windowClosing(e: WindowEvent) {
				future.complete(true)
			}

			override fun windowClosed(e: WindowEvent) {
				// Just in case closing didn't get triggered - if something else called dispose() the
				// future will have already completed
				future.complete(true)
			}
		})
	}
}
