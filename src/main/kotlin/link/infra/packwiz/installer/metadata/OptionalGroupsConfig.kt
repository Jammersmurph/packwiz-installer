package link.infra.packwiz.installer.metadata

import cc.ekblad.toml.decode
import cc.ekblad.toml.tomlMapper
import link.infra.packwiz.installer.request.RequestException
import link.infra.packwiz.installer.target.ClientHolder
import link.infra.packwiz.installer.target.path.HttpUrlPath
import link.infra.packwiz.installer.target.path.PackwizPath
import link.infra.packwiz.installer.ui.data.IOptionDetails
import link.infra.packwiz.installer.ui.data.OptionGroup
import link.infra.packwiz.installer.util.Log

data class OptionalGroupsConfig(
	val version: Int = 1,
	val groups: List<Group> = emptyList()
) {
	data class Group(
		val id: String,
		val name: String,
		val description: String = "",
		val mods: List<String>
	)

	companion object {
		internal fun parse(text: String): OptionalGroupsConfig {
			val config = text.byteInputStream().use { tomlMapper {}.decode<OptionalGroupsConfig>(it) }
			require(config.version == 1) { "Unsupported optional-groups.toml version ${config.version}" }
			require(config.groups.size <= 100) { "Too many optional groups" }
			require(config.groups.map { it.id }.distinct().size == config.groups.size) { "Duplicate group IDs" }
			for (group in config.groups) {
				require(group.id.isNotBlank() && group.name.isNotBlank()) { "Group ID and name must not be blank" }
				require(group.mods.size <= 2000) { "Too many mods in group ${group.id}" }
				for (path in group.mods) {
					require(path.isNotBlank() && !path.contains('\\') && !path.startsWith('/') &&
						!path.contains(':') && ".." !in path.split('/')) { "Mod paths must be relative to pack.toml: $path" }
				}
			}
			return config
		}
	}

	internal fun resolve(
		packFile: PackwizPath<*>,
		optionalChoices: Map<PackwizPath<*>, IOptionDetails>
	): List<OptionGroup> {
		val used = mutableSetOf<IOptionDetails>()
		return groups.mapNotNull { group ->
			val members = group.mods.distinct().mapNotNull { path ->
				val choice = optionalChoices[packFile.resolve(path)]
				if (choice == null) {
					Log.warn("Optional group '${group.id}' references an unavailable optional mod: $path")
					null
				} else if (!used.add(choice)) {
					Log.warn("Optional mod '$path' appears more than once; keeping its first group")
					null
				} else choice
			}
			if (members.isEmpty()) null else OptionGroup(group.id, group.name, group.description, members)
		}
	}
}

/** Loaded only when opening optional choices; malformed or missing configuration is non-fatal. */
internal fun loadOptionalGroups(packFile: PackwizPath<*>, client: ClientHolder): OptionalGroupsConfig? {
	return try {
		val path = packFile.resolve("optional-groups.toml")
		val source = if (path is HttpUrlPath) path.source(client, 3000) else path.source(client)
		source.use {
			require(!it.request(128 * 1024L + 1)) { "optional-groups.toml exceeds 128 KiB" }
			OptionalGroupsConfig.parse(it.readUtf8())
		}
	} catch (e: Exception) {
		val missing = e is RequestException.Response.File.FileNotFound ||
			(e is RequestException.Response.HTTP.ErrorCode && e.res.code == 404)
		if (!missing) Log.warn("Ignoring optional-groups.toml; using the normal optional-mod list", e)
		null
	}
}
