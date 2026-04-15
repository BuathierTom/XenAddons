# XenAddons

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.10-green.svg)
![Fabric](https://img.shields.io/badge/Fabric-Mod-orange.svg)
![Java](https://img.shields.io/badge/Java-21+-blue.svg)
![Version](https://img.shields.io/badge/Version-1.3.1-purple.svg)
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
- `/xa procs <spirit|bonzo|phoenix>`
- `/xa gate`
- `/xa milestone`
- `/xa mask`
- `/xa mask has`
- `/xa mask nomask`
- `/xa pf <pseudo>`

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

- `Milestone Reminder`
  - Displays your current dungeon milestone as HUD text.
  - Starts its reminder timer when the milestone first becomes available.
  - Sends reminders every 30 seconds while the milestone stays below 3.
  - Reminder text is customizable.
  - Supports custom/random sounds or the default invisibility timer sound.
  - HUD and alert positions/scales are editable through `Edit HUD`.

- `Invisibility timer`
  - Shows active invincibility duration after:
    - Bonzo's Mask proc
    - Spirit Mask proc
    - Phoenix Pet proc
  - HUD element is editable through `Edit HUD` (move/scale/reset).
  - Display mode selector:
    - `Seconds` (`3.000`)
    - `Ticks` (`60t`)
  - Color thresholds:
    - green: `3000 -> 1500`
    - yellow: `1500 -> 700`
    - red: `700 -> 0`
  - Proc durations:
    - Spirit: `3s`
    - Bonzo: `3s`
    - Phoenix: `4s`
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
  - Cooldown reduction now scales from your class level and unique-class bonus.

- `Custom Hyperion Sound`
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

- `Peek Chat`
  - Hold a keybind to temporarily display the expanded chat without opening chat.
  - Default keybind is `Not Bound`.
  - Mouse wheel scroll works while the key is held.
  - Releasing the key returns chat to the bottom.

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

## Visual Words / Custom Data

The mod now uses a unified cosmetics snapshot:
- Primary source: `https://api.xenus.fr/`
- Fallback source: `assets/xenaddons/visual/cosmeticPeople.json`
  - visual name replacements
  - custom player scale
  - UUID-based mapping for stable behavior across nick/display changes
- Cosmetics portal login: `https://addons.xenus.fr/login`

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
