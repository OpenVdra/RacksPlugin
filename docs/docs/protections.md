---
title: Protection Plugins
---

# Protection Plugins

Breaking a rack, swapping its item or turning it is checked against WorldGuard regions and
GriefPrevention claims, whichever of the two — or both — is installed. There is nothing to install
beyond the protection plugin itself; Racks detects it automatically on startup.

**Placing** a rack needs no integration at all: it is a normal `BlockPlaceEvent`, the same event a
chest or a torch raises, so both plugins already protect it on their own, with no extra code from
Racks.

## Quick reference

| Rack action | WorldGuard flag | GriefPrevention trust |
|---|---|---|
| Break | `block-break` | Build |
| Swap an item, or turn the rack (sneak + right-click) | `interact` | Inventory (`/containertrust`) |
| Place | *(a normal block placement — always protected on its own)* | *(a normal block placement — always protected on its own)* |

Outside any region or claim, every action is allowed.

## Turning it off

```yaml
protection:
  worldguard: true
  griefprevention: true
```

Each is independent: set `worldguard: false` to skip only WorldGuard's checks, or `griefprevention:
false` to skip only GriefPrevention's, without touching the other. Either has no effect if that
plugin is not installed, and neither affects placement, which is protected regardless.

## WorldGuard

**Download:** [Modrinth](https://modrinth.com/plugin/worldguard)

The check uses WorldGuard's build test, so **region members are always allowed**. For non-members —
for example a public or `__global__` region — enable the flags explicitly:

```bash
/rg flag <region> block-break allow
/rg flag <region> interact allow
```

Setting a flag to `deny` blocks it even for members:

```bash
/rg flag <region> interact deny
```

### Bypass

Operators and players with `worldguard.region.bypass` (or the per-world
`worldguard.region.bypass.<world>` form) skip both checks. Test with a regular player account.

## GriefPrevention

**Download:** [Modrinth](https://modrinth.com/plugin/griefprevention)

Breaking a rack asks for **Build trust**, the same as breaking a block. Swapping its item or turning
it asks for the lighter **Inventory (Container) trust**, the same a chest asks for — since neither
action destroys the rack.

```bash
/trust <player>          # Build trust: covers breaking, swapping and turning
/containertrust <player> # Inventory trust: covers swapping and turning, not breaking
```

`/accesstrust` (buttons and doors only) is **not enough** for either action.

### Bypass

A claim's owner, anyone explicitly trusted, admin claims and a player's own `/ignoreclaims` toggle
are all respected automatically — there is no separate permission node to grant on top of that.
