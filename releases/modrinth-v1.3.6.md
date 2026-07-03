## Added

- **Minecraft 26.1.2 Support**
  - Added support for Minecraft `26.1.2`.
  - Updated Fabric Loader, Fabric API, Loom, Kotlin, Gradle, and LWJGL/NanoVG dependencies.

## Changed

- **Rendering**
  - Migrated GUI, HUD, chat, item, world, sound, camera, and cooldown compatibility code for `26.1.2`.
  - Updated NanoVG render state handling for the newer Minecraft render pipeline.

- **Click GUI / HUD**
  - Adjusted Click GUI sizing and positioning after the migration.
  - Improved HUD editor and config screen rendering behavior.

- **Inventory Buttons**
  - Updated inventory button config screens for the new render flow.
  - Improved custom texture lookup and safer item/skull fallback handling.

- **Cursor 1.8.9**
  - Reduced forced cursor resets outside dungeons.
  - Kept hard cursor centering behavior available for dungeon mode.

## Fixed

- **Launch / Mixins**
  - Fixed broken mixin targets for chat, handled screens, HUD rendering, fluid rendering, and related compatibility hooks.

- **Inventory Buttons**
  - Fixed a crash when opening the inventory button config menu on `26.1.2`.
  - Added safer handling for missing or invalid custom button items.

- **GUI**
  - Fixed GUI scale/framing issues caused by the Minecraft `26.1.2` render changes.
  - Fixed several render ordering issues in screens and HUD elements.

- **Data / Items**
  - Fixed lazy Hypixel item stack creation for the new item/component behavior.
