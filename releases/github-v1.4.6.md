# XenAddons v1.4.6

Minecraft `26.1.2` • Fabric • Java `25+`

## Added

- **New category: Slayer**
  - **Slayer Boss HUD** — Shows the nametag of the slayer boss you spawned, so you can read its health without looking at it. Optional timer line.
  - **Boss Alert** — HUD alert and ping when your slayer boss spawns.
  - **Mini-Boss Alert** — HUD alert and ping when a slayer mini-boss spawns. Works for every slayer.
  - **Cocoon Alert** — HUD alert and ping when you cocoon a slayer mob, with an optional announce in party chat naming the mob.
  - The three alerts share the same settings: an editable text line (`&` colour codes supported), a movable and scalable HUD element, and the sound picker used by the mask proc alert.

- **No Glint**
  - Removes the enchantment glint from every item at once: items in GUIs, in hand, dropped, in item frames, worn armor and shields.
  - Your vanilla "Glint Strength" video setting is left untouched.

- **Private Message Format**
  - Rewrites private messages as `Msg > From [MVP+] Player: message`, with the message body in white so they stop blending into the rest of chat.
  - Ranks keep their original colours, and a normal chat message that happens to start with "From" is never touched.

## Changed

- **HUD editor**
  - Only shows the elements whose feature is actually enabled. A hidden element can no longer be dragged, scaled or moved by accident either.

- **Debug commands**
  - A dev command that is switched off no longer appears in `/xa` autocompletion, and cannot be run. The list refreshes as soon as you toggle it in the GUI.
  - `/xa procs` now respects its own `/xa procs` toggle, which previously had no effect.

## Fixed

- **Alert sound volume**
  - Raising the volume slider above `1` did nothing, because Minecraft caps a single sound instance at that value. The slider is now linear across its whole range, so `5` really is five times louder than `1`.
  - Applies to the slayer alerts and to the mask proc alert.
