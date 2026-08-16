<div align="center">

![Racks in every wood](https://raw.githubusercontent.com/OpenVdra/RacksPlugin/main/docs/public/media/showcase.jpeg)

A Paper/Folia plugin port of **[Racks](https://modrinth.com/datapack/racks)** that adds wooden display racks for tools and weapons. No resource pack required.

[![Modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/modrinth_vector.svg)](https://modrinth.com/plugin/racks)
[![Documentation](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/documentation/ghpages_vector.svg)](https://openvdra.github.io/RacksPlugin/)
[![GitHub](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/compact/available/github_vector.svg)](https://github.com/OpenVdra/RacksPlugin)

</div>

---

Craft a wooden rack, put it on the floor or hang it on a wall, and display your gear on it.
Right-click to swap an item in or out, sneak and right-click to change how the items are angled,
left-click to take the rack back down.

- **Minecraft:** 1.21.11+
- **Server:** Paper/Folia or compatible fork
- **Java:** 21

## Features

| | |
|---|---|
| **12 wood variants** | Oak, spruce, birch, jungle, acacia, dark oak, mangrove, cherry, pale oak, bamboo, crimson, warped |
| **Floor and wall racks** | Two slots on the floor, one on a wall, each with its own frame and click areas |
| **Per-player language** | Each player reads their own client language; fully translatable, see [Language](https://openvdra.github.io/RacksPlugin/docs/language) |
| **Custom item NBT data** | Enchantments, custom names, lore and other NBT data survive being placed on a rack |
| **Data pack migration** | Racks left standing by the original data pack can be imported on request |
| **WorldGuard / GriefPrevention** | Breaking a rack or swapping its item is checked against regions and claims, if either plugin is installed |

<table>
<tr>
<td width="50%"><img src="https://raw.githubusercontent.com/OpenVdra/RacksPlugin/main/docs/public/media/variants-overworld-empty.png" alt="Birch, oak, jungle, spruce and dark oak racks, empty"></td>
<td width="50%"><img src="https://raw.githubusercontent.com/OpenVdra/RacksPlugin/main/docs/public/media/variants-overworld.png" alt="The same racks holding tools"></td>
</tr>
<tr>
<td width="50%"><img src="https://raw.githubusercontent.com/OpenVdra/RacksPlugin/main/docs/public/media/variants-nether-empty.png" alt="Bamboo, acacia, mangrove, cherry, crimson and warped racks, empty"></td>
<td width="50%"><img src="https://raw.githubusercontent.com/OpenVdra/RacksPlugin/main/docs/public/media/variants-nether.png" alt="The same racks holding tools"></td>
</tr>
</table>

## Crafting

Three planks in a row, two sticks below the ends. Any of the twelve woods works.

<img src="https://raw.githubusercontent.com/OpenVdra/RacksPlugin/main/docs/public/media/recipe.gif" alt="Rack crafting recipe" width="420">

### What a rack will hold

Floor racks take axes, hoes, pickaxes, shovels, spears, swords, maces, carrots on a stick, warped
fungus on a stick and fishing rods. Wall racks take all of those plus bows, crossbows, tridents,
shields, shears and spyglasses. Holding anything else and right-clicking does nothing at all.

This goes by the item's actual type, not its name or texture, so a reskinned axe, sword, bow and so on
from ItemsAdder, Oraxen, Nexo or similar plugins goes on a rack the same as a vanilla one.

## Commands

| Command | Permission | |
|---|---|---|
| `/racks give <variant> [player] [count]` | `racks.command.give` | Hand out a rack |
| `/racks reload` | `racks.command.reload` | Re-read `config.yml` and the language files |

`/rack` is an alias.

### Permissions

| Node | Default | |
|---|---|---|
| `racks.use` | `true` | Place racks, swap items, change pose, break them |
| `racks.command.give` | `op` | |
| `racks.command.reload` | `op` | |

`racks.use` defaults to true because the data pack gated nothing. Out of the box the plugin behaves
identically. Negate it for a group to restrict racks.

## Language

Every message and item name ships in English and Vietnamese, and each player automatically sees
whichever one matches their own client language. Operators can add more: drop a
`language/<locale>/messages.yml` file next to the bundled ones and translate it, then
`/racks reload` picks it up.

## Protection plugins

Breaking a rack, swapping its item or turning it is checked against **WorldGuard** regions and
**GriefPrevention** claims. Nothing needs configuring beyond having the protection plugin itself
running. Putting a rack down is a normal `BlockPlaceEvent`, so both plugins already cover it.

| Rack action | WorldGuard flag | GriefPrevention trust |
|---|---|---|
| Break | `block-break` | Build (`/trust`) |
| Swap an item, or turn the rack | `interact` | Container (`/containertrust`) |

Operators and `worldguard.region.bypass` skip WorldGuard's checks. A GriefPrevention claim owner,
anyone trusted, admin claims and a player's `/ignoreclaims` toggle are all respected already, with no
extra permission to grant. See
[Protection Plugins](https://openvdra.github.io/RacksPlugin/docs/protections) for the full breakdown.

## Migrating from the data pack

> [!WARNING]
> **Remove the data pack before starting the server with this plugin.** If both are installed they
> will fight over the same entities.

Set `adopt-datapack-racks: true` in `config.yml` (it is `false` by default) and the plugin imports
racks the data pack left standing. It reads each one off the entities that make it up: the wood from
the block its frame displays, the direction from the yaw those displays were turned to, the contents
from what the item displays are holding, and even the pose, by matching the display's transform
against the same tables the data pack applied. The old entities are then removed and a plugin-owned
rack is built in their place, holding the same items in the same pose.

This happens per chunk, as chunks load, so a large world migrates gradually rather than all at once.

Two things cannot be recovered, because they lived only in the data pack's command storage, which has
no API:

- **The recorded owner** is lost. The data pack stored it but never used it, and neither does this
  plugin.
- **The creation time** becomes 0, meaning "old enough to drop", the same way the data pack treated
  racks placed before its own 3.0.0.

Rack items already in players' inventories keep working with no migration at all; they are recognised
by the `custom_model_data` string that has been on every version of the item. They are quietly
re-stamped into the plugin's own form the next time their owner logs in, which is what gives them a
name in the player's language instead of the data pack's fixed English.

Coming from Racks V.2? Run the V.3 data pack's `from_v2` upgrade first. The plugin can only read V.3
racks.

## Credits

The original **[Racks](https://modrinth.com/datapack/racks)** data pack is by **KawaMood**:
[Modrinth](https://modrinth.com/user/KawaMood) ·
[YouTube](https://www.youtube.com/@KawaMood) ·
[Discord](https://discord.com/invite/w8s9XWgN6v) ·
[kawamood.com](https://www.kawamood.com)

The rack designs, item textures, display transformations, poses, and the source showcase screenshot
are KawaMood's work, used under the terms of the original license. Its plaque is relabelled here to
identify this Paper and Folia plugin.

Plugin port by **Nighter**.
