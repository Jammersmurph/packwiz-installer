package link.infra.packwiz.installer

import link.infra.packwiz.installer.target.Side
import link.infra.packwiz.installer.target.path.PackwizFilePath
import link.infra.packwiz.installer.ui.IUserInterface
import link.infra.packwiz.installer.ui.cli.CLIHandler
import link.infra.packwiz.installer.ui.data.InstallProgress
import okio.Path.Companion.toPath
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest

class PackTitleTest {
	@get:Rule val temporary = TemporaryFolder()

	private class RecordingUI : IUserInterface by CLIHandler() {
		override var title = "packwiz-installer"
		val messages = mutableListOf<String>()
		override fun submitProgress(progress: InstallProgress) { messages.add(progress.message) }
		override fun showErrorAndExit(message: String, e: Exception?): Nothing {
			throw AssertionError(message, e)
		}
	}

	private fun fixture(name: String, title: String? = null): UpdateManager.Options {
		val source = temporary.newFolder("source")
		val installed = temporary.newFolder("installed")
		val launcher = temporary.newFolder("launcher")
		val index = File(source, "index.toml").apply {
			writeText("hash-format = \"sha256\"\nfiles = []\n")
		}
		val hash = MessageDigest.getInstance("SHA-256").digest(index.readBytes())
			.joinToString("") { "%02x".format(it.toInt() and 0xff) }
		File(source, "pack.toml").writeText("""
			name = "$name"
			pack-format = "packwiz:1.1.0"
			[index]
			file = "index.toml"
			hash-format = "sha256"
			hash = "$hash"
		""".trimIndent())
		val packFolder = PackwizFilePath(installed.absolutePath.toPath())
		return UpdateManager.Options(
			PackwizFilePath(source.absolutePath.toPath(), "pack.toml"),
			packFolder / "packwiz.json", packFolder,
			PackwizFilePath(launcher.absolutePath.toPath()), Side.CLIENT, 0, title
		)
	}

	@Test fun usesPackTomlNameOnFirstInstallAndCachedLaunch() {
		val options = fixture("Create VC")
		val first = RecordingUI()
		UpdateManager(options, first)
		assertEquals("Create VC", first.title)
		val cached = RecordingUI()
		UpdateManager(options, cached)
		assertEquals("Create VC", cached.title)
		assertTrue(cached.messages.any { it == "Modpack is already up to date!" })
	}

	@Test fun preservesExplicitTitle() {
		val options = fixture("Create VC", "Custom launcher title")
		val ui = RecordingUI().apply { title = "Custom launcher title" }
		UpdateManager(options, ui)
		assertEquals("Custom launcher title", ui.title)
	}

	@Test fun blankPackNameKeepsDefaultTitle() {
		val ui = RecordingUI()
		UpdateManager(fixture(""), ui)
		assertEquals("packwiz-installer", ui.title)
	}
}
