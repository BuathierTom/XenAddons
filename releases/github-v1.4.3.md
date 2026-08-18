# XenAddons v1.4.3

Minecraft `26.1.2` • Fabric • Java `25+`

## Added

- **Cosmetics**
  - Display names can now be written as JSON text components, which supports `shadow_color` — something the legacy `&#RRGGBB` codes cannot express.
  - Accepted both as an inline JSON object and as an escaped string.
  - The legacy format still works, so entries can be converted one at a time.

## Fixed

- **Better Party Finder**
  - Player stats failed to load entirely. The stats API moved to a new host and the redirect was not being followed.

- **Cosmetics**
  - Custom emojis in display names appeared twice in chat messages received from the server.
