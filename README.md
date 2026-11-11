# packwiz-installer
An installer for launching packwiz modpacks with MultiMC. You'll need [the bootstrapper](https://github.com/comp500/packwiz-installer-bootstrap/releases) to actually use this.

## Desktop interface

The installer uses a dark, Minecraft-inspired Swing interface with a resizable
progress window, expandable activity log, and searchable optional-mod selection.
Use `--title "My Modpack"` to personalize the header, or `--no-gui` for the CLI.

### Safe local preview

With **JDK 17** installed (compatible with this project's Gradle 7.5 wrapper):

```sh
JAVA_HOME=/path/to/jdk-17 ./gradlew previewGui
```

This runs a simulated installation and provides buttons to preview optional mods
and download errors. It does not download mods or change an installation. Try
expanding the activity log, resizing the window, searching and toggling mods,
and selecting a failed file to inspect its details. The preview closes
automatically when the simulated update finishes, just like the real installer;
Cancel closes it early. The normal installer still uses the bootstrapper.

## GitHub builds and releases

Pushes to `main`, `dev`, and pull requests build on Java 8 and run the GUI tests
under a virtual display. The JAR is also uploaded as a workflow artifact.

- **`main`:** Each successful build publishes a stable release with a unique
  `build-<run-id>-<attempt>` tag pointing at the built commit. The new release is
  marked latest, so the updater's `/releases/latest` endpoint discovers it.
- **`dev`:** Each successful build replaces the JAR in a single rolling
  prerelease named **Development (rolling)**. Its `dev` tag moves to the built
  commit. Development builds never become the latest stable release.
- **Pull requests:** Build and test only; no release is published.

Every release asset is named **`packwiz-installer.jar`**, regardless of its tag.
No BunnyCDN credentials or personal access token are required; the workflow
uses GitHub's built-in token. Pushing tags alone does not publish a release.

Stable updater configuration:

```properties
packupdater.installer-url=https://api.github.com/repos/Jammersmurph/packwiz-installer/releases/latest
```

To explicitly use rolling development builds instead:

```properties
packupdater.installer-url=https://api.github.com/repos/Jammersmurph/packwiz-installer/releases/tags/dev
```
