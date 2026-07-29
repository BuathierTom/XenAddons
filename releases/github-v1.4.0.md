# XenAddons v1.4.0

Minecraft `26.1.2` • Fabric • Java `25+`

## Added

- **Spotify / Deezer on Windows**
  - Added a native Windows media-session bridge using Global System Media Transport Controls.
  - Added support for Spotify and Deezer metadata, playback state, progress, duration, and embedded album artwork on Windows.
  - Added native previous, play/pause, next, repeat, and shuffle controls without launching PowerShell.

## Changed

- **Spotify Better Display**
  - Progress now advances in real time between metadata refreshes on both Windows and macOS.
  - Media labels now follow the detected source, including Spotify and Deezer.
  - Added support for embedded Windows artwork and improved album-cover caching.
  - Removed the volume slider, mute button, and like button from the overlay.
  - Kept previous, play/pause, next, repeat, and shuffle controls.
  - Interactive controls are now available only while the chat or an inventory/container screen is open.

## Fixed

- **Spotify Better Display**
  - Prevented the overlay from intercepting clicks in XenAddons menus, the HUD editor, and other configuration screens.
  - Hidden the overlay during loading/progress screens and while the Minecraft HUD is disabled.
  - Stopped Liquid Glass background capture whenever the overlay should not be rendered.
  - Improved Windows media detection, artwork loading, progress tracking, and playback controls.
  - Reduced the Liquid Glass blur kernel and limited background capture to `30 FPS` to reduce GPU usage.
  - Combined macOS metadata reads into a single AppleScript process to reduce periodic stutters.
