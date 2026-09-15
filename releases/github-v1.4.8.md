# XenAddons v1.4.8

Minecraft `26.1.2` • Fabric • Java `25+`

## Added

- **Warps Shortcuts** (MISC)
  - Warp with `/desert` instead of `/warp desert`. The shortcuts are real client commands, so they show up in autocompletion.
  - A shortcut can point somewhere else than its own name: `/atoll` warps to `lotus`, `/ch` to `crystals`, `/wolf` to `howl`.
  - The list comes from `api.xenus.fr/v1/skyblock/warpshortcuts`, fetched once per launch, with `assets/xenaddons/skyblock/warpShortcuts.json` bundled as fallback if the API is unreachable.
  - Turning the module off hides the shortcuts from autocompletion and sends what you typed to the server untouched, instead of swallowing it.
  - A shortcut whose name is already used by another XenAddons command is skipped, so nothing you already have gets overridden.

- **Farming Tools Keys** (MISC)
  - Gives you separate forward, back, left, right, and break keys while you hold a farming tool, so you no longer have to edit your controls when you farm.
  - Only in the Garden, and only while the tool is in hand. Switch to anything else and your normal keys are back.
  - A toggle keybind (unbound by default) turns the module on and off in game, with a chat line telling you which way it went.

- **Mute Bark Rune** (DUNGEONS)
  - Mutes the wolf growl and bark sounds played by the Bark Rune, with an "Only in Dungeons" option on by default.

- **Cosmetics: Custom Wings** (DEV)
  - Dragon wings on players who have them, with a size and a color read per player from the cosmetics API.
  - Sits behind its own "Custom Wings" toggle in the Cosmetics module.

## Changed

- **Loadout Title**
  - New "Highlight Active Loadout" option, on by default, tinting the background of the equipped loadout in the `/loadout` menu.
  - The highlight is a background behind the item, not an outline drawn over it, so the icon stays readable.
  - The color is a picker, apple green by default.

- **Pet Level**
  - The level text now has its own color picker.

- **Item Price**
  - Lowest BIN is no longer shown when you hover an item in the `/loadout` menu.

## Notes

- Warps Shortcuts is off by default, like every other module. The shortcut list refreshes on game start only.
