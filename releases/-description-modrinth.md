# XenAddons

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.10-green.svg)
![Fabric](https://img.shields.io/badge/Fabric-Mod-orange.svg)
![Java](https://img.shields.io/badge/Java-21+-blue.svg)
![Version](https://img.shields.io/badge/Version-1.2.11-purple.svg)
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
- `/xa hud` - Open HUD editor
- `/pcc` - Shortcut for `/chat party`

Debug subcommands (when `Debug` is enabled + corresponding toggle is enabled):
- `/xa title`
- `/xa petrule`
- `/xa ticlass`
- `/xa secretroom`

Additional proc test command:
- `/xa procs <spirit|bonzo|phoenix>`

## Modules

### Dungeon

- `AutoPet Title`
  - Replaces AutoPet equip chat message with a HUD notification.
  - Supports HUD move/scale through `Edit HUD`.

- `Pos Message`
  - Sends party callouts when your character reaches configured dungeon spots.
  - Updated safespot detection:
    - Early Enter 2 safespots use specific point checks.
    - Early Enter 3 safespots use specific point checks.
  - Added custom point `At pew pew dev!`.
  - Removed class restriction on `At P2` and `At Pre4 Entry`.

- `Pos Titles`
  - Displays teammate callouts as HUD text.
  - Supports HUD move/scale through `Edit HUD`.

- `SecretRoutes`
  - Renders room routes/secrets in dungeons.
  - Settings include:
    - line color / line width / box alpha
    - render toggles (etherwarp, mines, interacts, superbooms, enderpearls, item secrets, interact secrets, bat secrets)
  - Improved route line width consistency.
  - Hides routes when room secrets are completed, including after re-entering the same room.
  - Shows locked chest lever waypoint when a chest is locked.

- `Players Leaped`
  - Displays `x/4 Players Leaped` in F7 terms-related zones.
  - Supports HUD move/scale through `Edit HUD`.

- `Invisibility timer`
  - Shows active invincibility duration after:
    - Bonzo's Mask proc
    - Spirit Mask proc
    - Phoenix Pet proc
  - HUD element is editable through `Edit HUD` (move/scale/reset).
  - Display mode selector:
    - `Seconds` (`3.000`)
    - `Ticks` (`60t`)
  - Includes `Mask procs alert`:
    - Large red on-screen proc alert (`Bonzo`, `Spirit`, `Phoenix`)
    - Configurable proc sound:
      - sound selector
      - `Sound Volume` (`0.0 -> 5.0`)
      - `Sound Pitch` (`0.0 -> 2.0`)
      - `Note play` preview button
    - Alert position/scale editable through `Edit HUD` as `Mask proc alert`.

- `Dungeon not full alert`
  - Shows an alert at dungeon start if the party is not full.
  - Improved party size detection from tab/class lines to avoid false `1/5` cases.

- `Bat Death Alert`
  - Shows an alert when a bat dies in dungeons.

- `Better Party Finder`
  - Reworked Party Finder join/class messages.
  - Detailed dungeon stats display for joining players.
  - Clickable quick actions (`kick`, `block`, `pv`) with direct execution.

- `Item Quality`
  - Adds `Quality Bonus: +...% (...)` to dungeon item tooltips.
  - Quality line is shown under item rarity.

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
  - `/ld` now waits longer before `/warp dungeon_hub`.

- `Cursor 1.8.9`
  - Keeps inventory cursor behavior closer to legacy 1.8.9 style.
  - Includes `Hard in dungeons only` mode for immediate center reset when cursor becomes visible in dungeons.

- `Copy Chat`
  - Right-click a chat line in chat screen to copy message text.
  - `Alt/Option + right-click` copies formatted text.
  - Integrated with VisualWords so copied text uses original message content.

- `Lava Remplacement`
  - Replaces lava visual rendering with water visual rendering.

- `Compact Potion Message`
  - Shortens potion effect chat messages into compact colored lines.

- `Cooldown Items`
  - Item ability cooldown overlay inspired by SkyHanni.
  - Supports ready state (`R`), cooldown text/colors, and inventory display options.
  - Includes mage cooldown reduction handling in dungeons.

- `Custom Hyperon Sound`
  - Replaces Wither Blade hype sound with a selectable custom sound.
  - Includes sound selector, volume/pitch controls, and `Play Sound` test button.

- `Experimentation Table Solver`
  - Chronomatron and Ultrasequencer solver.
  - Superpairs discovered-item visibility and XP overlay.

- `Item Price`
  - Shows SkyBlock item prices in tooltips.
  - Supports:
    - `Bazaar Price` (Hypixel bazaar)
    - `Lowest BIN` (from whatyouth API `auction` data)

- `Bazaar/AH Quick Search`
  - Search hovered item instantly with a keybind (default `M`).
  - Auto-selects `/bz` or `/ahs`.
  - Cleans reforges and decorative symbols from item name before search.
  - Keybind setting supports `ESC -> Not Bound`.

- `Spotify Overlay`
  - HUD showing current playing track in colored format:
    - `Spotify > Author - Song`
  - Long titles are truncated with `...` to keep HUD readable.
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

- `Config GUI`
  - GUI theme color settings.

- `Custom Size`
  - Applies per-player custom scale from JSON data.
  - Toggleable in `Dev`.

- `Edit HUD`
  - Move/scale/reset supported HUD elements.

- `Debug`
  - Enables test/debug helpers used by `/xa ...` subcommands.

## Update Checker

- Runs once on first connection each launch.
- Checks latest release on Modrinth.
- Shows chat status:
  - checking update
  - update available / up-to-date
- If update is available, downloads latest jar directly to `mods/` and asks for restart.

## Credits

Thanks to the following projects/mods for inspiration and/or technical references:

- [NoammAddons](https://github.com/Noamm9/NoammAddons)
- [OdinFabric](https://github.com/odtheking/OdinFabric/)
- [Devonian](https://github.com/Synnerz/devonian)
- [SecretRoute](https://github.com/yourboykyle/SecretRoutes/)
- [Blade-addons](https://github.com/BladeMasterGabe/blade-addons/)
- [jcnlkAddons](https://github.com/jcnlk/jcnlkAddons)
- [Krypt](https://github.com/Eclipse-5214/Krypt)
