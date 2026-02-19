# XenAddons v1.2.3

Minecraft `1.21.10` • Fabric

## Changed

- **Click GUI scroll**
  - Reworked to panel-based handling (Odin-style behavior)
  - Scroll now targets the hovered column/panel instead of affecting unrelated content

- **Dev / Config GUI**
  - Added `Custom HUD Font` toggle in **Config GUI**
  - GUI theme color remains configurable in **Config GUI**
  - Removed `Edit HUD` from the Dev config list

- **ZguegAddons settings layout**
  - Added section grouping:
    - `Notify` (Crypts Notify, Class Notify)
    - `Chat Hider` (Radio Msg, Discord Warn)
    - `Commands` (`!` command toggles)
  - Removed legacy parent toggle entries for Chat Hider / Commands

- **InvButtons actions**
  - Action labels changed from generic `Run` style to:
    - `Edit buttons`
    - `Configuration menu`

## Fixed

- Improved runtime GUI color application flow
- Fixed formatted HUD text rendering parser edge cases (`§` formatting)
- Increased custom HUD font readability (larger rendering size)

## Removed

- `Custom Keybinds` module removed from module initialization/list
- SecretRoutes temporary `Render Toggle` action button removed
