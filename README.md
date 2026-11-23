# packwiz-installer
An installer for launching packwiz modpacks with MultiMC. You'll need [the bootstrapper](https://github.com/comp500/packwiz-installer-bootstrap/releases) to actually use this.

## Desktop interface

The installer uses a dark purple Swing interface with PackUpdater logo branding and a fixed-size
progress window, expandable activity log, and searchable optional-mod selection.
The header and window title use the name from `pack.toml` once it loads.
Use `--title "My Modpack"` to override it, or `--no-gui` for the CLI.

Add an icon beside your hosted `pack.toml` to replace the default logo in
the updater header and window icon. This also works for local packs. Artwork
keeps its aspect ratio; missing, invalid, or oversized icons leave the default
logo unchanged. The bundled logo is square; pack artwork fits a square display
area without stretching. PNG, WebP, JPEG, GIF (first frame), and BMP are supported.
The updater checks for `icon.png`, `icon.webp`, `icon.jpg`, `icon.jpeg`, `icon.gif`,
and `icon.bmp` beside `pack.toml`. Exactly one must exist; multiple matches use
the default PackUpdater logo instead. Missing or invalid artwork also uses the default. The actual image
format is detected from its contents, so a WebP named `icon.png` also works.
Icons must be no larger than 1 MiB and 2048×2048 pixels. All remote artwork
lookups share a three-second deadline, and headless installs skip them.

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

## Grouping optional mods

Place **`optional-groups.toml` beside `pack.toml`** in your hosted pack, without
changing Packwiz or registering it in the index. Local packs work too.

```toml
version = 1 # Optional; defaults to 1. This is the config format, not pack version.

[[groups]]
id = "vr"
name = "VR Support"
description = "Enable these mods when playing with a VR headset."
mods = [
    "mods/vivecraft.pw.toml",
    "mods/vivecraft-compat.pw.toml",
]
```

Each group has an expand/collapse arrow and a checkbox to enable or disable all
its members. Expand a group to change individual mods; mixed selections show an
intermediate checkbox state and an enabled count. Search finds group names and
individual mods, expanding matches automatically. Group actions always affect
the whole group, including members hidden by a search.

- Entries match **metadata paths**, relative to `pack.toml`, not display names
  or downloaded JAR names. Only existing, optional, current-side mods can match.
- Grouping preserves existing choices and Packwiz defaults. **Save selection**
  applies changes; **Cancel** or closing the dialog discards them.
- Unlisted mods appear under **Other optional mods**. A mod listed in multiple
  groups belongs to the first group; unknown members are skipped with warnings,
  and empty groups are hidden.
- The config is fetched only when opening the optional-mod selector (including
  on already-up-to-date packs). No config means the original flat list.
- Invalid config, unsupported versions, or download failures warn and fall back
  to the flat list, without preventing installation. IDs must be unique and
  paths must stay inside the pack. Config is limited to 128 KiB, 100 groups,
  and 2,000 members per group; remote fetches have a three-second deadline.
- The CLI keeps its existing behavior and does not fetch group configuration.

See [`examples/optional-groups.toml`](examples/optional-groups.toml) for a larger
example. `./gradlew previewGui` includes sample VR and visual groups to try
without changing a real installation.

## GitHub builds and releases

Pushes to `main` and pull requests build on Java 8 and run the GUI tests
under a virtual display. The JAR is also uploaded as a workflow artifact.

- **`main`:** Each successful build publishes a stable release with a unique
  `build-<run-id>-<attempt>` tag pointing at the built commit. The new release is
  marked latest, so the updater's `/releases/latest` endpoint discovers it.
  The JAR's `Implementation-Version` matches that tag, so the bootstrapper
  does not download it again unless a newer release is published.
- **Development:** Test locally using `./gradlew build` and `./gradlew previewGui`.
  Pushes to `dev` run the same build and compatibility checks, then publish a
  prerelease with a unique `dev-build-<run-id>-<attempt>` tag. The JAR's
  `Implementation-Version` matches that tag. Prereleases are not marked latest,
  so the stable updater's `/releases/latest` endpoint continues to use stable releases.
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
