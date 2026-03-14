# XenAddons v1.2.14

Minecraft `1.21.10` • Fabric

## Fixed

- **Visual Words / Chat Compatibility**
  - Fixed disconnects caused by malformed legacy formatting interacting with chat-tab filters.
  - Hardened Visual Words text rebuilding to safely handle dangling `§` markers.
  - Improved sanitization in text replacement/render paths to avoid invalid formatted output.
