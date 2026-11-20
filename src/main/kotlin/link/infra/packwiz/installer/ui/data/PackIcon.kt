package link.infra.packwiz.installer.ui.data

import link.infra.packwiz.installer.target.ClientHolder
import link.infra.packwiz.installer.target.path.HttpUrlPath
import link.infra.packwiz.installer.target.path.PackwizPath
import link.infra.packwiz.installer.util.Log
import link.infra.packwiz.installer.request.RequestException
import java.util.concurrent.TimeUnit

internal const val MAX_PACK_ICON_BYTES = 1024 * 1024

/** Artwork is optional, bounded, and resolved beside pack.toml (including nested packs). */
internal fun loadPackIcon(
	packFile: PackwizPath<*>,
	clientHolder: ClientHolder,
	accept: (ByteArray) -> Boolean = { true }
): ByteArray? {
	val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3)
	var candidate: ByteArray? = null
	var found = 0
	for (name in listOf("icon.png", "icon.webp", "icon.jpg", "icon.jpeg", "icon.gif", "icon.bmp")) {
		val remaining = TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime())
		if (remaining <= 0) {
			Log.info("Pack icon lookup timed out; using the default logo")
			return null
		}
		try {
			val icon = packFile.resolve(name)
			val source = if (icon is HttpUrlPath) icon.source(clientHolder, remaining) else icon.source(clientHolder)
			candidate = source.use {
				found++
				if (found > 1) {
					Log.info("Multiple pack icons found; using the default logo")
					return null
				}
				if (it.request(MAX_PACK_ICON_BYTES.toLong() + 1)) null else it.readByteArray()
			}
		} catch (e: Exception) {
			// Only a confirmed missing file rules out another candidate.
			val missing = e is RequestException.Response.File.FileNotFound ||
				(e is RequestException.Response.HTTP.ErrorCode && e.res.code == 404)
			if (!missing) {
				Log.info("Pack icon lookup failed; using the default logo")
				return null
			}
		}
	}
	if (candidate != null && accept(candidate)) return candidate
	Log.info("No supported pack icon available; using the default logo")
	return null
}
