package link.infra.packwiz.installer.metadata

import link.infra.packwiz.installer.target.ClientHolder
import link.infra.packwiz.installer.target.path.PackwizFilePath
import link.infra.packwiz.installer.ui.data.IOptionDetails
import okio.Path.Companion.toPath
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class OptionalGroupsConfigTest {
	@get:Rule val temporary = TemporaryFolder()

	private fun option(label: String) = object : IOptionDetails {
		override val name = label
		override var optionValue = false
		override val optionDescription = ""
	}

	private val config = """
		[[groups]]
		id = "vr"
		name = "VR Support"
		description = "Enable VR integration."
		mods = ["mods/vivecraft.pw.toml", "mods/missing.pw.toml"]
		[[groups]]
		id = "other"
		name = "Other tools"
		mods = ["mods/vivecraft.pw.toml", "mods/map.pw.toml"]
	""".trimIndent()

	@Test fun defaultsToVersionOneAndResolvesOnlyAvailableOptionalMods() {
		val parsed = OptionalGroupsConfig.parse(config)
		assertEquals(1, parsed.version)
		val pack = PackwizFilePath("/pack".toPath(), "nested/pack.toml")
		val vr = option("Vivecraft")
		val map = option("Map")
		val groups = parsed.resolve(pack, mapOf(
			pack.resolve("mods/vivecraft.pw.toml") to vr,
			pack.resolve("mods/map.pw.toml") to map
		))
		assertEquals(listOf("VR Support", "Other tools"), groups.map { it.name })
		assertEquals(listOf(vr), groups[0].options)
		assertEquals(listOf(map), groups[1].options)
		assertFalse(vr.optionValue)
	}

	@Test(expected = IllegalArgumentException::class) fun rejectsUnsupportedVersion() {
		OptionalGroupsConfig.parse("version = 2\n$config")
	}

	@Test(expected = IllegalArgumentException::class) fun rejectsDuplicateIds() {
		OptionalGroupsConfig.parse(config.replace("id = \"other\"", "id = \"vr\""))
	}

	@Test(expected = IllegalArgumentException::class) fun rejectsPathsOutsidePack() {
		OptionalGroupsConfig.parse(config.replace("mods/vivecraft.pw.toml", "../vivecraft.pw.toml"))
	}

	@Test fun missingAndInvalidConfigFallBackAndNestedSiblingLoads() {
		val root = temporary.newFolder()
		val nested = File(root, "nested").apply { mkdirs() }
		val pack = PackwizFilePath(root.absolutePath.toPath(), "nested/pack.toml")
		assertNull(loadOptionalGroups(pack, ClientHolder()))
		val file = File(nested, "optional-groups.toml")
		file.writeText("not valid TOML {{{")
		assertNull(loadOptionalGroups(pack, ClientHolder()))
		file.writeText(config)
		assertEquals("VR Support", loadOptionalGroups(pack, ClientHolder())!!.groups.first().name)
	}
}
