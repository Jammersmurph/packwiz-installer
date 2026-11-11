# packwiz-installer
An installer for launching packwiz modpacks with MultiMC. You'll need [the bootstrapper](https://github.com/comp500/packwiz-installer-bootstrap/releases) to actually use this.

## Desktop interface

The installer uses a dark, Minecraft-inspired Swing interface with a fixed-size
progress window, expandable activity log, and searchable optional-mod selection.
Use `--title "My Modpack"` to personalize the header, or `--no-gui` for the CLI.

### Safe local preview

With **JDK 17** installed (compatible with this project's Gradle 7.5 wrapper):

```sh
JAVA_HOME=/path/to/jdk-17 ./gradlew previewGui
```

This runs a simulated installation and provides buttons to preview optional mods
and download errors. It does not download mods or change an installation. Try
expanding the activity log, searching and toggling mods,
and selecting a failed file to inspect its details. The preview closes
automatically when the simulated update finishes, just like the real installer;
Cancel closes it early. The normal installer still uses the bootstrapper.

### Bootstrapper compatibility check

`./gradlew bootstrapGuiTest` loads the final, shrunk installer JAR in an isolated
class loader, matching the bootstrapper rather than the development class path.
It checks UI delegates and fonts and fails on GUI initialization errors. This
check also runs during `build` (with a display, or under `xvfb-run` on Linux).
CI builds on Java 8 and additionally runs this packaged-GUI check on Java 21.

## GitHub builds and releases

Pushes to `main` and pull requests build on Java 8 and run the GUI tests
under a virtual display. The JAR is also uploaded as a workflow artifact.

- **`main`:** Each successful build publishes a stable release with a unique
  `build-<run-id>-<attempt>` tag pointing at the built commit. The new release is
  marked latest, so the updater's `/releases/latest` endpoint discovers it.
  The JAR's `Implementation-Version` matches that tag, so the bootstrapper
  does not download it again unless a newer release is published.
- **Development:** Test locally using `./gradlew build` and `./gradlew previewGui`.
  Pushes to `dev` do not build or publish anything. The branch remains available
  for development work.
- **Pull requests:** Build and test only; no release is published.

Every release asset is named **`packwiz-installer.jar`**, regardless of its tag.
No BunnyCDN credentials or personal access token are required; the workflow
uses GitHub's built-in token. Pushing tags alone does not publish a release.

Stable updater configuration:

```properties
packupdater.installer-url=https://api.github.com/repos/Jammersmurph/packwiz-installer/releases/latest
```

Release automation passes `-PinstallerVersion=<release-tag>` to Gradle before
building. Local builds default to a Git-derived version instead.
