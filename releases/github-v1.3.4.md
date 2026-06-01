# XenAddons v1.3.4

Minecraft `1.21.10` • Fabric

## Fixed

- **Better Party Finder**
  - Fixed dungeon stats lookup through `https://api.odtheking.com/hypixel/get/`.
  - Added a Mojang UUID lookup fallback when Minecraft Services lookup fails.
  - Fixed UUID format handling between dashed UUIDs and Hypixel member keys.
  - Failed UUID lookups are no longer cached for the whole session.
