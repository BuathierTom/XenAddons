# XenAddons v1.2.0

Minecraft `1.21.10` • Fabric

## Added

- Visual Words now loaded from mod resources:
  - `assets/xenaddons/visual/visualWords.json`
- Custom player scales now loaded from mod resources:
  - `assets/xenaddons/visual/customSizes.json`
- Added custom capybara bitmap font integration for Visual Words:
  - `assets/xenaddons/font/capybara.json`
  - Used for `Antorak` display replacement (`{capy}` token)

## Changed

- Resource structure updated for visual config and assets:
  - `visualWords.json` and `customSizes.json` moved under `assets/xenaddons/visual/`
- Removed unstable butt-boost experimental rendering path
- Kept stable per-UUID full model scaling through `Custom Size`

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
- InvButtons
- ZguegAddons

### Dev
- Colors
- Edit HUD
- Debug

## Commands

- `/xa`
- `/xenaddons`
- `/invbuttons`
