---
title: Getting Started
---

# Getting Started

A Paper/Folia plugin port of [KawaMood's Racks data pack](https://modrinth.com/datapack/racks) that
adds wooden display racks for tools and weapons. Racks live in a database, so they survive a world
upgrade and never depend on command storage.

## Requirements

- Minecraft 1.21.11+
- Paper, Folia, or a compatible fork
- Java 21

No resource pack is needed. The rack item is a player head with a custom skin, and the rack itself is
built from ordinary fences and buttons, so it looks right on a plain client.

## Download

Get the latest `Racks-<version>.jar` from [Modrinth](https://modrinth.com/plugin/racksplugin) or
[GitHub Releases](https://github.com/OpenVdra/RacksPlugin/releases).

## Install

1. Stop the server.
2. If the Racks data pack is installed in any world, remove it now. See
   [Coming from the Data Pack](/docs/migration) before you do.
3. Put the jar in the `plugins` folder.
4. Start the server.

On first start the plugin creates `plugins/Racks/` with `config.yml`, the language files and an empty
`racks.db`.
