# XenAddons

<div align="center">

[![Downloads](https://img.shields.io/modrinth/dt/xenaddons?style=for-the-badge&logo=modrinth&logoColor=007F43&label=Downloads&color=00AF5C)](https://modrinth.com/mod/xenaddons)
[![Discord](https://img.shields.io/discord/1473269453213925492?style=for-the-badge&logo=discord&logoColor=white&label=Discord&color=1E3A8A)](https://discord.gg/yRbJJwGTPG)
[![GitHub Version](https://img.shields.io/badge/GitHub-v1.3.6-E67E22?style=for-the-badge&logo=github&logoColor=white)](https://github.com/BuathierTom/XenAddons/releases)
[![Cosmetics Portal](https://img.shields.io/badge/Cosmetics-Portal-00AF5C?style=for-the-badge&logo=firefoxbrowser&logoColor=white)](https://addons.xenus.fr/login)

### A Fabric mod for Minecraft 1.21.11 focused on Hypixel SkyBlock QoL

</div>

---

## Compatibility

- Minecraft: `1.21.11`
- Mod loader: `Fabric`
- Java: `21+`

---

## Commands

- `/xa` or `/xenaddons` → Open GUI
- `/xa hud` → Open HUD editor
- `/xa refill <pearl|tnt|leap|twap|tap|jerry|decoy>` → Refill a dungeon utility item to its target stack size
- `/pcc` → Shortcut for `/chat party`

### Debug Commands

Available when `Debug` is enabled:

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

---

## Features

### Category: MISC

- **Bazaar/AH Quick Search** → Search hovered items instantly.
- **Command Aliases** → Useful SkyBlock shortcuts.
- **Compact Potion Message** → Cleaner potion effect messages.
- **Cooldown Items** → Item ability cooldown overlay.
- **Copy Chat** → Right-click messages to copy chat text.
- **Cursor 1.8.9** → Legacy-style inventory cursor behavior.
- **Custom Hyperion Sound** → Replace Wither Blade sounds.
- **Exp Table Addons** → Keeps Experimentation Table items visible.
- **Inventory Buttons** → Configurable inventory buttons.
- **Item Price** → Bazaar & Lowest BIN tooltip prices.
- **Peek Chat** → Temporarily expand chat without opening it.
- **Spotify Overlay** → Displays the current Spotify track as a HUD element, with optional better display controls.
- **ZguegAddons** → Chat, dungeon, and party QoL tools.

### Category: DUNGEONS

- **AutoPet Title** → HUD notifications for AutoPet messages.
- **Bat Death Alert** → Alerts when bats die in dungeons.
- **Better Party Finder** → Enhanced PF stats and quick actions.
- **Dungeon Not Full Alert** → Warns if your party is incomplete.
- **Invisibility Timer** → Tracks Bonzo, Spirit, and Phoenix invulnerability.
- **Item Quality** → Dungeon item quality tooltip.
- **Mask Reminder** → Reminds you to equip dungeon masks.
- **Milestone Reminder** → Dungeon milestone HUD reminders.
- **Players Leaped** → Displays leap progress in F7 terms.
- **Pos Message** → Party callouts at configured spots.
- **Pos Titles** → Displays teammate callouts as HUD text.
- **Refill** → Uses `/xa refill` to request only the dungeon utility items missing from the target stack.
- **Reminder Gate** → Gate reminder from lever progress.
- **SecretRoutes** → Dungeon routes, secrets, and room helpers.

### Category: DEV

- **Config GUI** → Customize GUI appearance and theme settings.
- **Cosmetics** → Custom names, player scaling, and cosmetics refresh with a 30s cooldown.
- **Edit HUD** → Move, scale, and reset HUD elements.
- **Debug** → Internal testing and debugging tools.

---

## Visual Words / Custom Data

The mod uses a unified cosmetics snapshot.

### Primary API

```txt
https://api.xenus.fr/public/cosmetics
```

### Fallback Source

```txt
assets/xenaddons/visual/cosmeticPeople.json
```

Features:

- Visual name replacements
- Custom player scale
- UUID-based mapping for stable behavior across nick/display changes

### Cosmetics Portal

```txt
https://addons.xenus.fr/login
```

---

## Update Checker

- Runs once on first connection each launch
- Checks latest release on Modrinth
- Shows update status in chat:
  - Checking update
  - Update available / Up-to-date
- Downloads the latest jar directly to `mods/`
- Requires restart after updating

---

## Config File

```txt
.minecraft/config/xenaddons_config.json
```

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
