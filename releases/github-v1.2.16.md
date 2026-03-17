# XenAddons v1.2.16

Minecraft `1.21.10` • Fabric

## Added

- **Dungeon / Reminder Gate**
  - Added a new `Reminder Gate` module.
  - Trigger logic based on lever messages (`(x/7)` and `(x/8)`) with phase/class conditions.
  - Added customizable HUD text, custom sound list, random sound mode, and sound preview.

- **Dungeon / Mask Reminder**
  - Added a new `Mask Reminder` module using dungeon leap regions.
  - Triggers on Goldor start line or lever/terminal/device messages while inside configured regions.
  - Added mask-equipped checks (`Spirit Mask` / `Bonzo's Mask`) and one-time-per-run behavior.

- **Dev / Debug Commands**
  - Added `/xa gate` debug test flow behind Debug settings.
  - Added `/xa mask`, `/xa mask has`, `/xa mask nomask` debug helpers.

## Changed

- **Dungeon / Reminder Gate**
  - Improved gate-break handling to avoid reminder re-triggering on the second lever when the gate is already destroyed.

- **Dungeon / Mask Reminder**
  - Reminder display duration tuned to `1s` (`20 ticks`).
