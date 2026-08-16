---
title: Permissions
---

# Permissions

## Using racks

**`racks.use`** &nbsp; default: **everyone**
Place racks, put items on them, change their pose and break them.

This is on for everyone by default because the original data pack gated nothing. A fresh install
behaves exactly like the data pack did.

To stop a group from using racks at all, negate the node for that group in your permissions plugin.
A player without it can still craft the item, but placing it puts the rack straight back in their
inventory and clicking an existing rack does nothing.

::: tip Protecting builds
`racks.use` is all or nothing. To stop players breaking each other's racks in specific areas, use
WorldGuard or GriefPrevention. Racks checks both before a rack is broken or an item swapped, see
[Protection Plugins](/docs/protections).
:::

## Commands

Each command has its own node. There is no shared parent node, so granting one does not grant the
others. All three default to operators.

**`racks.command.give`** - `/racks give`: hand out a rack of any wood.

**`racks.command.reload`** - `/racks reload`: re-read the config and language files.

**`racks.command.setting`** - `/racks setting`: read or change the wall support rule.

A player who holds none of the three does not see `/racks` in their command list at all.
