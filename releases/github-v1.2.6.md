# XenAddons v1.2.6

Minecraft `1.21.10` • Fabric

## Added

- **Core command**
  - Added `/pcc` shortcut that runs `/chat party`

- **Dungeon**
  - Added `Dungeon Not Full Alert`
  - Added locked chest support in `SecretRoutes`:
    - when a chest is locked, a waypoint is shown to the linked lever

## Changed

- **SecretRoutes**
  - Route now hides when room secrets are completed
  - Completion state is kept when leaving/re-entering the same room
  - Improved fallback handling for secret progression (interact/bat/item cases)

- **VisualWords**
  - Updated custom gradient text styling for special name formatting

- **Custom Size**
  - Toggleable from `Dev` category
