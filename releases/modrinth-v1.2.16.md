## Added

- `Reminder Gate` (Dungeon)
  - Added a gate reminder module triggered by lever progress messages (`(x/7)`, `(x/8)`).
  - Includes class-based phase logic, customizable text, custom sound list, random mode, and sound preview.

- `Mask Reminder` (Dungeon)
  - Added a region-based mask reminder module using the same leap regions.
  - Triggers on Goldor start / lever / terminal / device messages while in region.
  - Checks for `Spirit Mask` / `Bonzo's Mask` equipped and only reminds once per run.

- Debug commands
  - Added `/xa gate`.
  - Added `/xa mask`, `/xa mask has`, `/xa mask nomask`.

## Changes

- `Reminder Gate`
  - Gate-break behavior improved so reminder does not retrigger incorrectly on the second lever after gate destruction.

- `Mask Reminder`
  - Reminder display duration set to `1s` (`20 ticks`).
