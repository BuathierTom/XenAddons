## Added

- **Spotify Better Display**
  - Added a Liquid Glass HUD with album artwork, track information, time, and animated progress.
  - Added volume, mute, previous, play/pause, next, repeat, shuffle, and like controls.

- **Loadout Title**
  - Added a DUNGEONS feature that replaces loadout equip messages with a compact chat line and a short HUD title.
  - Added an optional `In Dungeons Only` setting.

- **SecretRoutes: Old Routes**
  - Added an `Old Routes` option to switch between the new and legacy route sets.
  - The current room reloads immediately when the selected route set changes.

## Changed

- **Refill**
  - Added custom target amounts with `/xa refill <item> [amount]`.
  - Enabled the module by default and improved item detection and command speed.
  - Removed the refill-start chat message.

- **SecretRoutes**
  - Updated route and secret-location data.
  - Added the `Full route`, `All secrets`, and `Player waypoint line` settings to the config.
  - Changed the default line width to `2`.
  - The `Start` label is now shown only for the first secret.

## Fixed

- **SecretRoutes**
  - Improved dungeon room scanning, retry behavior, run resets, and floor detection.
  - Prevented stale route loads when switching between current and old routes.
