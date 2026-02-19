# XenAddons v1.2.2

Minecraft `1.21.10` • Fabric

## Added

- New **Spotify Overlay** module (Misc)
  - Displays: `Spotify > Author - Song` with custom colors
  - Integrated into **Edit HUD** (drag + scale + reset)
  - Stable left-anchored layout (no horizontal shift with long titles)
  - Tab list now stays visually above the overlay

- New **Update Checker** (first join each launch)
  - Chat message: update check status
  - Compares current mod version with latest Modrinth version
  - Auto-downloads latest `.jar` to `mods/` when update is available

## Changed

- **Cursor 1.8.9**
  - Added `Hard in dungeons only`
  - When enabled, cursor recenters immediately when it becomes visible in dungeons

- **Item Price**
  - Bazaar uses Hypixel API
  - Lowest BIN uses Noamm API fallback when item is not bazaar
  - Added spacing before price section in item tooltips

## Fixed

- Fixed update-checker thread issue (`RenderSystem called from wrong thread`)
  - Chat messages are now dispatched on the client thread

- Improved HUD positioning consistency on small window sizes
  - Added on-screen clamping for:
    - Pos Titles
    - Players Leaped
    - AutoPet notification
    - Spotify Overlay

## Included modules

### Dungeon
- Auto Pet Rule Title
- Pos Message
- Pos Titles
- SecretRoutes
- Players Leaped

### Misc
- Command Aliases
- Custom Keybinds
- Cursor 1.8.9
- Custom Size
- Copy Chat
- Item Price
- InvButtons
- Spotify Overlay
- ZguegAddons

### Dev
- Colors
- Edit HUD
- Debug

## Commands

- `/xa`
- `/xenaddons`
- `/invbuttons`
