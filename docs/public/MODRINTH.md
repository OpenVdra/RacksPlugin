![Banner](https://raw.githubusercontent.com/OpenVdra/RacksPlugin/main/docs/public/banner.png)

<div align="center">

[![Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/modrinth_vector.svg)](https://modrinth.com/plugin/racks)
[![Documentation](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/documentation/ghpages_vector.svg)](https://openvdra.github.io/RacksPlugin/)
[![GitHub](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/github_vector.svg)](https://github.com/OpenVdra/RacksPlugin)

</div>

Tools and weapons deserve better than a chest. **Racks** puts them on display: a wooden rack on the floor or on the wall, holding a pickaxe, a sword or a trident where everyone can see it.

This is a Paper and Folia port of **[KawaMood's Racks data pack](https://modrinth.com/datapack/racks)**. Everything a player sees is the data pack's, down to the recipe and the poses. What changed is underneath, so racks now survive a Minecraft version upgrade with their items intact.

## Highlights

- **Twelve woods, two shapes**: oak through warped, on the floor or on a wall. Floor racks hold two tools, wall racks hold one.

- **Six floor poses, four wall poses**: sneak and right-click to angle the gear until it looks right.

- **Items are safe across updates**: every rack and what is on it is saved to the plugin's own database, not to command storage.

- [**WorldGuard and GriefPrevention**](https://openvdra.github.io/RacksPlugin/docs/protections): regions and claims are checked before a rack is broken or an item swapped. No setup, both can be switched off separately.

- [**Data pack migration**](https://openvdra.github.io/RacksPlugin/docs/migration): racks already standing in the world can be imported as their chunks load, keeping their wood, facing, items and pose.

- **Folia ready**: built for it, not bolted on afterwards.

- **English and Vietnamese included**: each player reads their own game language, and any other language can be added as a folder.

- **No resource pack**: the rack item is a player head and the rack is built from ordinary fences and buttons, so it looks right on a plain client.

## Screenshots

<p align="center">
  <img src="https://raw.githubusercontent.com/OpenVdra/RacksPlugin/main/docs/public/media/showcase.jpeg" alt="Racks in every wood, holding tools" />
</p>
<p align="center">
  <img src="https://raw.githubusercontent.com/OpenVdra/RacksPlugin/main/docs/public/media/variants-overworld.png" alt="Birch, oak, jungle, spruce and dark oak racks holding tools" />
</p>
<p align="center">
  <img src="https://raw.githubusercontent.com/OpenVdra/RacksPlugin/main/docs/public/media/variants-nether.png" alt="Bamboo, acacia, mangrove, cherry, crimson and warped racks holding tools" />
</p>

## Crafting

Three planks in a row, two sticks below the ends. Any of the twelve woods works.

<p align="center">
  <img src="https://raw.githubusercontent.com/OpenVdra/RacksPlugin/main/docs/public/media/recipe.gif" alt="Rack crafting recipe" width="420" />
</p>

## Using a rack

| Action | Does |
|---|---|
| Right-click | Put the held item on the rack, or take one off |
| Sneak and right-click | Change the pose |
| Left-click | Break the rack, dropping it and everything on it |

Floor racks take axes, hoes, pickaxes, shovels, spears, swords, maces, carrots on a stick, warped fungus on a stick and fishing rods. Wall racks take all of those plus bows, crossbows, tridents, shields, shears and spyglasses.

## Commands

| Command | Permission | Does |
|---|---|---|
| `/racks give <variant> [player] [count]` | `racks.command.give` | Hand out a rack of any wood |
| `/racks reload` | `racks.command.reload` | Re-read the config and language files |
| `/racks setting ignore-wall-rack-support [true\|false]` | `racks.command.setting` | Read or change the wall support rule |

`/rack` is an alias. Using racks is gated by `racks.use`, which is on for everyone by default, matching the data pack.

## Get started

**Minecraft** 1.21.11 · **Server** Paper / Folia / Purpur · **Java** 21+

Drop the `.jar` into `plugins/` and restart. Nothing to configure.

Coming from the data pack? **Remove it first**, then follow the [migration guide](https://openvdra.github.io/RacksPlugin/docs/migration).

Racks is **free and open source**. Fork it, build on it, or contribute back.
