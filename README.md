# XenAddons

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.10-green.svg)
![Fabric](https://img.shields.io/badge/Fabric-Mod-orange.svg)
![Java](https://img.shields.io/badge/Java-21+-blue.svg)
![Version](https://img.shields.io/badge/Version-1.2.2-purple.svg)
![License](https://img.shields.io/badge/License-All%20Rights%20Reserved-lightgrey.svg)

**A Fabric mod for Minecraft 1.21.10 focused on Hypixel SkyBlock QoL**

</div>

XenAddons is a Fabric mod for Minecraft **1.21.10** focused on Hypixel SkyBlock quality-of-life features.

## Compatibility

- Minecraft: `1.21.10`
- Mod loader: `Fabric`
- Java: `21+`

## Main Commands

- `/xa` - Open XenAddons GUI
- `/xenaddons` - Open XenAddons GUI

Debug subcommands (when `Debug` is enabled + corresponding toggle is enabled):
- `/xa title`
- `/xa petrule`
- `/xa ticlass`
- `/xa secretroom`

## Modules

### Dungeon

- `AutoPet Title`
  - Replaces AutoPet equip chat message with a HUD notification.
  - Supports HUD move/scale through `Edit HUD`.

- `Pos Message`
  - Sends party callouts when your character reaches configured dungeon spots.

- `Pos Titles`
  - Displays teammate callouts as HUD text.
  - Supports HUD move/scale through `Edit HUD`.

- `SecretRoutes`
  - Renders room routes/secrets in dungeons.
  - Settings include:
    - route rendering mode (current, full route, all steps, all secrets)
    - line color / line width / box alpha
    - render toggles (etherwarp, mines, interacts, superbooms, enderpearls, item secrets, interact secrets, bat secrets)

- `Players Leaped`
  - Displays `x/4 Players Leaped` in F7 terms-related zones.
  - Supports HUD move/scale through `Edit HUD`.

### Misc

- `Command Aliases`
  - Shortcuts with per-group toggles:
    - `/d`, `/dung`
    - `/ai`
    - `/pko`
    - `/pd`
    - `/pk <player>`
    - `/pt <player>`
    - `/lsb`, `/ld`
    - `/f1`..`/f7`
    - `/m1`..`/m7`

- `Custom Keybinds`
  - Bind commands to keys:
    - Wardrobe
    - Equipment
    - Trades
    - Garden
    - Pets

- `Cursor 1.8.9`
  - Keeps inventory cursor behavior closer to legacy 1.8.9 style.
  - Includes `Hard in dungeons only` mode for immediate center reset when cursor becomes visible in dungeons.

- `Custom Size`
  - Applies per-player custom scale from JSON data.
  - Optional `In Dungeon Only` toggle.

- `Copy Chat`
  - Right-click a chat line in chat screen to copy message text.
  - Integrated with VisualWords so copied text uses original message content.

- `Item Price`
  - Shows SkyBlock item prices in tooltips.
  - Supports:
    - `Bazaar Price` (Hypixel bazaar)
    - `Lowest BIN` (Noamm API fallback when item is not bazaar)

- `Spotify Overlay`
  - HUD showing current playing track in colored format:
    - `Spotify > Author - Song`
  - Supports HUD move/scale through `Edit HUD`.
  - Tab list remains visually on top of the overlay.

- `InvButtons`
  - Adds configurable inventory buttons.
  - Includes button position editor and profiles/advanced screen.

- `ZguegAddons`
  - Chat tools:
    - Chat Hider
    - Radio message filter
    - Discord warning filter
  - Dungeon tools:
    - 5 crypts party notify
    - Class notify
  - Party commands:
    - `!boom`
    - `!rng`
    - `!gay`
    - `!racist`
    - `!py`
    - `!gwer`
    - `!size`

### Dev

- `Colors`
  - GUI theme color settings.

- `Edit HUD`
  - Move/scale/reset supported HUD elements.

- `Debug`
  - Enables test/debug helpers used by `/xa ...` subcommands.

## Visual Words / Custom Data

The mod includes :
- visual name replacements / formatting (`visualWords`)
- player scaling (`customSizes`)

## Update Checker

- Runs once on first connection each launch.
- Checks latest release on Modrinth.
- Shows chat status:
  - checking update
  - update available / up-to-date
- If update is available, downloads latest jar directly to `mods/` and asks for restart.

## Config File

Runtime module state/settings are saved in:

- `.minecraft/config/xenaddons_config.json`

## Build

```bash
./gradlew build
```

Built jar output:

- `build/libs/`
