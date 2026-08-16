---
title: Protection Plugins
---

# Protection Plugins

Breaking a rack, swapping its item or turning it is checked against WorldGuard regions and
GriefPrevention claims. Nothing needs installing beyond the protection plugin itself, Racks finds it
on startup.

Putting a rack down is a normal block placement, so both plugins already cover it on their own.

## What each action asks for

| Rack action | WorldGuard flag | GriefPrevention trust |
|---|---|---|
| Break | `block-break` | Build |
| Swap an item, or turn the rack | `interact` | Container |

Outside any region or claim, every action is allowed.

## Turning a check off

```yaml
protection:
  worldguard: true
  griefprevention: true
```

The two are independent. `worldguard: false` skips only WorldGuard's checks, `griefprevention:
false` skips only GriefPrevention's. Either is ignored when that plugin is not installed, and
neither affects putting a rack down.

## WorldGuard

**Download:** [Modrinth](https://modrinth.com/plugin/worldguard)

The check uses WorldGuard's build test, so **region members are always allowed**. For non-members,
for example in a public region or in `__global__`, the flags have to be set explicitly:

```bash
/rg flag <region> block-break allow
/rg flag <region> interact allow
```

Setting a flag to `deny` blocks it even for members:

```bash
/rg flag <region> interact deny
```

### Bypass

Operators and anyone with `worldguard.region.bypass`, or the per-world
`worldguard.region.bypass.<world>` form, skip both checks. Test with a normal player account.

## GriefPrevention

**Download:** [Modrinth](https://modrinth.com/plugin/griefprevention)

Breaking a rack asks for **Build trust**, the same as breaking a block. Swapping an item or turning
a rack asks for the lighter **Container trust**, the same a chest asks for, since neither destroys
the rack.

```bash
/trust <player>
/containertrust <player>
```

`/trust` covers breaking, swapping and turning. `/containertrust` covers swapping and turning only.
`/accesstrust`, which is for buttons and doors, is not enough for either action.

### Bypass

The claim owner, anyone trusted, admin claims and a player's own `/ignoreclaims` toggle are all
respected already. There is no extra permission node to grant.
