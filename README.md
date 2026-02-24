# QoL Recommender (NeoForge 1.21.1)

Client-only NeoForge mod that:
- shows a dismissable Swing popup recommending optional client/QoL mods
- can download **direct Modrinth CDN .jar URLs** into the instance `mods/` folder
- saves a JSON config so the popup can be disabled without deleting files

## Config location
`config/qolrecommender.json`

Each entry supports:
- `name`
- `description` (optional)
- `pageUrl` (optional; used by "Open page")
- `downloadUrl` (**direct** Modrinth CDN jar URL)
- `filenameHint` (recommended; used to detect installed jars)

## Building
- If you have Gradle installed: `gradle build`
- Or generate wrapper once: `gradle wrapper`, then `./gradlew build`

Output jar: `build/libs/`

## Notes
- Downloader allows only `cdn.modrinth.com` by default.
- Downloads are written to `.part` first, then renamed.
- Restart Minecraft after installing downloaded mods.
