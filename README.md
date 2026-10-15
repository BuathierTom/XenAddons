# XenAddons

<div align="center">

[![Downloads](https://img.shields.io/modrinth/dt/xenaddons?style=for-the-badge&logo=modrinth&logoColor=007F43&label=Downloads&color=00AF5C)](https://modrinth.com/mod/xenaddons)
[![Discord](https://img.shields.io/discord/1473269453213925492?style=for-the-badge&logo=discord&logoColor=white&label=Discord&color=1E3A8A)](https://discord.gg/yRbJJwGTPG)
[![GitHub Version](https://img.shields.io/badge/GitHub-v1.4.11-E67E22?style=for-the-badge&logo=github&logoColor=white)](https://github.com/BuathierTom/XenAddons/releases)
[![Cosmetics Portal](https://img.shields.io/badge/Cosmetics-Portal-00AF5C?style=for-the-badge&logo=firefoxbrowser&logoColor=white)](https://addons.xenus.fr/login)

### A Fabric mod for Minecraft 26.1.2, 26.2 and 26.3 focused on Hypixel SkyBlock QoL

</div>

---

## Commands

- `/xa` or `/xenaddons` → Open GUI
- `/xa hud` → Open HUD editor
- `/xa refill <pearl|tnt|leap|twap|tap|jerry|decoy> [amount]` → Refill a dungeon utility item to its default or custom target amount

### Debug Commands

Available when `Debug` is enabled. Each one can be toggled individually, and a disabled command is
hidden from `/xa` autocompletion:

- `/xa title`
- `/xa petrule`
- `/xa ticlass`
- `/xa secretroom`
- `/xa procs <spirit|bonzo|phoenix>`
- `/xa gate`
- `/xa milestone`
- `/xa pf <pseudo>`

---

## Features

### Category: MISC

- **Bazaar/AH Quick Search** → Search hovered items instantly.
- **Command Aliases** → Useful SkyBlock shortcuts.
- **Compact Potion Message** → Cleaner potion effect messages.
- **Compact Stash** → Replaces the stash reminders with one clickable `Item/Material Stash >` line that runs `/pickupstash`.
- **Cooldown Items** → Item ability cooldown overlay.
- **Copy Chat** → Right-click messages to copy chat text.
- **Cursor 1.8.9** → Legacy-style inventory cursor behavior.
- **Custom Hyperion Sound** → Replace Wither Blade sounds.
- **Exp Table Addons** → Keeps Experimentation Table items visible.
- **Farming Tools Keys** → Swaps your movement and break keys while you hold a farming tool in the Garden, with a toggle keybind.
- **Hide Parts on Rod** → Hides the empty Hook, Line, and Sinker lines from fishing rod tooltips.
- **Inventory Buttons** → Configurable inventory buttons.
- **No Glint** → Removes the enchantment glint from every item.
- **Item Price** → Bazaar & Lowest BIN tooltip prices, including shards and sack contents.
- **Pet Level** → Shows a pet's level in the corner of its slot, hidden once it is maxed, with a color picker for the text.
- **Peek Chat** → Temporarily expand chat without opening it.
- **Private Message Format** → Rewrites private messages as `Msg > To/From <player>: message` with the text in white.
- **Spotify Overlay** → Displays Spotify on macOS and Spotify/Deezer media sessions on Windows, with an optional Liquid Glass HUD.
- **Visitor Garden** → Right-click a Garden visitor offer to search the Bazaar for the item it needs.
- **Warps Shortcuts** → Warp with `/desert` instead of `/warp desert`, from a remote warp list.
- **ZguegAddons** → Chat, dungeon, and party QoL tools.

### Category: DUNGEONS

- **AutoPet Title** → HUD notifications for AutoPet messages.
- **Bat Death Alert** → Alerts when bats die in dungeons.
- **Better Party Finder** → Enhanced PF stats and quick actions.
- **Dungeon Not Full Alert** → Warns if your party is incomplete.
- **Highlight Pressure Plate** → Highlights the I4 room's Light Weighted Pressure Plate in green while it's pressed.
- **Invincibility Display** → HUD with the Spirit Mask, Bonzo Mask and Phoenix Pet icons showing their invincibility time, cooldown or ready state, and which mask you are wearing.
- **Invincibility Timer** → Tracks Bonzo, Spirit, and Phoenix invulnerability.
- **Item Quality** → Dungeon item quality tooltip.
- **Loadout Title** → Replaces loadout equip messages with a compact chat line and an optional HUD title, and highlights the equipped loadout in `/loadout` with a pickable color.
- **Milestone Reminder** → Dungeon milestone HUD reminders.
- **Mute Bark Rune** → Mutes the wolf growl and bark sounds of the Bark Rune, optionally only in dungeons.
- **Players Leaped** → Displays leap progress in F7 terms.
- **Pos Message** → Party callouts at configured spots.
- **Pos Titles** → Displays teammate callouts as HUD text.
- **Refill** → Uses `/xa refill` to request only the missing items up to the default or custom target amount.
- **Reminder Gate** → Gate reminder from lever progress.
- **SecretRoutes** → Dungeon routes, secrets, and room helpers, with current and legacy route sets.
- **Show Ultimate Enchant** → Marks a Terminator or Hyperion carrying a tracked ultimate enchant directly on its item slot, with a customizable label color.

### Category: SLAYER

- **Slayer Boss HUD** → Shows your slayer boss's nametag and health on screen.
- **Boss Alert** → HUD alert and ping when your slayer boss spawns.
- **Mini-Boss Alert** → HUD alert and ping when a slayer mini-boss spawns.
- **Cocoon Alert** → HUD alert and ping when you cocoon a slayer mob, with an optional party chat announce.

### Category: FISHING

- **Fishing Hook Display** → HUD for the Hypixel fishing timer and pull alert, with an editable alert text and the vanilla nametag hidden automatically.
- **Hotspot Highlight** → Highlights fishing hotspots with a color per buff type, outline and/or filled circle, and hides their spawn particles.
- **Hide Other Bobbers** → Hides every other player's fishing bobber.

### Category: DEV

- **Config GUI** → Customize GUI appearance and theme settings.
- **Cosmetics** → Custom names, player scaling, dragon wings, and cosmetics refresh with a 30s cooldown.
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

- Visual name replacements, static or animated
- Custom player scale
- Dragon wings, with a per-player size and color
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

---

## Multi-version

The project uses [Stonecutter](https://stonecutter.kikugie.dev/) to build one jar per Minecraft version from the same sources.

| Minecraft | Fabric API     | Folder            |
|-----------|----------------|-------------------|
| 26.1.2    | 0.155.3+26.1.2 | `versions/26.1.2` |
| 26.2      | 0.161.0+26.2   | `versions/26.2`   |
| 26.3      | 0.162.0+26.3   | `versions/26.3`   |

- The code in `src/` is the active version, **26.1.2**.
- API renames between versions are declared in `stonecutter.gradle.kts`, per-version dependencies in `versions/<version>/gradle.properties`.
- Code that differs between versions uses Stonecutter blocks (`//? if >=26.2 {`).
- Switch the active version: `./gradlew "Set active project to 26.3"`, then `./gradlew "Reset active project"` before committing.

---

## Run

```bash
vim ~/.devauth/config.toml

./gradlew :26.1.2:runClient
./gradlew :26.1.2:runClient -P devauth_account=<pseudo>
```
