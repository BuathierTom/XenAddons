# XenAddons v1.2.13

Minecraft `1.21.10` • Fabric

## Fixed

- **Dungeon / Pos Titles**
  - Fixed a crash (`NullPointerException`) that could occur while receiving chat packets during join/disconnect transitions.
  - Added null-safety guards when handling chat/title alerts if `mc.player` is temporarily unavailable.
