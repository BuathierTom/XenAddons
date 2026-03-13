# XenAddons v1.2.11

Minecraft `1.21.10` • Fabric

## Added

- **Dungeon**
  - `Item Quality`
    - Added dungeon tooltip line:
      - `Quality Bonus: +...% (...)`
    - Quality line is now displayed directly under item rarity.

- **Misc**
  - `Bazaar/AH Quick Search`
    - Added hovered-item quick search keybind (default `M`).
    - Automatically chooses `/bz` or `/ahs`.

## Changed

- **Misc / Bazaar/AH Quick Search**
  - Improved search text sanitization:
    - removes reforges (e.g. `Ancient`, `Gentle`, etc.)
    - removes decorative symbols/logos (dyes/custom symbols)
    - keeps clean item name for better query accuracy.

- **Config GUI / Keybinds**
  - Pressing `ESC` while recording a keybind now sets `Not Bound` and does not close the config menu.
  - `Backspace` while recording also sets `Not Bound`.
