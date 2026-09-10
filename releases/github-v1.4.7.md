# XenAddons v1.4.7

Minecraft `26.1.2` • Fabric • Java `25+`

## Added

- **Pet Level**
  - Draws a pet's level in the top left corner of its slot, so you can read it without hovering.
  - A maxed pet shows nothing. The cap is per pet: 200 for Golden and Jade Dragons, 100 for everything else — the Ender Dragon included, despite the name.
  - Works everywhere an item is drawn: inventory, menus, Auction House results.

## Changed

- **Item Price**
  - Shard prices now show up in the shard menu. The entry there is named after the attribute alone ("Foxtrot"), so the `COMMON FOREST SHARD (ID C49)` line at the bottom of the tooltip is what identifies it. Shards held as an item already worked.
  - SHIFT now multiplies by what you actually hold rather than by a vanilla stack:
    - in a sack it uses the `Stored:` amount,
    - in the shard menu it uses the `Owned:` amount.
  - The SHIFT line is hidden when you only hold one, since `x1` says nothing.

## Fixed

- **Item Price**
  - Lowest BIN prices now come from `api.xenus.fr` instead of the third party endpoint used before.
