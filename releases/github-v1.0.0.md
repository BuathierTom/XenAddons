# XenAddons v1.0.0

Minecraft `1.21.10` • Fabric

XenAddons est un mod Fabric orienté Hypixel SkyBlock, avec des modules Dungeon, Misc et Dev.

## Highlights

- Nouveau module HUD Dungeon: **Players Leaped**
- **Melody Warning** supprimé
- Éditeur HUD intégré pour déplacer/redimensionner les éléments supportés
- SecretRoutes avec rendu configurable (couleur/alpha/épaisseur/toggles)

## Modules

### DUNGEON

#### Auto Pet Rule Title
- Remplace le message chat AutoPet par un titre HUD centré
- Position et scale configurables via **Edit HUD**

#### Pos Message
- Envoie automatiquement les callouts de position en party chat

#### Pos Titles
- Affiche les callouts de position en HUD
- Position et scale configurables via **Edit HUD**

#### SecretRoutes
- Affiche routes/waypoints secrets en donjon
- Réglages de rendu:
  - `Render Routes`
  - `Full route`
  - `Render steps`
  - `All secrets`
  - `Render Lines`
  - `Line color`
  - `Line width`
  - `Box Alpha`
- Toggles de rendu:
  - `Etherwarps`
  - `Mines`
  - `Interacts`
  - `Superbooms`
  - `Enderpearls`
  - `Item secrets`
  - `Interact secrets`
  - `Bat secrets`

#### Players Leaped
- Affiche `x/4 Players Leaped` dans les zones F7 Terms
- Détection automatique du spot/phase F7
- Position et scale configurables via **Edit HUD**

### MISC

#### Command Aliases
- Alias:
  - `/d`, `/dung`
  - `/ai`
  - `/pko`
  - `/pd`
  - `/pk <player>`
  - `/pt <player>`
  - `/lsb`, `/ld`
  - `/f1` ... `/f7`
  - `/m1` ... `/m7`
- Toggle individuel par groupe de commandes

#### Custom Keybinds
- Keybinds configurables:
  - `Wardrobe`
  - `Equipment`
  - `Trades`
  - `Garden`
  - `Pets`

#### InvButtons
- Boutons custom dans l’inventaire
- Actions:
  - `Edit Buttons Position`
  - `Profiles & Advanced`
- Options:
  - `Show Tooltips`
  - `Hide Recipe Book`

#### ZguegAddons
- Chat:
  - `Chat Hider`
  - `Radio Msg`
  - `Discord Warn`
- Donjon:
  - `Crypts Notify`
  - `Class Notify`
- Party commands (activables individuellement):
  - `!boom`, `!rng`, `!gay`, `!racist`, `!py`, `!gwer`, `!size`, `!911`

### DEV

#### Colors
- Couleur thème du GUI

#### Edit HUD
- Ouvre l’éditeur HUD
- Drag & drop, scale molette, reset placement

#### Debug
- Toggles debug:
  - `/xa title`
  - `/xa petrule`
  - `/xa ticlass`
  - `/xa secretroom`

## Commandes principales

- `/xa`
- `/xenaddons`
- `/invbuttons`

## Compatibilité

- Minecraft: `1.21.10`
- Loader: Fabric
- Java: `21+`
