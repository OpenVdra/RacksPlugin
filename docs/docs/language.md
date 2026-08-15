---
title: Language
---

# Language

English and Vietnamese ship with the plugin. Each player reads messages in their own game language
when a file for it exists.

## Where the text lives

```
plugins/Racks/language/
  en_US/messages.yml
  vi_VN/messages.yml
```

Both files are written on first start. Edit either one and run `/racks reload`.

## Which language a player sees

The plugin looks for the player's own game language first. If there is no folder for it, it falls
back to the `language` setting in `config.yml`, and then to `en_US`, which is always present.

Only languages with a folder under `language/` can be shown. Adding one is all it takes.

Set `language-auto-detect` to `false` in `config.yml` to put everyone on the `language` setting
regardless of their game language.

## Adding a language

1. Copy `language/en_US/` to `language/<locale>/`, for example `language/de_DE/`.
2. Translate the values. Leave the keys and the `{placeholders}` alone.
3. Run `/racks reload`.

No plugin update is needed, and the folder is picked up on every reload from then on.

## Writing the text

Colours work two ways. Modern tags:

```yaml
reloaded: '{prefix}<gray>Configuration and language files reloaded.'
```

Or the older `&` codes, including `&#RRGGBB` for hex:

```yaml
reloaded: '{prefix}&7Configuration and language files reloaded.'
```

`{prefix}` inserts the `prefix` value from the top of the same file. The other `{placeholders}` are
filled in by the plugin, so keep every one that appears in the English line.

## Item names

Racks are named in the language of whoever received them, because an item carries its own name
rather than being re-read for each viewer.

On a server where players read different languages, that means two racks of the same wood can fail
to stack. Set `language-auto-detect` to `false` if you would rather they always stacked.
