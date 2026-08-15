# Changelog

All notable changes to Racks are recorded here, newest first.

## 1.0.0 - 2026-08-15

First release. A Paper and Folia port of KawaMood's Racks data pack, version 3.2.3.

### Added

- Floor and wall racks in twelve woods, crafted from three planks and two sticks. Floor racks hold two tools, wall racks hold one.
- Right-click a rack to put a tool on it or take one off. Sneak and right-click to change the pose, six on the floor and four on a wall. Left-click to break it.
- Placed racks and their contents are stored in `plugins/Racks/racks.db`. Racks and the items on them survive a Minecraft version upgrade.
- Racks left standing by the Racks data pack are imported automatically as their chunks load, keeping their wood, facing, items and pose. Turn it off with `adopt-datapack-racks` in `config.yml`.
- Rack items already in players' inventories keep working, and are refreshed into the plugin's form on their next login.
- English and Vietnamese language files, with each player reading messages in their own game language. Add a folder under `language/` for any other language.
- `/racks give` hands out a rack of any wood, `/racks reload` re-reads the config and language files, and `/racks setting ignore-wall-rack-support` reads or changes the wall support rule.
- `lootable-delay` in `config.yml` sets how long a rack must stand before breaking it returns the rack item, for servers where a land protection plugin cancels placements a moment late.
- Racks whose display is missing when a chunk loads are rebuilt from the database, so a restored backup or a stray `/kill` no longer leaves a rack nobody can see or click.

### Changed

Behaviour is the data pack's throughout, apart from one point:

- Breaking the barrier under a floor rack in creative mode now breaks the rack. The data pack never watched that block, so the rack's entities and its database row stayed behind and the rack went on working with nothing solid under it.

### Notes

- Remove the Racks data pack before starting the server with this plugin. Running both means the two fight over the same racks.
- Coming from Racks V.2, run the V.3 data pack's `from_v2` upgrade first. The plugin can only read V.3 racks.
- The owner recorded against each rack is not carried over from the data pack. Neither the data pack nor the plugin used it for anything.
