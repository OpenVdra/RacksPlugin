---
title: Commands
---

# Commands

Every command starts with `/racks`. `/rack` does the same thing.

Players never need a command. Racks are crafted, placed and used entirely in the world.

<BaseTable :columns="['Command', 'What it does']" grid="1fr 1fr">

<CommandRow commands="/racks give &lt;wood&gt; [player] [count]" permission="racks.command.give">

Gives a rack. With no player, gives it to yourself. The wood is one of the twelve
[wood names](/docs/wood-variants#the-full-list), and the count is 1 to 64.

Anything that does not fit in the target's inventory drops at their feet.

</CommandRow>

<CommandRow commands="/racks reload" permission="racks.command.reload">

Re-reads `config.yml` and the language files. Racks already in the world are untouched.

Use this after editing any file in `plugins/Racks/`.

</CommandRow>

<CommandRow commands="/racks setting ignore-wall-rack-support" permission="racks.command.setting">

Shows whether wall racks currently break when their support block is removed.

</CommandRow>

<CommandRow commands="/racks setting ignore-wall-rack-support &lt;true|false&gt;" permission="racks.command.setting">

Turns that behaviour off or on. The new value is written to `config.yml`, so it survives a restart.

`true` means wall racks stay put with no support. `false` is the default and matches the data pack.

</CommandRow>

</BaseTable>

## Examples

Give yourself one oak rack:

```
/racks give oak
```

Give another player sixteen cherry racks:

```
/racks give cherry Steve 16
```

Stop wall racks from falling when their wall is mined:

```
/racks setting ignore-wall-rack-support true
```
