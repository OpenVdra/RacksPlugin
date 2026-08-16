---
title: Getting Started
---

# Getting Started

Racks adds wooden display racks for tools and weapons. Craft one, put it on the floor or on a wall,
and right-click it with something in hand.

It is a plugin port of [KawaMood's Racks data pack](https://modrinth.com/datapack/racks). Everything a
player sees is the same. What is different is underneath: racks live in a database, so they survive a
world upgrade and never depend on command storage.

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

## Your first rack

<CardGrid>
<FeatureCard icon="Hammer" title="1. Craft it">

Three planks in a row with two sticks below the ends. Any of the twelve woods works.

</FeatureCard>
<FeatureCard icon="Blocks" title="2. Place it">

On the ground for two tool slots, or against a wall for one. A floor rack turns to face you.

</FeatureCard>
<FeatureCard icon="Hand" title="3. Fill it">

Right-click with a tool in hand to put it up. Right-click with an empty hand to take it back.

</FeatureCard>
</CardGrid>

## Where to next

<CardGrid>
<DocCard icon="Axe" title="Using Racks" link="/docs/using-racks" desc="Crafting, placing, poses, and what a rack will and will not hold." />
<DocCard icon="TreePine" title="Wood Variants" link="/docs/wood-variants" desc="All twelve woods and what they look like." />
<DocCard icon="Settings" title="Configuration" link="/docs/configuration" desc="Every setting in config.yml and what changing it does." />
<DocCard icon="ArrowRightLeft" title="Coming from the Data Pack" link="/docs/migration" desc="Keep the racks already standing in your world." />
</CardGrid>
