# XenAddons v1.2.19

Minecraft `1.21.10` • Fabric

## Added

- **Cosmetics / API**
  - Added live cosmetics loading from `https://api.xenus.fr/`.
  - Added a shared cosmetics snapshot layer for usernames, aliases, display names, and player scale.

## Changed

- **Cosmetics / Source Priority**
  - `assets/xenaddons/visual/cosmeticPeople.json` is now fallback-only when the API is unavailable or invalid.
  - Default cosmetics endpoint now targets the live XenAddons API instead of the local development URL.

## Fixed

- **Cosmetics / Stability**
  - Network failures and invalid JSON no longer hard-fail cosmetics loading.
  - `displayName = null` and `scale = null` are handled safely.

## Links

- **XenAddons Portal**
  - Login: https://addons.xenus.fr/login
