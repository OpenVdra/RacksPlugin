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

Show each player messages in their own game language. Only languages with a folder under
`language/` can be shown, everyone else falls back to the `language` setting above.

Off puts everyone on one language, which is how the data pack behaved. It also makes every rack of
the same wood stack, since they are then all named identically.

</ConfigProperty>

<ConfigGroup name="database">
<ConfigProperty name="file" value="racks.db">

The database file inside `plugins/Racks/`. It holds every placed rack and whatever is on it.

**Needs a full restart.** Back this file up along with the world. Losing it means every rack in the
world stops working, and the items on them are gone with it.

</ConfigProperty>
</ConfigGroup>

<ConfigGroup name="settings">
<ConfigProperty name="ignore-wall-rack-support" value="false" type="boolean">

Whether a wall rack survives losing the block it hangs on.

`false` matches the data pack: the rack drops itself and its items, the way a painting does. `true`
leaves it floating, and stops the check below running at all.

</ConfigProperty>
<ConfigProperty name="wall-support-check-interval" value="10" type="number">

How often wall racks look at the block behind them, in ticks. 20 ticks is one second.

Higher means less work for the server and a longer wait before a rack falls. It has no effect when
`ignore-wall-rack-support` is on.

</ConfigProperty>
</ConfigGroup>

<ConfigGroup name="protection">
<ConfigProperty name="worldguard" value="true" type="boolean">

Check WorldGuard regions before a player breaks a rack or swaps its item. Breaking needs the
`block-break` flag, swapping and turning need `interact`.

Ignored when WorldGuard is not installed. Turning it off leaves GriefPrevention below untouched.
See [Protection Plugins](/docs/protections).

</ConfigProperty>
<ConfigProperty name="griefprevention" value="true" type="boolean">

Check GriefPrevention claims before a player breaks a rack or swaps its item. Breaking needs Build
trust, swapping and turning need Container trust.

Ignored when GriefPrevention is not installed. Turning it off leaves WorldGuard above untouched.
See [Protection Plugins](/docs/protections).

</ConfigProperty>
</ConfigGroup>

<ConfigProperty name="adopt-datapack-racks" value="false" type="boolean">

Import racks left standing in the world by the Racks data pack, as their chunks load.

Remove the data pack before turning the server on, or the two fight over the same racks. See
[Coming from the Data Pack](/docs/migration).

</ConfigProperty>
<ConfigProperty name="recipes-enabled" value="true" type="boolean">

Register the twelve crafting recipes.

Off means racks cannot be crafted at all, and come only from `/racks give` or loot tables added by
the server owner.

</ConfigProperty>
<ConfigProperty name="update-checker" value="true" type="boolean">

Look up the newest release once at startup, and tell operators in chat and in the console when a
newer one exists.

**Needs a full restart.** Nothing is ever downloaded or installed, only the version number is read.
Modrinth is asked first, and the GitHub releases page stands in when it cannot be reached. Off stops
the lookup entirely, which is the setting to use on a server with no outbound internet access.

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

protection:
  worldguard: true
  griefprevention: true

adopt-datapack-racks: false
recipes-enabled: true
update-checker: true
```
