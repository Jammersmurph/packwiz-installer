package link.infra.packwiz.installer.ui.data

import link.infra.packwiz.installer.target.ClientHolder
import link.infra.packwiz.installer.target.path.PackwizFilePath
import link.infra.packwiz.installer.target.path.HttpUrlPath
import okhttp3.HttpUrl.Companion.toHttpUrl
import com.sun.net.httpserver.HttpServer
import okio.Path.Companion.toPath
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class PackIconTest {
	@get:Rule val temporary = TemporaryFolder()

	@Test fun resolvesIconBesideNestedPackRatherThanRepositoryRoot() {
		val root = temporary.newFolder()
		val nested = File(root, "packs/client").apply { mkdirs() }
		val png = javaClass.getResourceAsStream("/branding/packupdater.png")!!.use { it.readBytes() }
		File(root, "icon.png").writeText("Not the icon for this pack")
		File(nested, "icon.png").writeBytes(png)
		val path = PackwizFilePath(root.absolutePath.toPath(), "packs/client/pack.toml")
		assertArrayEquals(png, loadPackIcon(path, ClientHolder()))
	}

	@Test fun missingIconIsOptional() {
		val root = temporary.newFolder()
		assertNull(loadPackIcon(PackwizFilePath(root.absolutePath.toPath(), "pack.toml"), ClientHolder()))
	}

	@Test fun oversizedIconIsIgnored() {
		val root = temporary.newFolder()
		File(root, "icon.png").writeBytes(ByteArray(MAX_PACK_ICON_BYTES + 1))
		assertNull(loadPackIcon(PackwizFilePath(root.absolutePath.toPath(), "pack.toml"), ClientHolder()))
	}

	@Test fun acceptsEverySupportedFilenameWhenItIsTheOnlyCandidate() {
		val png = javaClass.getResourceAsStream("/branding/packupdater.png")!!.use { it.readBytes() }
		for (name in listOf("icon.png", "icon.webp", "icon.jpg", "icon.jpeg", "icon.gif", "icon.bmp")) {
			val root = temporary.newFolder()
			File(root, name).writeBytes(png)
			assertArrayEquals(name, png, loadPackIcon(PackwizFilePath(root.absolutePath.toPath(), "pack.toml"), ClientHolder()))
		}
	}

	@Test fun multipleCandidatesUseDefaultEvenWhenOneIsInvalid() {
		val root = temporary.newFolder()
		val png = javaClass.getResourceAsStream("/branding/packupdater.png")!!.use { it.readBytes() }
		File(root, "icon.png").writeBytes(png)
		File(root, "icon.webp").writeText("Not a valid image")
		var applied = false
		val data = loadPackIcon(PackwizFilePath(root.absolutePath.toPath(), "pack.toml"), ClientHolder()) {
			applied = true
			true
		}
		assertNull(data)
		assertFalse(applied)
	}

	@Test fun multipleRemoteCandidatesUseDefault() {
		val png = javaClass.getResourceAsStream("/branding/packupdater.png")!!.use { it.readBytes() }
		val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
		for (name in listOf("/icon.png", "/icon.jpg")) {
			server.createContext(name) {
				it.sendResponseHeaders(200, png.size.toLong())
				it.responseBody.use { output -> output.write(png) }
				it.close()
			}
		}
		server.start()
		val client = ClientHolder()
		try {
			val pack = HttpUrlPath("http://127.0.0.1:${server.address.port}/".toHttpUrl(), "pack.toml")
			assertNull(loadPackIcon(pack, client))
		} finally {
			client.close()
			server.stop(0)
		}
	}

	@Test fun loadsRemoteIconBesidePackToml() {
		val png = javaClass.getResourceAsStream("/branding/packupdater.png")!!.use { it.readBytes() }
		val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
		server.createContext("/nested/icon.png") {
			it.sendResponseHeaders(200, png.size.toLong())
			it.responseBody.use { output -> output.write(png) }
			it.close()
		}
		server.start()
		val client = ClientHolder()
		try {
			val pack = HttpUrlPath("http://127.0.0.1:${server.address.port}/".toHttpUrl(), "nested/pack.toml")
			assertArrayEquals(png, loadPackIcon(pack, client))
			assertNull(loadPackIcon(HttpUrlPath("http://127.0.0.1:${server.address.port}/".toHttpUrl(), "missing/pack.toml"), client))
		} finally {
			client.close()
			server.stop(0)
		}
	}

	@Test(timeout = 12000) fun stalledArtworkDoesNotBlockInstallation() {
		val release = CountDownLatch(1)
		val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
		server.createContext("/icon.png") {
			try {
				it.sendResponseHeaders(200, 100)
				release.await(10, TimeUnit.SECONDS)
			} finally { it.close() }
		}
		server.start()
		val client = ClientHolder()
		try {
			val start = System.nanoTime()
			val pack = HttpUrlPath("http://127.0.0.1:${server.address.port}/".toHttpUrl(), "pack.toml")
			assertNull(loadPackIcon(pack, client))
			assertTrue(TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start) < 8)
		} finally {
			release.countDown()
			client.close()
			server.stop(0)
		}
	}
}
