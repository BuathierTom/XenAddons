# XenAddons

<div align="center">

[![Downloads](https://img.shields.io/modrinth/dt/xenaddons?style=for-the-badge&logo=modrinth&logoColor=007F43&label=Downloads&color=00AF5C)](https://modrinth.com/mod/xenaddons)
[![Discord](https://img.shields.io/discord/1473269453213925492?style=for-the-badge&logo=discord&logoColor=white&label=Discord&color=1E3A8A)](https://discord.gg/yRbJJwGTPG)
[![Version](https://img.shields.io/modrinth/v/xenaddons?style=for-the-badge&logo=modrinth&logoColor=white&label=Version&color=E67E22)](https://modrinth.com/mod/xenaddons)
[![Cosmetics Portal](https://img.shields.io/badge/Cosmetics-Portal-00AF5C?style=for-the-badge&logo=firefoxbrowser&logoColor=white)](https://addons.xenus.fr/login)

**A Fabric mod for Minecraft 26.1.2 focused on Hypixel SkyBlock QoL**

</div>

XenAddons is a Fabric mod for Minecraft **26.1.2** focused on Hypixel SkyBlock quality-of-life features.

## Compatibility

- Minecraft: `26.1.2`
- Mod loader: `Fabric`
- Java: `25+`

## Main Commands

- `/xa` - Open XenAddons GUI
- `/xenaddons` - Open XenAddons GUI
- `/xa hud` - Open HUD editor
- `/xa refill <pearl|tnt|leap|twap|tap|jerry|decoy> [amount]` - Refill a dungeon utility item to its default or custom target amount
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
- **Farming Tools Keys**: Swaps your movement and break keys while you hold a farming tool in the Garden, with a toggle keybind.
- **Hide Parts on Rod**: Hides the empty Hook, Line, and Sinker lines from fishing rod tooltips.
- **Inventory Buttons**: Adds configurable inventory buttons.
- **No Glint**: Removes the enchantment glint from every item.
- **Item Price**: Shows item prices in tooltips, including shards and sack contents.
- **Pet Level**: Shows a pet's level in the corner of its slot, hidden once it is maxed, with a color picker for the text.
- **Peek Chat**: Temporarily displays expanded chat without opening chat.
- **Private Message Format**: Rewrites private messages as `Msg > To/From <player>: message` with the text in white.
- **Spotify Overlay**: Displays Spotify on macOS and Spotify/Deezer media sessions on Windows, with an optional Liquid Glass HUD.
- **Visitor Garden**: Right-click a Garden visitor offer to search the Bazaar for the item it needs, with the amount copied to your clipboard.
- **Warps Shortcuts**: Warp with `/desert` instead of `/warp desert`, from a remote warp list with a bundled fallback.
- **ZguegAddons**: Extra chat, dungeon, and party utilities.

### Category: DUNGEONS

- **AutoPet Title**: Replaces AutoPet equip chat messages with HUD notifications.
- **Bat Death Alert**: Alerts when a bat dies in dungeons.
- **Better Party Finder**: Improves Party Finder messages with player stats and quick actions.
- **Dungeon Not Full Alert**: Warns at dungeon start if the party is not full.
- **Invisibility Timer**: Tracks Bonzo's Mask, Spirit Mask, and Phoenix Pet invulnerability timers.
- **Item Quality**: Adds dungeon item quality information to item tooltips.
- **Loadout Title**: Replaces loadout equip messages with a compact chat line and an optional HUD title, and highlights the equipped loadout in `/loadout` with a pickable color.
- **Milestone Reminder**: Displays dungeon milestone progress and sends reminders when needed.
- **Mute Bark Rune**: Mutes the wolf growl and bark sounds of the Bark Rune, optionally only in dungeons.
- **Players Leaped**: Displays leap progress in F7 terminal-related zones.
- **Pos Message**: Sends party callouts when reaching configured dungeon spots.
- **Pos Titles**: Displays teammate callouts as movable HUD text.
- **Refill**: Checks your inventory and requests only the missing items up to the default or custom target amount.
- **Reminder Gate**: Shows gate reminders based on lever progress messages.
- **SecretRoutes**: Renders dungeon routes, secrets, waypoints, and room helpers, with current and legacy route sets.
- **Show Ultimate Enchant**: Marks a Terminator carrying Soul Eater or Duplex directly on its item slot.

### Category: SLAYER

- **Slayer Boss HUD**: Shows your slayer boss's nametag and health on screen.
- **Boss Alert**: HUD alert and ping when your slayer boss spawns.
- **Mini-Boss Alert**: HUD alert and ping when a slayer mini-boss spawns.
- **Cocoon Alert**: HUD alert and ping when you cocoon a slayer mob, with an optional party chat announce.

### Category: DEV

- **Config GUI**: Customize the GUI appearance and theme settings.
- **Cosmetics**: Applies custom names, player scaling, dragon wings, and cosmetics refresh with a 30s cooldown.
- **Edit HUD**: Move, scale, and reset HUD elements.
- **Debug**: Internal testing and debugging tools.

## Visual Words / Custom Data

XenAddons uses a unified cosmetics snapshot.

### Features:

- Visual name replacements
- Custom player scale
- Dragon wings, with a per-player size and color
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
