# Particles

A Minecraft Bukkit/Spigot plugin that creates floating particle above you and causes damage (like a death ray).

## Description

This plugin allows you to make a death ray possible in minecraft!

## Features

- Blue flames
- Customizable text height, spacing, and size
- Different particle effects for main text and subtitle (FLAME and SOUL_FIRE_FLAME)
- Infinitely loopable
- OP-only permission system

## Commands

| Command | Description | Usage |
|---------|-------------|-------|
| `/sbeam` | playername/pos | `/sbeam [playername] or [pos]` |

**Example:**
```
/sbeam Its_BiBi
```

## Installation

1. Download the latest BEAM.jar from the releases section
2. Place the JAR file in your server's `plugins` folder
3. Restart your server or use a plugin manager to load the plugin
4. Use the `/sbeam` command as a server operator

## Requirements

- Bukkit/Spigot/Paper Minecraft server (1.21+)
- Server operator permissions to use the command

## Configuration

- `Height`: Height above the player for the main text (blocks)
- `Damage`: DPS - Damage per second
- `Radius`: Radius where damage is felt
- `Particle Density`: Closeness of particles

## Notes
- The plugin is designed for server operators only
