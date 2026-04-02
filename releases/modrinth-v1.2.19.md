## Added

- `Cosmetics API`
  - Cosmetics now load from `https://api.xenus.fr/`.
  - Added a shared snapshot layer for usernames, aliases, display names, and scale.

## Changes

- `Cosmetics source`
  - `assets/xenaddons/visual/cosmeticPeople.json` is now only used as a fallback if the API fails or returns invalid data.
  - Default endpoint now points to the live XenAddons API.

## Fixed

- `Cosmetics loading`
  - Network and JSON parsing failures are handled safely.
  - `displayName` and `scale` can now be `null` without breaking the mod.

## Links

- XenAddons portal login: https://addons.xenus.fr/login
