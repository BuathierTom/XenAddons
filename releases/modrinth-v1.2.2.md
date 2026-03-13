## New

- **Spotify Overlay** (Misc)
  - Colored now-playing HUD: `Spotify > Author - Song`
  - Editable through **Edit HUD** (drag/scale/reset)
  - Left-anchored text to prevent horizontal shifting with long titles
  - Tab list remains visually above the overlay

- **Update Checker**
  - Runs on first join each launch
  - Checks latest Modrinth version
  - Auto-downloads latest jar into `mods/` when update is available

## Improvements

- **Cursor 1.8.9**: added `Hard in dungeons only` mode
- **Item Price**:
  - Bazaar from Hypixel API
  - Lowest BIN fallback from Noamm API
  - Better spacing in tooltip section

## Fixes

- Fixed update-checker wrong-thread crash for chat messages
- Improved HUD bounds handling in small window mode (prevents off-screen text)
