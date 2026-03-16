# XenAddons v1.2.8

Minecraft `1.21.10` • Fabric

## Added

- **Misc**
  - `Lava Remplacement`
    - Lava is rendered like water (visual replacement).
  - `Compact Potion Message`
    - Potion gain chat lines are compacted to short colored messages.
  - `Cooldown Items`
    - SkyHanni-style ability cooldown overlays:
      - ready `R` state
      - colored cooldown text
      - inventory visibility option
    - Added mage cooldown reduction handling in dungeons.
  - `Custom Hyperion Sound`
    - Custom Wither Blade hype sound replacement.
    - Sound selector with short labels.
    - Volume/Pitch range `0.0 -> 2.0` with `0.1` step.
    - `Play Sound` button in config.

## Changed

- `Copy Chat`
  - Added formatted copy using `Alt/Option + right-click`.
- `Spotify Overlay`
  - Long song titles are truncated with `...` in HUD.
- `Item Price` tooltip
  - `[SHIFT show x..]` hint hides while Shift is pressed and returns on release.
- Removed `Players Leaped` from active module registration.

## Fixes

- Custom Hyperion Sound:
  - Improved Wither Blade click detection and sound suppression window.
  - Mutes base hype explosion/teleport/cure/fizz sounds while playing custom sound.
