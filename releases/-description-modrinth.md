# XenAddons

<div align="center">

[![Downloads](https://img.shields.io/modrinth/dt/xenaddons?style=for-the-badge&logo=modrinth&logoColor=007F43&label=Downloads&color=00AF5C)](https://modrinth.com/mod/xenaddons)
[![Discord](https://img.shields.io/discord/1473269453213925492?style=for-the-badge&logo=discord&logoColor=white&label=Discord&color=1E3A8A)](https://discord.gg/yRbJJwGTPG)
[![Version](https://img.shields.io/modrinth/v/xenaddons?style=for-the-badge&logo=modrinth&logoColor=white&label=Version&color=E67E22)](https://modrinth.com/mod/xenaddons)
[![Cosmetics Portal](https://img.shields.io/badge/Cosmetics-Portal-00AF5C?style=for-the-badge&logo=firefoxbrowser&logoColor=white)](https://addons.xenus.fr/login)

**A Fabric mod for Minecraft 1.21.11 focused on Hypixel SkyBlock QoL**

</div>

XenAddons is a Fabric mod for Minecraft **1.21.11** focused on Hypixel SkyBlock quality-of-life features.

## Compatibility

- Minecraft: `1.21.11`
- Mod loader: `Fabric`
- Java: `21+`

## Main Commands

- `/xa` - Open XenAddons GUI
- `/xenaddons` - Open XenAddons GUI
- `/xa hud` - Open HUD editor
- `/xa refill <pearl|tnt|leap|twap|tap|jerry|decoy>` - Refill a dungeon utility item to its target stack size
- `/pcc` - Shortcut for `/chat party`

## Features

### Category: MISC

- **Bazaar/AH Quick Search**: Search hovered items instantly on the Bazaar or Auction House.
- **Command Aliases**: Adds useful SkyBlock command shortcuts.
- **Compact Potion Message**: Shortens potion effect chat messages.
- **Cooldown Items**: Displays item ability cooldowns.
- **Copy Chat**: Right-click chat messages to copy their content.
- **Cursor 1.8.9**: Restores inventory cursor behavior closer to Minecraft 1.8.9.
- **Custom Hyperion Sound**: Replaces Wither Blade sounds with selectable custom sounds.
- **Exp Table Addons**: Keeps Experimentation Table items visible.
- **Inventory Buttons**: Adds configurable inventory buttons.
- **Item Price**: Shows item prices in tooltips.
- **Peek Chat**: Temporarily displays expanded chat without opening chat.
- **Spotify Overlay**: Displays the current Spotify track as a HUD element, with optional better display controls.
- **ZguegAddons**: Extra chat, dungeon, and party utilities.

### Category: DUNGEONS

- **AutoPet Title**: Replaces AutoPet equip chat messages with HUD notifications.
- **Bat Death Alert**: Alerts when a bat dies in dungeons.
- **Better Party Finder**: Improves Party Finder messages with player stats and quick actions.
- **Dungeon Not Full Alert**: Warns at dungeon start if the party is not full.
- **Invisibility Timer**: Tracks Bonzo's Mask, Spirit Mask, and Phoenix Pet invulnerability timers.
- **Item Quality**: Adds dungeon item quality information to item tooltips.
- **Mask Reminder**: Reminds you to equip Spirit Mask or Bonzo's Mask during relevant dungeon phases.
- **Milestone Reminder**: Displays dungeon milestone progress and sends reminders when needed.
- **Players Leaped**: Displays leap progress in F7 terminal-related zones.
- **Pos Message**: Sends party callouts when reaching configured dungeon spots.
- **Pos Titles**: Displays teammate callouts as movable HUD text.
- **Refill**: Checks your inventory and requests only the missing dungeon utility items with `/gfs`.
- **Reminder Gate**: Shows gate reminders based on lever progress messages.
- **SecretRoutes**: Renders dungeon routes, secrets, waypoints, and room helpers.

### Category: DEV

- **Config GUI**: Customize the GUI appearance and theme settings.
- **Cosmetics**: Applies custom names, player scaling, and cosmetics refresh with a 30s cooldown.
- **Edit HUD**: Move, scale, and reset HUD elements.
- **Debug**: Internal testing and debugging tools.

## Visual Words / Custom Data

XenAddons uses a unified cosmetics snapshot.

### Features:

- Visual name replacements
- Custom player scale
- UUID-based mapping for stable behavior across nick/display changes

### Cosmetics Portal

```txt
https://addons.xenus.fr/login
```

## Update Checker

- Checks for updates once per launch.
- Shows update status in chat.
- Can download the latest release directly into the `mods/` folder.
- Requires a restart after updating.

## Config File

```txt
.minecraft/config/xenaddons_config.json
```
