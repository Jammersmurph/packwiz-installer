package link.infra.packwiz.installer.ui.data

import link.infra.packwiz.installer.target.ClientHolder
import link.infra.packwiz.installer.target.path.HttpUrlPath
import link.infra.packwiz.installer.target.path.PackwizPath
import link.infra.packwiz.installer.util.Log

internal const val MAX_PACK_ICON_BYTES = 1024 * 1024

/** Artwork is optional, bounded, and resolved beside pack.toml (including nested packs). */
internal fun loadPackIcon(packFile: PackwizPath<*>, clientHolder: ClientHolder): ByteArray? {
	return try {
		val icon = packFile.resolve("icon.png")
		val source = if (icon is HttpUrlPath) icon.source(clientHolder, 3000) else icon.source(clientHolder)
		source.use {
			if (it.request(MAX_PACK_ICON_BYTES.toLong() + 1)) {
				Log.info("Pack icon exceeds 1 MiB; using the default logo")
				null
			} else {
				it.readByteArray()
			}
		}
	} catch (e: Exception) {
		Log.info("Pack icon unavailable; using the default logo")
		null
	}
}
