# Changelog

All notable changes to Racks are recorded here, newest first.

## 1.0.0 - 2026-08-15

First release. A Paper and Folia port of KawaMood's Racks data pack, version 3.2.3.

### Added

- Floor and wall racks in twelve woods, crafted from three planks and two sticks. Floor racks hold two tools, wall racks hold one.
- Right-click a rack to put a tool on it or take one off. Sneak and right-click to change the pose, six on the floor and four on a wall. Left-click to break it.
- Placed racks and the items on them are saved in `plugins/Racks/racks.db`, and survive a Minecraft version upgrade.
- WorldGuard regions and GriefPrevention claims are checked before a player breaks a rack or swaps its item. `worldguard` and `griefprevention` in `config.yml` turn each check off.
- Racks left standing by the Racks data pack can be imported as their chunks load, keeping their wood, facing, items and pose. Turned on with `adopt-datapack-racks` in `config.yml`.
- Rack items already in player inventories keep working, and take their new name on the next login.
- English and Vietnamese messages, with each player reading their own game language. Other languages can be added as a folder under `language/`.
- `/racks give` hands out a rack of any wood, `/racks reload` re-reads the config and language files, and `/racks setting ignore-wall-rack-support` reads or changes the wall support rule.
- A rack whose frame is missing when its chunk loads is rebuilt, so a restored backup or a stray `/kill` no longer leaves a rack nobody can see or click.
- Installing a newer version brings an existing `racks.db` up to date on its own, with no manual step.

### Changed

Behaviour is the data pack's throughout, apart from one point:

- Breaking the barrier under a floor rack in creative mode now breaks the rack. The data pack left the rack standing with nothing solid under it.

### Notes

- Remove the Racks data pack before starting the server with this plugin. Running both means the two fight over the same racks.
- Coming from Racks V.2, run the V.3 data pack's `from_v2` upgrade first. This plugin can only read V.3 racks.
- The owner recorded against each rack is not carried over from the data pack. Neither the data pack nor the plugin used it for anything.
