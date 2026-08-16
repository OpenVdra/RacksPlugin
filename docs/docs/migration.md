---
title: Coming from the Data Pack
---

# Coming from the Data Pack

Racks already standing in your world can be imported automatically, with the items still on them —
turn on `adopt-datapack-racks` in `config.yml` (off by default).

::: warning Remove the data pack first
Do not run the data pack and the plugin at the same time. Both will try to control the same racks.
:::

## Steps

1. Stop the server.
2. Back up your world. This is the one step worth not skipping.
3. Delete the Racks data pack from every world's `datapacks` folder. Do **not** run the data pack's
   uninstall function, that removes the racks themselves.
4. Put the plugin jar in `plugins`.
5. In `plugins/Racks/config.yml`, set `adopt-datapack-racks: true`.
6. Start the server.

That is all. Racks are imported as their chunks load, so a large world moves over gradually as
players travel rather than all at once on startup.

Leave `adopt-datapack-racks: false` (the default) if you would rather leave old racks alone and not
bring them into the plugin.

## What carries over

<CardGrid>
<FeatureCard icon="Check" title="Kept">

The wood, which way the rack faces, whether it is on the floor or a wall, the items on it and the
pose they are arranged in.

</FeatureCard>
<FeatureCard icon="TriangleAlert" title="Not kept">

Who originally placed the rack. The data pack recorded it but never used it, and neither does the
plugin.

</FeatureCard>
</CardGrid>

Imported racks count as old enough to drop when broken, whatever `lootable-delay` is set to. The
data pack treated its own older racks the same way.

## Rack items in players' inventories

Nothing to do. Racks in chests, shulkers and inventories keep working straight away.

They are quietly refreshed into the plugin's own form the next time their owner logs in, which is
what gives them a name in that player's language instead of fixed English.

## If a rack is missing after the move

Walk into its chunk and back out, then in again. Racks are rebuilt from the database whenever their
chunk loads and something is missing, so the round trip usually fixes it.

## Coming from Racks V.2

Update to the V.3 data pack and run its `from_v2` upgrade **before** installing the plugin. The
plugin can only read V.3 racks.
