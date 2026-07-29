## Added

- **Spotify / Deezer on Windows**
  - Added native Windows media-session support for Spotify and Deezer.
  - Added metadata, playback state, real-time progress, duration, embedded album artwork, and playback controls without PowerShell.

## Changed

- **Spotify Better Display**
  - Progress now advances in real time on Windows and macOS.
  - Media labels now follow the detected source.
  - Removed the volume, mute, and like controls.
  - Kept previous, play/pause, next, repeat, and shuffle controls.
  - Controls are now clickable only from the chat or an inventory/container screen.

## Fixed

- **Spotify Better Display**
  - Prevented the overlay from intercepting clicks in mod configuration screens and the HUD editor.
  - Hidden the overlay and stopped Liquid Glass capture during loading screens or while the HUD is disabled.
  - Improved Windows media detection, album artwork, progress, and controls.
  - Reduced Liquid Glass GPU usage and periodic macOS metadata stutters.
