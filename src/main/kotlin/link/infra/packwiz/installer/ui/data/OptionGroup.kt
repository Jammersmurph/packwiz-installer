package link.infra.packwiz.installer.ui.data

/** Members are references to existing optional choices, never additional install tasks. */
data class OptionGroup(
	val id: String,
	val name: String,
	val description: String = "",
	val options: List<IOptionDetails>
)
