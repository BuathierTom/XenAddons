## Added

- `Invisibility timer` display mode selector (`Seconds` / `Ticks`)
- `Invisibility timer` proc sound customization (`Sound`, `Volume`, `Pitch`, `Note play`)
- `/xa procs <spirit|bonzo|phoenix>` command (direct proc alert + timer test)

## Changes

- Pos Message safespots updated with exact point checks:
  - Early Enter 2
  - Early Enter 3
- Added pos point message: `At pew pew dev!`
- Removed class restriction on `At P2` and `At Pre4 Entry`
- Invisibility proc durations:
  - Spirit `3s`
  - Bonzo `3s`
  - Phoenix `4s`
- Invisibility proc labels now use `Spirit`, `Bonzo`, `Phoenix`
- `/ld` waits longer before `/warp dungeon_hub`

## Fixes

- Improved invisibility timer proc behavior to reduce repeated alert/sound retriggers while a proc timer is already active.
