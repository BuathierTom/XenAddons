# XenAddons v1.3.5

Minecraft `1.21.11` • Fabric

## Added

- **Click GUI**
  - New NanoVG-based config GUI.
  - Improved search matching across feature names, settings, options, and keywords.
  - Better HUD editor access from the config menu.

- **Cosmetics**
  - Updated bundled fallback cosmetics snapshot.

## Changed

- **Inventory Buttons**
  - Renamed `InvButtons` to `Inventory Buttons`.
  - Added compatibility for the old config entry name.

- **Experimentation Table**
  - Renamed `Experimentation Table Solver` to `Exp Table Addons`.
  - Removed Chronomatron, Ultrasequencer, and Superpairs solver behavior.
  - Kept only the `Keep Items Visible` option.

- **Build**
  - Release artifacts now include the Minecraft version in the archive name.

## Fixed

- **Spotify Overlay**
  - Fixed Windows title decoding for Russian and other special characters.
  - Improved truncation so special characters are not split mid-character.

- **Cosmetics**
  - Added a 30s cooldown to the refresh button to prevent spam.

- **GUI**
  - Improved config menu sizing and category drag bounds.

- **1.21.11 Compatibility**
  - Cleaned up render, chat, camera, sound, and cooldown compatibility code.
