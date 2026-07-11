# XenAddons v1.3.8

Minecraft `26.1.2` • Fabric • Java `25+`

## Added

- **Refill Command**
  - Added the new `Refill` feature to the DUNGEONS category.
  - Added `/xa refill <pearl|tnt|leap|twap|tap|jerry|decoy>`.
  - Checks the current inventory before using `/gfs` and requests only the missing amount.
  - Refills Ender Pearls up to `16`, Twilight/Toxic Arrow Poison up to `8`, and all other supported items up to `64`.
  - Does not send a `/gfs` command when the target amount is already reached.

- **Item Price Tooltips**
  - Added pet Lowest BIN support by resolving pet prices from name + rarity, such as `ROCK_PET_LEGENDARY`.
  - Added Bazaar price support for enchanted books from NBT, item name, and lore.
  - Supports enchanted book displays like `Legion V`, `Enchanted Book (Legion V)`, and NBT enchantment data.

- **ZguegAddons Chat Hiders**
  - Added the `Ability damage` hider for ability hit messages such as Implosion damage lines.
  - Added the `Annoying messages` hider with support for `There are blocks in the way!`.

- **Discord Invite**
  - Added a clickable Discord invite message after the auto-update check finishes.
  - The invite is shown as `CLICK TO JOIN` in chat.

## Changed

- **Bazaar/AH Quick Search**
  - Improved enchanted book quick search so it searches the real enchantment name instead of `Enchanted Book`.
  - Expanded reforge cleanup with the updated Hypixel reforge list, including multi-word reforges.

- **Item Price Tooltips**
  - Enchanted books no longer show the `[SHIFT show x64]` hint.
  - Enchanted book Bazaar prices now stay unit-based instead of being multiplied by x64.

- **Cosmetics**
  - Updated the bundled cosmetics cache.
