# QoL Recommender

A client-only NeoForge 1.21.1 mod that opens a Swing window on startup listing optional client and QoL mods for a modpack, with one-click downloads from Modrinth CDN straight into the instance `mods/` folder.

## Requirements

- Minecraft 1.21.1
- NeoForge 21.1.219 or newer (client side only)
- Java 21 (for building from source)

## How it works

Drop the jar into your instance `mods/` folder and launch the game. Once Minecraft creates its window, QoL Recommender loads `config/qolrecommender.json` and opens a list of recommended mods.

- Scans `mods/` to mark each entry as installed, missing dependencies, or not installed.
- Downloads listed dependencies first, then the main jar, writing to a `.part` file and rejecting responses under 50 KB before renaming into place.
- Allows downloads from `cdn.modrinth.com` only, so arbitrary hosts are blocked by default.
- Saves the "Don't show this again" checkbox back to `config/qolrecommender.json` when you close the window.

Restart Minecraft after downloading new mods so NeoForge loads the jars on the next launch.

## Configuration

The mod creates `config/qolrecommender.json` with an empty entry list on first launch. The table below lists the top-level fields in that file.

| Field | Type | Default | What it controls |
| --- | --- | --- | --- |
| `enabled` | boolean | `true` | Set to `false` to disable the popup completely. |
| `showOnFirstLaunchOnly` | boolean | `true` | When `true`, checking "Don't show this again" hides the window on future launches. |
| `dismissed` | boolean | `false` | Saved state of the checkbox. |
| `windowTitle` | string | `"Optional Client QoL Mods"` | Title bar text for the Swing window. |
| `entries` | array | `[]` | List of optional mods shown in the window. |

Each item inside `entries` uses the fields below (`description`, `pageUrl`, `filenameHint`, and `dependencies` are optional).

```json
{
  "enabled": true,
  "showOnFirstLaunchOnly": true,
  "dismissed": false,
  "windowTitle": "Optional Client QoL Mods",
  "entries": [
    {
      "name": "Iris Shaders",
      "description": "Shader support for Sodium.",
      "pageUrl": "https://modrinth.com/mod/iris",
      "downloadUrl": "https://cdn.modrinth.com/data/YL57xq9U/versions/example/iris-neoforge-1.8.0.jar",
      "filenameHint": "iris",
      "dependencies": [
        {
          "url": "https://cdn.modrinth.com/data/AANobbMI/versions/example/sodium-neoforge-0.6.0.jar",
          "hint": "sodium"
        }
      ]
    }
  ]
}
```

If `pageUrl` is left blank, "Open page" opens `downloadUrl` instead. When `filenameHint` is set, any `.jar` in `mods/` whose name contains that text (case-insensitive) counts as installed; otherwise the mod checks for the exact filename from the URL.

## Building

```bash
./gradlew test build
```

The built jar lands in `build/libs/qolrecommender-1.1.0.jar`.

## Limitations

- "Open mods folder" and "Open page" use Java's `Desktop` API, which requires a desktop session and will show an error dialog in headless environments.
- Downloaded jars are not hot-loaded; players need to restart Minecraft after installing them.

## License

MIT. See [LICENSE](LICENSE).
