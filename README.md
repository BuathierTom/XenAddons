# XenAddons

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.10-green.svg)
![Fabric](https://img.shields.io/badge/Fabric-Mod-orange.svg)
![Java](https://img.shields.io/badge/Java-21+-blue.svg)
![License](https://img.shields.io/badge/License-CC0--1.0-lightgrey.svg)

**A Fabric mod for Minecraft 1.21.10, bringing back features from 1.8.9 Hypixel mods**

</div>

---

## 📋 Table of Contents

- [About](#-about)
- [Features](#-features)
  - [Dungeons](#-dungeons)
  - [Miscellaneous](#-miscellaneous)
  - [Development Tools](#-development-tools)
- [Installation](#-installation)
- [Usage](#-usage)
- [Configuration](#-configuration)
- [Commands](#-commands)
- [Building](#-building)
- [Contributing](#-contributing)
- [License](#-license)

---

## 🎮 About

**XenAddons** is a comprehensive Fabric mod designed for Minecraft 1.21.10 that ports popular features from 1.8.9 Hypixel Skyblock mods to modern Minecraft. It focuses on quality-of-life improvements, dungeon utilities, and party management tools.

---

## ✨ Features

### 🏰 Dungeons

#### **Auto Pet Rule Title**
Automatically displays pet rules in dungeons with customizable titles.
- Configurable title display
- Dungeon-specific pet rule detection
- Clean HUD integration

#### **Position Messages**
Sends your position in chat for dungeon coordination.
- Quick position sharing with party members
- Customizable message format
- Keybind support

#### **Position Titles**
Displays position information as titles on screen.
- Real-time position updates
- Customizable title format and colors
- Toggle-able display

---

### 🛠️ Miscellaneous

#### **Command Aliases**
Powerful command shortcuts system with individual toggles for each command group.

**Available Commands:**
- `/d`, `/dung` - Quick warp to dungeon hub
- `/ai` - Toggle party all invite
- `/pko` - Kick offline party members
- `/pd` - Disband party
- `/pk <player>` - Kick specific player from party
- `/pt <player>` - Transfer party to player
- `/lsb` - Leave server and join Skyblock (lobby → play sb)
- `/ld` - Leave server, join Skyblock, and warp to dungeon hub
- `/f1` to `/f7` - Quick join Catacombs floors 1-7
- `/m1` to `/m7` - Quick join Master Catacombs floors 1-7

**Features:**
- Individual toggle for each command group
- Clean settings interface
- Optimized command execution

#### **Inventory Buttons**
Adds customizable buttons to inventory screens.
- Quick access to common actions
- Customizable button positions
- Clean inventory integration

#### **ZguegAddons Features**
Collection of party commands and chat utilities.

**Chat Features:**
- **Chat Hider** - Hides radio messages and Discord warnings
- **Crypts Notify** - Announces when 5 crypts are reached in party chat
- **Class Notify** - Automatically announces your selected dungeon class

**Party Commands:**
All commands can be toggled individually:
- `!boom` - Sends random Quranic verses
- `!rng` - Calculates RNG percentage for players
- `!gay` - Fun gay percentage calculator (goated players protected)
- `!racist` - Racist percentage calculator (goated players get 180%+)
- `!py` - "Can't py big gwer" message
- `!gwer` - Random gwer messages
- `!size` - Size calculator (goated players get 15-25cm)
- `!911` - Sends ✈ | |

**Goated Players System:**
Special players get different results in commands. Managed through configuration.

---

### 🔧 Development Tools

#### **Colors Module**
Development utility for testing color codes and formatting.
- Color palette testing
- Format code preview
- Quick color reference

---

## 📦 Installation

1. **Download and install [Fabric Loader](https://fabricmc.net/use/)**
   - Minecraft version: 1.21.10
   - Loader version: 0.16.9 or higher

2. **Download [Fabric API](https://modrinth.com/mod/fabric-api)**
   - Place in your `mods` folder

3. **Download XenAddons**
   - Get the latest release from the releases page
   - Place the `.jar` file in your `mods` folder

4. **Launch Minecraft**
   - Select the Fabric 1.21.10 profile
   - Enjoy!

---

## 🎯 Usage

### Opening the GUI
Press `Right Shift` (default keybind) to open the XenAddons click GUI.

### Navigating the GUI
- Click on category panels to expand/collapse
- Toggle modules with the checkbox
- Click on module names to access settings
- Drag panels to reposition them

### Configuring Modules
1. Open the GUI with `Right Shift`
2. Click on a module name to open its settings
3. Toggle individual features on/off
4. Settings are saved automatically

---

## ⚙️ Configuration

Configuration files are stored in `.minecraft/config/xenaddons/`

### Main Config
- Module states (enabled/disabled)
- Keybinds
- GUI positions

### Goated Players
Edit `goated_players.json` to add players who get special treatment in ZguegAddons commands:
```json
{
  "players": [
    "player1",
    "player2"
  ]
}
```

---

## 📝 Commands

### Main Command
- `/xa` - Opens the XenAddons GUI

### Command Aliases
See the [Command Aliases](#command-aliases) section for the full list of available shortcuts.

---

## 🔨 Building

### Prerequisites
- Java 21 or higher
- Git

### Build Steps
```bash
# Clone the repository
git clone https://github.com/BuathierTom/XenAddons.git
cd XenAddons

# Build the mod
./gradlew build

# The built jar will be in build/libs/
```

### Development
```bash
# Run the client
./gradlew runClient

# Run with auto-reload
./gradlew runClient --continuous
```

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Guidelines
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## 📄 License

This project is licensed under the CC0-1.0 License - see the LICENSE file for details.

---

## 🙏 Acknowledgments

- Fabric team for the modding framework
- Hypixel Skyblock community
- All contributors and users

---

## 📞 Contact

**Author:** Xenus

**Issues:** Please report bugs and feature requests on the [GitHub Issues](https://github.com/BuathierTom/XenAddons/issues) page.

---

<div align="center">

**Made with ❤️ for the Hypixel Skyblock community**

</div>
