# XenAddons v1.2.10

Minecraft `1.21.10` • Fabric

## Added

- **Dungeon**
  - `Invisibility timer`
    - Added display mode selector: `Seconds` (`3.000`) or `Ticks` (`60t`).
    - Added configurable proc sound settings:
      - sound selector
      - `Sound Volume` up to `5.0`
      - `Sound Pitch`
      - `Note play` preview button
  - Added `/xa procs <spirit|bonzo|phoenix>` command to trigger proc alert + timer directly.

## Changed

- **Dungeon / Pos Message**
  - Updated Early Enter 2 safespot detection to exact point checks.
  - Updated Early Enter 3 safespot detection to exact point checks.
  - Added new point message: `At pew pew dev!`.
  - Removed class restriction on `At P2` and `At Pre4 Entry` callouts.

- **Dungeon / Invisibility timer**
  - Proc durations updated:
    - Spirit: `3s`
    - Bonzo: `3s`
    - Phoenix: `4s`
  - Proc labels now display as `Spirit`, `Bonzo`, `Phoenix`.
  - Timer color thresholds refined:
    - green: `3000 -> 1500`
    - yellow: `1500 -> 700`
    - red: `700 -> 0`

- **Misc / Command Aliases**
  - `/ld` now waits longer before running `/warp dungeon_hub`.

## Documentation

- Bumped mod version to `1.2.10`.
- Updated `README.md`.
- Updated Modrinth long description (`releases/-description-modrinth.md`).
