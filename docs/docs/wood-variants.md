---
title: Wood Variants
---

# Wood Variants

Racks come in twelve woods. The recipe is the same for all of them, the planks decide which one you
get.

<Gallery :items="[
  { src: '/media/variants-overworld-empty.png', caption: 'Birch, oak, jungle, spruce, dark oak' },
  { src: '/media/variants-overworld.png', caption: 'The same five, holding tools' },
  { src: '/media/variants-nether-empty.png', caption: 'Bamboo, acacia, mangrove, cherry, crimson, warped' },
  { src: '/media/variants-nether.png', caption: 'The same six, holding tools' }
]" />

## The full list

Use these names with [`/racks give`](/docs/commands):

`acacia` &nbsp; `bamboo` &nbsp; `birch` &nbsp; `cherry` &nbsp; `crimson` &nbsp; `dark_oak`
&nbsp; `jungle` &nbsp; `mangrove` &nbsp; `oak` &nbsp; `pale_oak` &nbsp; `spruce` &nbsp; `warped`

Pale oak was added after the screenshots above were taken.

## Racks of different woods do not stack

That is normal. Each wood is its own item.

On a server where players read different languages, two racks of the *same* wood can also fail to
stack, because each one is named in the language of whoever received it. Set `language-auto-detect`
to `false` in `config.yml` if you would rather every rack always stacked. See
[Language](/docs/language).

## Heads show the wrong texture

The rack item is a player head, and the skin behind it is fetched from Mojang the first time a client
sees one. A player who was offline at that moment sees a plain head until their game cache updates.

Reconnecting once while online is usually enough. If a head is stuck, close the game, delete the most
recent folders under `.minecraft/assets/skins/`, and start it again.
