# XenAddons v1.5.0

Minecraft `26.1.2` • `26.2` • `26.3` • Fabric • Java `25+`

## Added

- **Minecraft 26.2 and 26.3 support**
  - XenAddons now ships one jar per Minecraft version: `xenaddons-1.5.0-mc26.1.2.jar`, `xenaddons-1.5.0-mc26.2.jar` and `xenaddons-1.5.0-mc26.3.jar`. Pick the one that matches your game.
  - Every feature is available on all three versions.

- **Compact Stash** (MISC)
  - Replaces Hypixel's stash reminders (`You have X items stashed away!`, the material version with its totals line, and `One or more items didn't fit in your inventory…`) with a single line: `Item Stash > You have X items stashed away!`, or `Material Stash >` for materials.
  - The line is clickable and runs `/pickupstash` directly instead of opening `/viewstash`.
  - The empty lines Hypixel puts around the reminder are removed as well.

- **Invincibility Display** (DUNGEONS)
  - HUD showing the Spirit Mask, Bonzo's Mask and Phoenix Pet heads, each with its invincibility time in gold, its cooldown in red, or a green ✔ when it's ready. Based on Odin's Invincibility Timer.
  - Bonzo's Mask cooldown is read from your helmet's lore when available.
  - "Displayed Items" chooses which of the three are shown. "Invincibility Hud" holds the HUD toggle, a "Show" mode (Always, Any, When Active, On Cooldown) and the "Equipped Mask" color.
  - A small bar in that color marks what will save you: the mask you're wearing, or the Phoenix when it's your active pet and you have no mask on.
  - The heads use your skins: Panda and Wisp for the Spirit Mask, Jester for Bonzo's Mask, and Flamingo, Ice and Seagull for the Phoenix. Masks are read from your helmet or inventory. The Phoenix is picked up from `/pets` and from Autopet and summon messages.
  - Only shown in dungeons.

## Changed

- **HUD system**
  - Rewritten from scratch. Every HUD element is now placed from a single editor (`/xa hud`): drag to move, scroll to scale, Esc to save.
  - Optional snap grid with an adjustable size, and a "Reset" button, with a confirmation, that puts every element back to its default spot.
  - HUD elements keep the same size whatever your GUI scale is.

- **Players Leaped**
  - Reworked from NoammAddons' leap counter. Tracks SS, EE2, high EE2, EE3, Core, inside Core and the Relic spot.
  - The expected count follows your party size.
  - New "Everyone Leaped!" title when everyone is there, with an editable text, a selectable sound, volume, pitch and a test button.

- **Reminder Gate**
  - New detection: the gate of your Phase 3 section is read straight from the world instead of being guessed from chat messages.
  - No more false reminder when the gate was blown before the first lever, or in the last section, which has no gate.
  - The reminder disappears as soon as the gate goes down.

- **Loadout Title**
  - New "Hide None Parts" option, on by default, that hides the `None` lines from the Pet line down in loadout tooltips.

- **Invincibility Timer**
  - Renamed from "Invisibility Timer".

- **Default modules**
  - On a new config, Cosmetics, Config GUI, ZguegAddons (with all its options) and Command Aliases are now on.
  - Config GUI can no longer be turned off.

## Fixed

- **Item Price**
  - Runes and unique runes now show their Lowest BIN.

## Notes

- The new HUD system resets every HUD position. Place your elements again once with `/xa hud`.
- Because of the rename, Invincibility Timer's settings and HUD position are back to default.
