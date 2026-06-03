# Particles

A Minecraft Bukkit/Spigot plugin that creates floating particle text displays in-game using particle effects.

## Description

CoordsPlugin allows server operators to create dramatic floating text announcements using particle effects. Display custom messages that appear above the player's location with configurable text sizing, spacing, and particle effects.

## Features

- Create floating text displays using particles
- Support for all uppercase letters (A-Z) and spaces
- Main title and subtitle text with different particle effects
- Customizable text height, spacing, and size
- Different particle effects for main text and subtitle (FLAME and SOUL_FIRE_FLAME)
- 15-second duration for text display
- OP-only permission system

## Commands

| Command | Description | Usage |
|---------|-------------|-------|
| `/intro` | Display floating particle text | `/intro [main_text] [subtitle]` |

**Example:**
```
/intro WELCOME_TO_SERVER NEW_PLAYERS
```
This creates "WELCOME TO SERVER" as the main text and "NEW PLAYERS" as the subtitle.

## Installation

1. Download the latest CoordsPlugin.jar from the releases section
2. Place the JAR file in your server's `plugins` folder
3. Restart your server or use a plugin manager to load the plugin
4. Use the `/intro` command as a server operator

## Requirements

- Bukkit/Spigot/Paper Minecraft server (1.21+)
- Server operator permissions to use the command

## Configuration

The plugin doesn't have an external configuration file, but you can modify these parameters in the code:

- `mainHeight`: Height above the player for the main text (blocks)
- `subtitleOffset`: Distance between main text and subtitle (blocks)
- `mainParticle` & `subtitleParticle`: Particle types for each text
- `mainSpacing` & `subtitleSpacing`: Spacing between letters
- `mainSize` & `subtitleSize`: Size of each character
- `mainParticleCount` & `subtitleParticleCount`: Particle density
- `duration`: Effect duration in ticks (20 ticks = 1 second)

## Notes

- Text can only contain uppercase letters A-Z and spaces
- Use underscores in your command to represent spaces (they'll be converted automatically)
- The plugin is designed for server operators only
