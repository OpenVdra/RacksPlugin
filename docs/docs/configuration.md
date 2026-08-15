---
title: Configuration
---

# Configuration

Everything lives in `plugins/Racks/config.yml`. Player-facing text is not here, it is in
[the language files](/docs/language).

Changes apply on `/racks reload`, except where noted.

## Settings

<ConfigProperty name="language" value="en_US">

The language used for players whose own language has no file, and for anything with no single
reader, such as a rack lying on the ground.

Must match a folder name under `plugins/Racks/language/`. `en_US` and `vi_VN` ship with the plugin.

</ConfigProperty>

<ConfigProperty name="language-auto-detect" value="true" type="boolean">

Show each player messages in their own game language when a folder for it exists.

Off puts everyone on the `language` setting above, which is how the original data pack behaved.
It also makes every rack of the same wood stack, since they are then all named identically.

</ConfigProperty>

<ConfigGroup name="database">
<ConfigProperty name="file" value="racks.db">

The database file inside `plugins/Racks/`. It holds every placed rack and whatever is on it.

**Needs a full restart.** Back this file up along with your world. Losing it means every rack in the
world stops working, and the items on them are gone with it.

</ConfigProperty>
</ConfigGroup>

<ConfigGroup name="settings">
<ConfigProperty name="ignore-wall-rack-support" value="false" type="boolean">

Whether a wall rack survives losing the block it hangs on.

`false` is the default and matches the data pack: the rack drops itself and its items, the way a
painting does. `true` leaves it floating, and stops the check running at all.

`/racks setting ignore-wall-rack-support <true|false>` changes this in game and writes it back here.

</ConfigProperty>
<ConfigProperty name="wall-support-check-interval" value="10" type="number">

How often wall racks look at the block behind them, in ticks. 20 ticks is one second.

Raising it means less work for the server and a longer pause before a rack falls. It has no effect
when `ignore-wall-rack-support` is on.

</ConfigProperty>
<ConfigProperty name="lootable-delay" value="0" type="number">

How long a rack must stand, in ticks, before breaking it drops the rack item. `0` always drops it.

Raise it if a land protection plugin cancels placements a moment after they happen, which otherwise
lets a player place a rack and break it for a free copy. `40` is two seconds and is plenty.

Items sitting on the rack always drop, whatever this is set to.

</ConfigProperty>
</ConfigGroup>

<ConfigProperty name="adopt-datapack-racks" value="true" type="boolean">

Import racks left standing in the world by the Racks data pack, as their chunks load.

Remove the data pack before turning the server on, or the two fight over the same racks. See
[Coming from the Data Pack](/docs/migration).

</ConfigProperty>
<ConfigProperty name="recipes-enabled" value="true" type="boolean">

Register the twelve crafting recipes.

Off means players cannot craft racks at all, and they come only from `/racks give` or loot tables you
add yourself.

</ConfigProperty>

## The whole file

```yaml
language: en_US
language-auto-detect: true

database:
  file: racks.db

settings:
  ignore-wall-rack-support: false
  wall-support-check-interval: 10
  lootable-delay: 0

adopt-datapack-racks: true
recipes-enabled: true
```
