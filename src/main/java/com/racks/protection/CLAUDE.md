# protection/

Optional integration with WorldGuard and GriefPrevention. Neither is a hard dependency — see
`softdepend` in `plugin.yml`/`paper-plugin.yml` — and nothing outside this package holds a reference
to either plugin's API; `RackInteractListener` only ever sees `ProtectionHooks`.

## Files

| File | Responsibility |
|---|---|
| `ProtectionHooks` | Public aggregator: detects which hooks apply, ANDs their answers |
| `ProtectionHook` | Package-private contract: `canBreak`/`canInteract` for one protection plugin |
| `WorldGuardHook` | `Flags.BLOCK_BREAK`/`Flags.INTERACT` via `RegionQuery.testBuild` |
| `GriefPreventionHook` | `ClaimPermission.Build`/`Inventory` via `Claim.checkPermission` |

## Why only break and interact — not place

Placing a rack is a real `BlockPlaceEvent` (the head goes down before `RackPlaceListener` converts it
a tick later, see that class's javadoc), so WorldGuard and GriefPrevention already protect it through
their own ordinary block-place listeners, well before Racks' own `MONITOR`-priority handler ever runs.
There is nothing to add there.

Breaking and interacting are different: once placed, a rack has no block at all — its hitboxes are
`Interaction` entities, its visuals are `BlockDisplay`/`ItemDisplay` entities (see `render/CLAUDE.md`).
`PrePlayerAttackEntityEvent`, `EntityDamageByEntityEvent` and `PlayerInteractEntityEvent` are outside
what a protection plugin's ordinary block-break/block-place flags cover, so `RackInteractListener`
asks `ProtectionHooks` explicitly, right after the `racks.use` permission check and before either
`RackService` call.

## The safe-class-loading pattern (load-bearing)

`WorldGuardHook` and `GriefPreventionHook` import types from their respective plugin's API at the top
of the file. That is fine at compile time (both are `compileOnly` dependencies, see `build.gradle.kts`)
but would throw `NoClassDefFoundError` at runtime on a server that has neither plugin installed — a
class is only verified, and only then does a missing referenced type become an error, the first time
something actually constructs it.

So neither class is ever constructed except from inside `ProtectionHooks.detect`, gated by
`PluginManager.isPluginEnabled("WorldGuard")` / `"GriefPrevention"` immediately beforehand. If a plugin
is absent, its hook class is never instantiated, and therefore never loaded, and therefore never
touches a class that does not exist on that server. Do not construct either hook anywhere else, and do
not add a field of either hook's type to anything outside this package — that would force the class to
resolve at class-load time of whatever holds the field, defeating the whole guard.

## `protection.worldguard` / `protection.griefprevention`

`ProtectionHooks` itself has no on/off switch — which plugins are installed cannot change without a
restart, so detection happens once, at enable. The live, per-plugin toggle is
`PluginConfig.isWorldGuardIntegrationEnabled()`/`isGriefPreventionIntegrationEnabled()` instead, read
fresh by `RackInteractListener.isHookEnabled` on every break and interaction and passed into
`ProtectionHooks.canBreak`/`canInteract` as a `Predicate<String>` matched against each hook's
`name()`. That indirection is what lets one plugin's checks be switched off without touching the
other's, and without `/racks reload` needing to re-register a listener or re-detect anything.

## GriefPrevention API note

`Claim.allowBreak`/`Claim.allowContainers` (the per-action methods) are deprecated in the version this
plugin compiles against; `Claim.checkPermission(Player, ClaimPermission, Event)` is the replacement,
returning a `Supplier<String>` (denial reason) or `null` (allowed). There is no real Bukkit event
backing a rack interaction, so `null` is passed where the API accepts one — the parameter only exists
so other plugins can inspect the triggering event through `ClaimPermissionCheckEvent`, and nothing here
needs that. `checkPermission` already resolves the claim owner, trusted players, admin claims and a
player's own `/ignoreclaims` toggle internally, unlike WorldGuard's `RegionQuery`, which the docs
explicitly say does *not* check bypass permissions on its own — hence `WorldGuardHook` checking
`worldguard.region.bypass` itself before ever building a query.
