# XenAddons v1.2.4

Minecraft `1.21.10` • Fabric

## Added

- New **Dungeon** module: **Invisibility timer**
  - Tracks active invincibility time after:
    - Spirit Mask proc
    - Bonzo's Mask proc
    - Phoenix Pet proc
  - Displays only the active invincibility timer in milliseconds
  - Fully integrated with **Edit HUD** (move/scale/reset)

- New setting in **Invisibility timer**: **Mask procs alert**
  - Shows a large red center-screen alert on proc:
    - `BONZO`
    - `SPIRIT`
    - `PHOENIX`
  - Plays a loud DING sound on proc

- New HUD editor element: **Mask proc alert**
  - Separate placement/scale control for the proc alert text

- New debug command: `/xa procs`
  - Triggers mask proc alert + sound for testing

## Changed

- Bumped mod version to `1.2.4`
- Updated README with new Dungeon functionality and debug command list

