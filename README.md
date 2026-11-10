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

Pushes to `main` and pull requests build on Java 8 and run the GUI tests under
a virtual display. The resulting `packwiz-installer.jar` is available in the
workflow's `packwiz-installer` artifact. The snapshot workflow can also be run
manually from the Actions tab.

Pushing a version tag such as `v1.0.0` builds and tests the installer, then
publishes a GitHub release in this repository with `packwiz-installer.jar`
attached. No BunnyCDN credentials or personal access token are required; the
workflow uses GitHub's built-in token. Normal version tags produce releases
that the `/releases/latest` updater endpoint can discover.
