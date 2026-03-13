## Fixes

- `Pos Titles` (Dungeon)
  - Fixed a crash (`NullPointerException`) that could happen while processing chat messages during connection transitions.
  - Added null checks to prevent alert handling when `mc.player` is temporarily null.
