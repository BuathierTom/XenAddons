# XenAddons v1.3.9

Minecraft `26.1.2` • Fabric • Java `25+`

## Added

- **Spotify Better Display**
  - Added a new Liquid Glass HUD inspired by MusicDisplay.
  - Displays album artwork, track title, artist, elapsed time, duration, and animated progress.
  - Added interactive volume, mute, previous, play/pause, next, repeat, shuffle, and like controls.
  - Added dedicated image, rounded rectangle, Liquid Glass, and MSDF font rendering pipelines.

- **Loadout Title**
  - Added the new `Loadout Title` feature to the DUNGEONS category.
  - Replaces Hypixel loadout equip messages with a compact chat message and a short HUD title.
  - Added an `In Dungeons Only` option for the HUD notification.

- **SecretRoutes: Old Routes**
  - Added an `Old Routes` option to switch between the new and legacy route sets.
  - Switching route sets immediately reloads the current room from the first secret.

## Changed

- **Refill**
  - Added an optional custom target amount: `/xa refill <item> [amount]`.
  - Enabled the `Refill` module by default.
  - Improved item counting and SkyBlock ID detection for faster command execution.
  - Removed the refill-start chat message and kept only the already-at-maximum notification.

- **SecretRoutes**
  - Updated the current route and secret-location data.
  - Exposed the `Full route`, `All secrets`, and `Player waypoint line` settings.
  - Changed the default route line width to `2`.
  - The `Start` label is now displayed only on the first secret of a route.

## Fixed

- **SecretRoutes**
  - Improved room scanning and retry behavior when dungeon chunks are not ready yet.
  - Fixed dungeon state resets when leaving a run.
  - Improved Catacombs floor detection from formatted scoreboard lines.
  - Prevented outdated asynchronous route loads from replacing the selected route set.
