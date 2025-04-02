# ForgeTowns

A Minecraft mod that adds town and nation management features to your server. This mod allows players to create and manage towns, form alliances through nations, and protect their territory through claimable land.

## Features

- Town System
  - Create and manage towns
  - Town menus for easy management
  - Claimable land for town expansion
  - Town-specific permissions and protections

- Nation System
  - Form alliances through nations
  - Nation commands for managing alliances
  - Nation-specific features and benefits

- Claim System
  - Protect your land from griefing
  - Claim management through GUI interface
  - Block protection for claimed areas

- Plot Management
  - Special plot wand item for managing plots
  - Creative mode tab integration
  - Plot-based permissions

## Requirements

- Minecraft 1.20.5+
- Java 21
- NeoForge mod loader

## Installation

1. Download the latest release from the [Releases](https://github.com/0xRogu/ForgeTowns/releases) page
2. Place the mod file in your `mods` folder
3. Start your server or game
4. The mod will automatically initialize

## Commands

### Town Commands
- `/town`: Base command for town management
- `/town create <name>`: Create a new town
- `/town join <town>`: Join an existing town
- `/town leave`: Leave your current town
- `/town claim`: Claim land for your town

### Nation Commands
- `/nation`: Base command for nation management
- `/nation create <name>`: Create a new nation
- `/nation join <nation>`: Join an existing nation
- `/nation leave`: Leave your current nation

## Development

This project uses Gradle for building. To build the mod:

```bash
./gradlew build
```

The compiled mod will be located in the `build/libs` directory.

## License

This project is licensed under the terms of the LICENSE file.