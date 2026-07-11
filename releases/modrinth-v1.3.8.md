## Added

- **Refill Command**
  - Added the new `Refill` feature to the DUNGEONS category.
  - Use `/xa refill <pearl|tnt|leap|twap|tap|jerry|decoy>` to refill supported dungeon utility items.
  - The command checks your inventory and requests only the missing amount with `/gfs`.
  - Ender Pearls refill up to `16`, Twilight/Toxic Arrow Poison up to `8`, and all other supported items up to `64`.
  - No `/gfs` command is sent when the target amount is already reached.

- **Item Price Tooltips**
  - Added Lowest BIN support for pets using their rarity-specific API prices.
  - Added Bazaar price support for enchanted books from NBT, display name, and lore.

- **Chat Hiders**
  - Added `Ability damage` for ability hit messages such as Implosion damage lines.
  - Added `Annoying messages`, including `There are blocks in the way!`.

- **Discord**
  - Added a clickable `CLICK TO JOIN` Discord invite after the auto-update check.

## Changed

- **Bazaar/AH Quick Search**
  - Improved enchanted book searches so the keybind searches the real enchantment name.
  - Expanded reforge cleanup with the updated Hypixel reforge list.

- **Item Price Tooltips**
  - Enchanted books no longer show `[SHIFT show x64]` and stay unit-priced.
