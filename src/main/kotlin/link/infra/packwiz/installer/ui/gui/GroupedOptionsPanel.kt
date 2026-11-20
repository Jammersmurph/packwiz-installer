package link.infra.packwiz.installer.ui.gui

import link.infra.packwiz.installer.ui.data.IOptionDetails
import link.infra.packwiz.installer.ui.data.OptionGroup
import java.awt.BorderLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.AbstractTableModel
import javax.swing.table.TableRowSorter
import java.util.regex.Pattern

/** Edits only temporary choices owned by OptionsSelectWindow; Cancel never applies them. */
internal class GroupedOptionsPanel(
	groups: List<OptionGroup>,
	temporary: Map<IOptionDetails, OptionTempHandler>,
	private val search: JTextField,
	private val description: JTextArea
) : JPanel(GridBagLayout()) {
	private val views = mutableListOf<GroupView>()

	init {
		val assigned = mutableSetOf<IOptionDetails>()
		for (group in groups) {
			val members = group.options.filter { it in temporary && assigned.add(it) }.map { temporary.getValue(it) }
			if (members.isNotEmpty()) views.add(GroupView(group.name, group.description, members))
		}
		val remaining = temporary.filterKeys { it !in assigned }.values.toList()
		if (remaining.isNotEmpty()) views.add(GroupView("Other optional mods", "", remaining))
		for ((index, view) in views.withIndex()) {
			add(view.section, GridBagConstraints().apply {
				gridx = 0; gridy = index; weightx = 1.0
				fill = GridBagConstraints.HORIZONTAL
				anchor = GridBagConstraints.NORTHWEST
			})
		}
		add(JPanel(), GridBagConstraints().apply {
			gridx = 0; gridy = views.size; weightx = 1.0; weighty = 1.0
			fill = GridBagConstraints.BOTH
		})
		search.document.addDocumentListener(object : DocumentListener {
			override fun insertUpdate(e: DocumentEvent) = filter()
			override fun removeUpdate(e: DocumentEvent) = filter()
			override fun changedUpdate(e: DocumentEvent) = filter()
		})
		filter()
	}

	fun commitEditing() {
		for (view in views) if (view.table.isEditing) view.table.cellEditor.stopCellEditing()
	}

	private fun refreshCounts() = views.forEach { it.refreshCount() }
	private fun filter() {
		views.forEach { it.filter(search.text.trim()) }
		revalidate()
		repaint()
	}

	private inner class GroupView(
		private val name: String,
		private val details: String,
		private val members: List<OptionTempHandler>
	) {
		val section = JPanel(BorderLayout()).apply { border = InstallerTheme.padding(4) }
		private val toggle = JToggleButton("▸").apply {
			toolTipText = "Expand or collapse ${this@GroupView.name}"
			accessibleContext.accessibleName = "Expand ${this@GroupView.name}"
		}
		private val check = JCheckBox().apply {
			toolTipText = "Enable or disable every mod in ${this@GroupView.name}, including filtered-out mods"
			accessibleContext.accessibleName = this@GroupView.name
		}
		private val model = object : AbstractTableModel() {
			override fun getRowCount() = members.size
			override fun getColumnCount() = 2
			override fun getColumnClass(column: Int): Class<*> =
				if (column == 0) Boolean::class.javaObjectType else String::class.java
			override fun isCellEditable(row: Int, column: Int) = column == 0
			override fun getValueAt(row: Int, column: Int): Any =
				if (column == 0) members[row].optionValue else members[row].name
			override fun setValueAt(value: Any, row: Int, column: Int) {
				if (column == 0) {
					members[row].optionValue = value as Boolean
					fireTableCellUpdated(row, column)
					refreshCounts()
				}
			}
		}
		private val sorter = TableRowSorter(model)
		val table = JTable(model).apply {
			rowSorter = sorter
			tableHeader = null
			setShowGrid(false)
			setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
			columnModel.getColumn(0).maxWidth = 36
			selectionModel.addListSelectionListener {
				if (selectedRow >= 0 && selectedRow < rowCount) {
					description.text = members[convertRowIndexToModel(selectedRow)].optionDescription.ifBlank { details }
					description.caretPosition = 0
				}
			}
		}

		init {
			section.add(JPanel(BorderLayout(8, 0)).apply {
				add(toggle, BorderLayout.WEST)
				add(check, BorderLayout.CENTER)
			}, BorderLayout.NORTH)
			section.add(table, BorderLayout.CENTER)
			table.isVisible = false
			toggle.addActionListener {
				description.text = details.ifBlank { "Select an optional mod to view its description." }
				description.caretPosition = 0
				filter(search.text.trim())
				this@GroupedOptionsPanel.revalidate()
			}
			check.addActionListener {
				val enabled = check.isSelected
				commitEditing()
				members.forEach { it.optionValue = enabled }
				model.fireTableDataChanged()
				refreshCounts()
				description.text = details
			}
			refreshCount()
		}

		fun refreshCount() {
			val enabled = members.count { it.optionValue }
			check.text = "$name ($enabled/${members.size})"
			check.isSelected = enabled == members.size
			check.putClientProperty("JButton.selectedState", if (enabled in 1 until members.size) "indeterminate" else null)
			check.accessibleContext.accessibleDescription = "$enabled of ${members.size} enabled"
		}

		fun filter(query: String) {
			sorter.rowFilter = if (query.isEmpty() || name.contains(query, ignoreCase = true)) null
				else RowFilter.regexFilter("(?i)" + Pattern.quote(query), 1)
			section.isVisible = table.rowCount > 0
			table.isVisible = toggle.isSelected || query.isNotEmpty()
			toggle.text = if (table.isVisible) "▾" else "▸"
		}
	}
}
