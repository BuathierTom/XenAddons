# XenAddons

<div align="center">

<img src="https://img.shields.io/badge/Minecraft-1.21.11-5E7C16?style=for-the-badge" />
<img src="https://img.shields.io/badge/Fabric-Mod-DB6E0D?style=for-the-badge" />
<img src="https://img.shields.io/badge/Java-21+-007396?style=for-the-badge" />
<img src="https://img.shields.io/badge/Version-1.3.4-7A3FE0?style=for-the-badge" />
<img src="https://img.shields.io/badge/License-ARR-555555?style=for-the-badge" />

### A Fabric mod for Minecraft 1.21.11 focused on Hypixel SkyBlock QoL

</div>

---

## Commands

- `/xa` or `/xenaddons` → Open GUI
- `/xa hud` → Open HUD editor
- `/pcc` → Shortcut for `/chat party`

Debug commands (`Debug` enabled):
- `/xa title`
- `/xa petrule`
- `/xa ticlass`
- `/xa secretroom`
- `/xa procs <spirit|bonzo|phoenix>`
- `/xa gate`
- `/xa milestone`
- `/xa mask`
- `/xa pf <pseudo>`

---

## Features

### Category: DUNGEON

- **AutoPet Title**: Replaces AutoPet equip messages with HUD notifications.
- **Pos Message**: Sends party callouts at configured dungeon spots.
- **Pos Titles**: Displays teammate callouts as HUD text.
- **SecretRoutes**: Dungeon routes, secret rendering, waypoints, and room tracking.
- **Players Leaped**: Shows leap progress (`x/4`) in F7 terms zones.
- **Milestone Reminder**: HUD reminders for missing dungeon milestones.
- **Invisibility Timer**: Tracks Bonzo, Spirit, and Phoenix invulnerability timers.
- **Dungeon Not Full Alert**: Warns if the party is not full.
- **Bat Death Alert**: Alerts when dungeon bats die.
- **Better Party Finder**: Enhanced PF messages with quick actions.
- **Item Quality**: Displays dungeon item quality bonuses.

### Category: GENERAL

- **Command Aliases**: Multiple SkyBlock shortcut commands.
- **Cursor 1.8.9**: Legacy cursor behavior.
- **Copy Chat**: Right-click to copy chat messages.
- **Lava Replacement**: Renders lava as water visuals.
- **Compact Potion Message**: Cleaner potion effect messages.
- **Cooldown Items**: Item cooldown overlay.
- **Custom Hyperion Sound**: Replace hype sounds.
- **Experimentation Table Solver**: Chronomatron, Ultrasequencer, Superpairs.
- **Item Price**: Bazaar and Lowest BIN tooltip prices.
- **Bazaar/AH Quick Search**: Search hovered items instantly.
- **Peek Chat**: Temporary expanded chat view.
- **Spotify Overlay**: HUD for currently playing Spotify track.
- **InvButtons**: Configurable inventory buttons.

### Category: ZGUEGADDONS

- **Chat Tools**: Chat hider, Implosion filter, radio filter.
- **Dungeon Tools**: 5 crypt notify, class notify.
- **Party Commands**: `!boom`, `!rng`, `!gay`, `!racist`, `!py`, `!gwer`, `!size`

### Category: DEV

- **Config GUI**: Theme color and GUI tweaks.
- **Custom Size**: Custom player scaling from JSON.
- **Edit HUD**: Move, scale, and reset HUD elements.
- **Debug**: Enables development/testing tools.

---

## Visual Words / Custom Data

Primary API:
- `https://api.xenus.fr/`

Fallback:
- `assets/xenaddons/visual/cosmeticPeople.json`

Cosmetics login:
- `https://addons.xenus.fr/login`

---

## Update Checker

- Checks updates on Modrinth
- Shows update status in chat
- Auto-downloads latest release to `mods/`
- Requires restart after update

---

## Config File

```txt
.minecraft/config/xenaddons_config.json
````

---

## Build

```bash
./gradlew build
```

Output:

```txt
build/libs/
```

---

## DevAuth (Dev only)

```bash
vim ~/.devauth/config.toml

[accounts.<pseudo>]
type = "microsoft"

export XENADDONS_DEVAUTH_ACCOUNT=<pseudo>
```
