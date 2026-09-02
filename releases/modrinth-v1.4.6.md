## Added

- **New category: Slayer**
  - **Slayer Boss HUD** — Shows the nametag of the slayer boss you spawned, so you can read its health without looking at it.
  - **Boss Alert**, **Mini-Boss Alert** and **Cocoon Alert** — HUD alert and ping on spawn or on catch, with an editable text, a movable HUD element, and an optional party announce for cocoons.

- **No Glint**
  - Removes the enchantment glint from every item at once, without touching your vanilla Glint Strength setting.

- **Private Message Format**
  - Rewrites private messages as `Msg > From [MVP+] Player: message` with the text in white.

## Changed

- The HUD editor now only shows the elements whose feature is enabled.
- A disabled dev command no longer shows up in `/xa` autocompletion, and the list refreshes as soon as you toggle it.

## Fixed

- Alert volume sliders did nothing above `1` because Minecraft caps a single sound at that value. They are now linear across the whole range, for both the slayer alerts and the mask proc alert.
