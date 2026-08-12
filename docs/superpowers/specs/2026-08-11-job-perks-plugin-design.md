# ModularJobs Perks Plugin Design

> Status: approved architecture and content mapping
> Date: 2026-08-11
> Scope: separate `perks` Paper plugin, upgrade-provider API, job perk trees, and MapGUI integration

## 1. Goal

Add a separate Gradle submodule that builds a separate Paper plugin. The plugin contributes job perk trees for the current ModularJobs job roster, implements archived Mayhem Multiverse mechanics where they have a current job analogue, adds balanced upgrades for unmatched jobs, and renders every contribution through ModularJobs' MapGUI upgrade graph.

ModularJobs remains authoritative for player state, upgrade points, purchase requirements, persistence, effect synchronization, and MapGUI actions. The perks plugin provides immutable content definitions and capability implementations; it cannot mutate repositories or bypass purchase gates.

## 2. Fixed scope and source policy

The supported roster is the 19 storage job IDs loaded from `paper/src/main/resources/jobs.yml`:

- `builder`
- `lumberjack`
- `miner`
- `farmer`
- `hunter`
- `fisherman`
- `blacksmith`
- `enchanter`
- `alchemist`
- `herbalism`
- `smelting`
- `milling`
- `tanning`
- `refining`
- `cooking`
- `armorsmithing`
- `tailoring`
- `engineering`
- `artisan`

The plugin does not create the archive's 20-job roster. Historical names map to current storage IDs only when their work domain matches: Arborist to `lumberjack`, Chef to `cooking`, and Inventor to `engineering`. Runtime validation uses `JobService`, not `ProfessionCatalog`, because the latter is a separate 15-track canonical catalog and omits current jobs.

The content baseline is the latest coherent May 2015 wiki revision. Earlier 2014 and February 2015 revisions differ in names, thresholds, cooldowns, and mechanics; values from different revisions are not merged. Archived behavior that is unsafe, obsolete, or dependent on unavailable server hooks is adapted explicitly, not presented as an exact recreation.

Primary archive sources:

- [Jobs overview](https://web.archive.org/web/20150529235837id_/http://wiki.mayhem-multiverse.com/index.php/Jobs_(2.0))
- [Advanced Jobs](https://web.archive.org/web/20150530011120id_/http://wiki.mayhem-multiverse.com/index.php/Advanced_Jobs)
- [Alchemist](https://web.archive.org/web/20150530005546id_/http://wiki.mayhem-multiverse.com/index.php/Alchemist)
- [Arborist](https://web.archive.org/web/20150511191009id_/http://wiki.mayhem-multiverse.com:80/index.php/Arborist)
- [Artisan](https://web.archive.org/web/20150530001928id_/http://wiki.mayhem-multiverse.com/index.php/Artisan)
- [Blacksmith](https://web.archive.org/web/20150529235944id_/http://wiki.mayhem-multiverse.com/index.php/Blacksmith)
- [Builder](https://web.archive.org/web/20150517055105id_/http://wiki.mayhem-multiverse.com:80/index.php/Builder)
- [Chef](https://web.archive.org/web/20150517005924id_/http://wiki.mayhem-multiverse.com:80/index.php/Chef)
- [Enchanter](https://web.archive.org/web/20150516232412id_/http://wiki.mayhem-multiverse.com:80/index.php/Enchanter)
- [Farmer](https://web.archive.org/web/20150530000613id_/http://wiki.mayhem-multiverse.com/index.php/Farmer)
- [Fisherman](https://web.archive.org/web/20150529234420id_/http://wiki.mayhem-multiverse.com/index.php/Fisherman)
- [Hunter](https://web.archive.org/web/20150530005701id_/http://wiki.mayhem-multiverse.com/index.php/Hunter)
- [Inventor](https://web.archive.org/web/20150529235852id_/http://wiki.mayhem-multiverse.com/index.php/Inventor)
- [Miner](https://web.archive.org/web/20150516215802id_/http://wiki.mayhem-multiverse.com:80/index.php/Miner)

The full archive inventory also records Excavator, Explorer, Merchant, Monster Hunter, Rancher, Soldier, Treasure Hunter, and Demolitionist. Their mechanics are used only where they fit a current job branch without changing the current roster.

## 3. Existing constraints

Current upgrade registries are private objects created inside `PluginContext`. `UpgradeTreeLoader` populates them before `UpgradeServiceImpl`, listeners, and GUI wiring are constructed. `Bridge` exposes no upgrade-registration service. `SimpleRegistryImpl.register` silently replaces an object by key, and different callers resolve trees inconsistently by canonical key or first matching job key.

`UpgradeTree`, `SkillTree`, and `SkillNode` defensively copy their collections. A provider cannot append nodes to a loaded tree. The only safe change is building and publishing a new complete immutable tree snapshot.

`NodeEffect` and `UpgradeEffect` are sealed. The parser and effect consumers understand only built-in boosts, ruled boosts, permissions, recipe unlocks, and state writes. Provider mechanics therefore require one API-owned capability envelope and a central dispatcher; arbitrary provider implementations of `NodeEffect` remain forbidden.

Player state is persisted by player and job, with v2 node levels stored by stable local node ID. Current point calculation ignores unknown nodes, and reset drops unknown nodes. Missing or renamed provider nodes can therefore create apparent refunds, later overspending, or silent state loss unless unresolved state fails closed.

## 4. Module and dependency layout

Add `perks` to `settings.gradle.kts` as a separate Paper plugin submodule.

`perks` uses `compileOnly(project(":api"))`, Paper API as `compileOnly`, and the repository's existing MockBukkit test convention. It does not depend on `:paper`, shade ModularJobs API/common classes, or access implementation packages. Its shadow jar contains only the provider implementation and private dependencies.

Both plugins use `paper-plugin.yml`. The `perks` descriptor declares a required server dependency on `ModularJobs` with `load: BEFORE` and `join-classpath: true`, meaning ModularJobs loads before `perks` and supplies the one shared API class identity. The existing ModularJobs MapGUI descriptor remains required. `perks` never imports MapGUI.

Descriptor tests open both built jars and assert:

- `perks` requires `ModularJobs`;
- joined classpath is enabled;
- neither `net/aincraft/**` API classes nor MapGUI classes occur in the perks jar;
- ModularJobs still requires MapGUI;
- loading the two jars in Paper produces one `UpgradeExtensionService` class identity.

## 5. Registration architecture

### 5.1 Executable public contract

Add these Paper-free contracts under `api/src/main/java/net/aincraft/upgrade/extension/`:

```java
public interface UpgradeExtensionService {
    UpgradeRegistrationResult install(UpgradeContribution contribution);
}

public record UpgradeContribution(
        Key owner,
        String ownerPlugin,
        int schemaVersion,
        Map<String, TreeContribution> trees,
        Map<Key, CapabilityHandler> handlers
) {
    public UpgradeContribution {
        trees = Map.copyOf(trees);
        handlers = Map.copyOf(handlers);
    }
}

public record TreeContribution(
        SkillTree completeTree,
        Set<String> acceptedBaseFingerprints
) {
    public TreeContribution {
        acceptedBaseFingerprints = Set.copyOf(acceptedBaseFingerprints);
    }
}
```

`CapabilityHandler` is an API interface implemented directly by classes in the perks plugin:

```java
public interface CapabilityHandler {
    Key key();
    Set<Integer> supportedSchemaVersions();
    CapabilityValidation validate(Map<String, String> payload);
    Component describe(Map<String, String> payload);
    CapabilityResult activate(CapabilityContext context);
    CapabilityResult revoke(CapabilityContext context);
}
```

`CapabilityContext` contains only player UUID, job/tree/node IDs, level index, immutable payload, source-effect ID, reason, and snapshot generation. It exposes no repository, points, award, reset, or purchase operation. A perks handler resolves Bukkit objects from the UUID inside its implementation; Bukkit types never enter the API signature.

This object reference is the executable transport across the plugin boundary. Joined Paper classpaths guarantee that the provider implementation and core dispatcher share the same API interface identity. ModularJobs validates `ownerPlugin` against the enabled Bukkit plugin and verifies that every handler class is loaded from that plugin's classloader. A provider cannot register handlers on behalf of another plugin.

`UpgradeRegistrationResult` is a sealed result with `Installed(generation)`, `DuplicateOwner`, `UnsupportedSchema`, `InvalidPlugin`, `InvalidJob`, `InvalidGraph`, `BaseTreeMismatch`, `KeyCollision`, `MissingCapability`, `PlayersOnline`, and `LifecycleRejected` variants. The service exposes no raw registry, loader, repository, implementation service, or GUI.

### 5.2 Lifecycle

Version one supports one post-enable installation per provider:

1. ModularJobs enables, constructs its built-in snapshot, and registers `UpgradeExtensionService` through Bukkit's `ServicesManager`.
2. Paper enables `perks` afterward because of the required dependency.
3. During `perks.onEnable`, the provider constructs all immutable trees and handler objects, then calls `install` once on the server thread.
4. Installation rejects if any player is online. This keeps version one to normal server startup and excludes `/reload` or late dynamic enable.
5. ModularJobs validates the complete batch and active base-tree fingerprints without scanning player rows.
6. Under the snapshot write lock, ModularJobs builds the complete replacement snapshot and publishes it through one `AtomicReference.set`.
7. The returned generation is stored by the provider; `onEnable` returns normally. On failure, `perks` logs the typed diagnostic and calls `PluginManager.disablePlugin(this)`.

Second installation, asynchronous installation, node patches, plugin reload, and hot replacement are unsupported and rejected. Server operators restart both plugins after changing either jar.

Live provider disable is also unsupported. `PluginDisableEvent` marks the owner unavailable and blocks new purchase/reset operations for its affected jobs. Core does not invoke disabled-plugin handlers. Existing in-memory capability effects may remain until the required full server restart; the design does not claim automatic live revocation.

### 5.3 Full-replacement semantics

Each `TreeContribution` is a complete replacement tree, never a patch. ModularJobs produces exactly one active v2 `SkillTree` per current job. Provider ownership remains snapshot metadata; the active key remains `modularjobs:upgrade_tree/<job>` because existing level listeners address that key directly.

For jobs with no active v2 tree, `acceptedBaseFingerprints` contains the sentinel `absent`. For Miner, the provider tree includes every shipped core node with the same stable ID, kind, level costs, requirements, effects, and state writes, then adds provider nodes around them. The build generates accepted fingerprints for each shipped core Miner variant. Installation rejects an operator-modified or unknown Miner tree rather than losing its nodes or state.

The provider replacement must contain every node from the accepted base with an identical semantic fingerprint. A semantic fingerprint covers ID, kind, levels, costs, requirements, effects, prerequisites, exclusions, and state writes; names, descriptions, icons, and positions are excluded so layout/text may evolve. Extra active-base IDs or changed semantic fingerprints produce `BaseTreeMismatch`. No `.findFirst()` merge, silent `SimpleRegistryImpl.put`, or implicit operator-tree overwrite remains.

Fresh-install correctness is independent of `UpgradeTreeLoader` copying bundled resources: a valid provider replacement supplies the complete tree. When `perks` is absent, ModularJobs retains its normal built-in/fallback behavior.

### 5.4 Canonical snapshot and generation

`UpgradeSnapshotStore` owns an `AtomicReference<UpgradeSnapshot>` and a server-thread write lock. A snapshot contains generation, one tree per job, owner availability, handler bindings, disabled-node reasons, base and content fingerprints, and contribution metadata. Reads capture one immutable snapshot reference for their whole operation. Generation increments only after a fully validated install.

`UpgradeServiceImpl`, level and login listeners, boost derivation, effect synchronization, commands, the existing Craftux GUI during migration, and the new MapGUI screen all resolve from this store. Installation invalidates tree lookup caches before publication. Version one installs before players join, so it needs no online-player effect resynchronization.

Public mutations become generation-aware:

```java
PurchaseResult purchaseSkillLevel(UUID player, String job, String node, long expectedGeneration);
PurchaseResult purchaseMajor(UUID player, String job, String node, long expectedGeneration);
ResetResult resetTree(UUID player, String job, long expectedGeneration);
```

`PurchaseResult` gains `STALE_GENERATION`, `UNRESOLVED_STATE`, `DISABLED_NODE`, and `OWNER_UNAVAILABLE`. `ResetResult` is typed instead of boolean and has corresponding failures. All command and GUI callers migrate; old mutation signatures are removed.

## 6. Validation, identity, and persistence

Registration validates owner namespace, owner plugin/classloader, live `JobService` IDs, complete graph structure, root, positions, levels, costs, requirements, references, acyclicity, exclusions, state writes, base fingerprints, and handler ownership before publication. Duplicate owner, tree, node, capability, and state keys reject the whole transaction. Providers cannot claim the reserved `modularjobs` namespace.

Node IDs are persistence identifiers. Display text, icon, and position may change; ID, kind, cost history, and semantics may not be removed, renamed, or reused without a future explicit migration contract. Version one defines no migration API and therefore rejects such changes.

State resolution is lazy and fail-closed; installation does not claim to scan PostgreSQL:

```java
sealed interface UpgradeStateResolution {
    record Resolved(SkillTreeState state) implements UpgradeStateResolution {}
    record Unresolved(SkillTreeState rawState, Set<String> unknownNodeIds)
            implements UpgradeStateResolution {}
}
```

On state read, login restore, GUI open, purchase, or reset, every persisted node ID must resolve in the captured snapshot. Unknown IDs return `Unresolved`; available/spent points are not calculated, effects are not newly activated, and purchase/reset are blocked. The raw IDs remain stored and visible in an operator diagnostic. Reset never filters or deletes them. Restoring a compatible provider jar resolves the state on the next access.

The initial perks release introduces new IDs and needs no prior provider migration. Its full Miner replacement preserves shipped core IDs exactly.

Every screen/session stores the snapshot generation returned with its tree view. A stale mutation fails before persistence and refreshes the screen.

### 6.1 Capability reconciliation storage

Capability activation is eventually reconciled with persisted node state. Add this table to the externally applied `paper/src/main/resources/sql/postgres.sql` schema:

```sql
CREATE TABLE upgrade_capability_reconciliation (
    player_id UUID NOT NULL,
    job_key TEXT NOT NULL,
    source_effect_id TEXT NOT NULL,
    desired_active BOOLEAN NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_error TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (player_id, job_key, source_effect_id)
);
```

Neither plugin executes DDL. Operators apply the updated schema with `scripts/apply-postgres-schema.sh` before installing the new jars; startup schema validation rejects a missing table with the existing connect-only guidance.

A purchase/reset database transaction writes both node state and the desired capability transition. After commit, the dispatcher calls the handler and deletes the reconciliation row on success. Retryable failure increments `attempt_count`, records `last_error`, and is retried on login plus a bounded server-thread scheduler. Permanent failure remains diagnostic and blocks further mutation of that player/job until repaired.

The source-effect ID is owner/capability/job/node/level specific, so two nodes granting the same capability reconcile independently. Handlers must be idempotent for that source ID. Desired-state diffing, not invocation count, decides activate versus revoke.

## 7. Capability effects

Add one API-owned permitted effect:

```java
record CapabilityEffect(
        Key capability,
        int schemaVersion,
        Map<String, String> payload
) implements NodeEffect {
    CapabilityEffect {
        payload = Map.copyOf(payload);
    }
}
```

Providers cannot define arbitrary `NodeEffect` subtypes. The central dispatcher validates and describes this envelope through the registered `CapabilityHandler`, and handles purchase, login restoration, reset, and reconciliation retries. Missing handlers, unsupported versions, malformed payloads, foreign ownership, or unknown capabilities reject installation.

`CapabilityResult` is `Success`, `RetryableFailure(message)`, or `PermanentFailure(message)`. Exceptions are converted to retryable failures, logged with owner/player/job/node/source context, and never reported as success. Reconciliation diagnostics are queryable by the admin diagnostic command and cleared only after a successful retry or explicit operator repair.

Built-in boosts, ruled boosts, permissions, recipes, and state writes keep their existing semantics but consume the same snapshot. Income and XP specializations use ruled boosts rather than duplicate payment code.

## 8. Normative tree definitions

Tree content ships as one validated JSON resource per job under `perks/src/main/resources/trees/`. Those files, not prose builders, are the reviewable source of truth. A test parses every resource and compares it with the table below.

### 8.1 Common topology

Except for the preserved core Miner cluster, every provider tree uses this layout:

- root `mayhem_perks.<job>.root` at `(0, 0)`, cost zero;
- branch A at `y=-4`, branch B at `y=0`, branch C at `y=4`;
- each first skill requires the root;
- each second skill requires the first skill at maximum level;
- each major costs five points, requires job level 40 and its second skill at maximum;
- the three provider majors exclude one another;
- original multi-level skills cost `[1, 2, 3]` and require job levels `[5, 15, 25]`;
- archive-derived multi-level skills without an inline override cost `[1, 1, 1]` and require job levels `[10, 20, 30]`;
- archive-derived single-level skills without an inline override cost one point and require job level 10;
- inline gates and values replace these defaults; their cost remains one point per archived tier unless stated otherwise;
- every abbreviated table ID expands to `mayhem_perks.<job>.<id>`;
- icons are explicit namespaced Material keys; graph validation rejects null, duplicate, or out-of-bounds positions.

The graph viewport accepts integer coordinates `x=0..12`, `y=-4..4`. MapGUI projects these through one geometry function with 24-pixel horizontal and vertical spacing. The preserved Miner companion cluster occupies `x=0..8`, `y=8..12`; provider branches begin below it and cannot overlap.

Legend:

- **A**: directly archive-derived behavior or advanced-job multiplier;
- **N**: new balanced behavior for ModularJobs;
- percentages apply only to the named job action through `RuledBoostEffect`;
- `cap:` names a provider capability handler;
- `recipe:` names a ModularJobs recipe-unlock effect.

### 8.2 Archive-backed jobs

| Job/branch | First skill | Second skill | Major |
|---|---|---|---|
| `builder` Safety | `fall_immunity` **A**, gate 2, `cap:fall_immunity`, qualifying placement grants 60 seconds of cannot-die-from-fall protection while normal damage remains | `safe_landing` **N**, 3 levels, reduce eligible fall damage 10/20/30% | `master_mason` **N**, +25% qualifying stone/brick placement income |
| `builder` Placement | `reach` **A**, gates 20/30/40, placement range 10/15/20 | `block_chaining` **A**, gates 20/30, cap 5/10 placed blocks | `aerial_architect` **N**, Town Flight duration +50% |
| `builder` Artistry | `painting` **A**, gate 10, namespaced recolor recipes | `town_flight` **A**, gates 10/20/30, duration 60/180/300 seconds | `master_painter` **N**, +50% qualifying decorative-block income |
| `lumberjack` Mobility | `tree_climb` **A**, gates 10/20, cap 16/32 blocks | `canopy_stride` **N**, 3 levels, leaf movement speed +5/10/15% | `lumberjack` **A**, +25% log-breaking income |
| `lumberjack` Harvest | `efficient_felling` **N**, process at most 2/4/6 connected logs | `tool_conservation` **N**, 10/20/30% chance not to consume extra durability from provider processing | `orchardist` **A**, x2 configured fruit-tree harvest income |
| `lumberjack` Stewardship | `sapling_recovery` **N**, +10/20/30% configured sapling chance | `automatic_replanting` **N**, replant one consumed sapling after protected checks | `tree_planter` **A**, x2 planting income |
| `miner` Discovery | `ore_radar` **A**, gates 10/15/20/25/30/35/40 for coal+iron/quartz/lapis/redstone/gold/emerald+diamond, cooldowns 5/5/5/10/10/15/15/30 minutes by configured tier | `prospecting` **N**, radius 8/12/16 loaded blocks | `prospector` **A**, +50% gold mining/smelting income |
| `miner` Processing | `condense_drops` **A**, gate 10, explicit 9-to-1 block recipes | `inventory_efficiency` **N**, auto-condense threshold 18/36/54 eligible drops | `carbon_miner` **A**, +25% coal/diamond income |
| `miner` Mobility | `grappling_hook` **A**, gate 20, range 16 loaded blocks | `safe_descent` **N**, 3 levels, reduce hook landing damage 25/50/75% | `geologist` **A**, +50% quartz and stone income |
| `farmer` Cultivation | `cultivation_radius` **A**, gates 10/30/50, radius 1/2/3 | `cultivation_range` **A**, gates 10/20/30/40/50, range 2/4/6/8/10, modes Plow/Plant/Fertilize/Harvest | `harvester` **A**, +50% configured crop income |
| `farmer` Husbandry | `pig_farming` **A**, gate 10, mounted plow/harvest | `husbandry_yield` **N**, +10/20/30% breed/shear/milk income | `miller` **A**, x2 wheat and bread income |
| `farmer` Vineyard | `vine_yield` **N**, +10/20/30% configured vine-crop yield | `compost_mastery` **N**, 10/20/30% fertilize-cost conservation | `vigneron` **A**, +50% vine-crop income |
| `hunter` Tracking | `tracking` **A**, gates 10/20/30, configured normal/heroic/legendary categories, 100 horizontal and 32 vertical blocks | `field_awareness` **N**, cooldown 60/45/30 seconds | `tracker` **N**, range +25% without revealing hidden players |
| `hunter` Trapping | `trapping` **A**, gates 10/20/30 for configured passive/rare/horse tiers, wait 10–15 minutes | `humane_release` **N**, recover trap with 50/75/100% durability | `trapper` **A**, +50% configured animal/trap income |
| `hunter` Ammunition | `xo_arrows` **A**, gates 10/15/20/25/30/40 for Fireball/Poison/Explosive+Slow/Piercing/Net+Compression/Decaying | `arrow_control` **N**, cooldown reduction 10/20/30% | `sniper` **A**, x2 qualifying headshot income |
| `fisherman` Angling | `angler` **A**, +10/20/25% fish income | `vehicle_fishing` **A**, gate 10, archive 2.5x XP while in boat; pig behavior disabled by default | `deep_sea_fisher` **A**, x2 ocean fishing and pufferfish income |
| `fisherman` Rivers | `fly_fishing` **A**, +25/50/100% river/salmon income | `current_reader` **N**, reduce bite wait 5/10/15% within Paper API limits | `fly_fisher` **A**, x2 river fishing income |
| `fisherman` Commerce | `fish_cooking` **A**, +25/50/100% configured cooked-fish income | `market_catch` **N**, +10/20/30% rare-fish income | `fish_monger` **A**, x2 fish cooking and configured trade income |
| `blacksmith` Defense | `chain_armor` **A**, gate 5, `recipe:chain_armor` | `fire_affinity` **A**, gate 10, reduce fire damage 50% | `armorsmith` **A**, +50% armor crafting income |
| `blacksmith` Forging | `horse_armor` **A**, gates 10/15/20 for iron/gold/diamond recipes | `material_efficiency` **N**, alternate recipes save one eligible ingot at levels 1/2/3 | `weaponsmith` **A**, +50% sword/axe crafting income |
| `blacksmith` Anvil | `anvil_crafting` **A**, gate 20, namespaced forge recipes | `repair_mastery` **N**, +10/20/30% repair income | `mechanic` **A**, +50% repair income |
| `enchanter` Ensorcell | `ensorcell` **A**, gate 10, namespaced selection UI capability | `enchant_tiers` **A**, gates 10/15/20/25/30/35 for tiers I–VI | `arcanist` **A**, +25% weapon/armor enchant income |
| `enchanter` Implements | `tool_enchants` **A**, allowlisted Molten Touch/Drill progression | `weapon_enchants` **A**, allowlisted archive weapon effects | `beguiler` **A**, +50% tool enchant income |
| `enchanter` Scholarship | `tomes` **A**, gate 35, tier VII namespaced tome recipes | `book_income` **A**, +25/50/100% writing/crafting book income | `scholar` **A**, x2 writing and book-crafting income |
| `alchemist` Brewing | `bottle_ammo` **A**, gate 5, configured splash heal/harm bottle substitution | `cauldron_brewing` **A**, gates 10/30/40 for 2/3/4 ingredients | `chemist` **A**, x2 potion extension/amplification income |
| `alchemist` Transmutation | `transmutation` **A**, gate 10, explicit 9 iron ore to 1 gold ore recipe | `spirit_potion` **A**, gate 10, allowlisted possession recipe | `scientist` **A**, +25% beneficial-potion income |
| `alchemist` Medicine | `medic_brewing` **A**, +25/50/100% health/regeneration/absorption potion income | `apothecary_brewing` **A**, +10/25/50% detrimental potion income | `medic` **A**, x2 health/regeneration/absorption potion income |
| `cooking` Meals | `food_tiers` **A**, gates 1/20/30 for tier I/II/Epic recipes | `ingredient_efficiency` **N**, alternate recipes save one configured ingredient at levels 1/2/3 | `gourmet_chef` **A**, +50% dinner-food income |
| `cooking` Pastry | `pastry_recipes` **A**, tiered cookie/cake/pie recipes | `baker_yield` **N**, +10/20/30% bake income | `pastry_chef` **A**, x2 pastry income |
| `cooking` Rotisserie | `roast_recipes` **A**, configured fish/meat/vegetable/chicken recipes | `hearty_meals` **N**, recipe unlocks only, no stacked potion effects | `rotisseur` **A**, x2 configured roast income |
| `artisan` Sponges | `blue_sponge` **A**, gate 10, 5x5x5 water and ten uses | `red_sponge` **A**, gate 20, 5x5x5 lava and ten uses | `fabricator` **A**, +25% crafted-block income |
| `artisan` Fabrication | `job_items` **A**, gate 30, allowlisted namespaced recipes | `craft_efficiency` **N**, +10/20/30% configured craft income | `craftsman` **A**, +25% crafted-item income |
| `artisan` Spawners | `spawner_crafting` **A**, gates 20/30/40 for configured entity tiers, disabled by default | `spawner_safety` **N**, enforce ownership/separation policy | `woodworker` **A**, +50% wooden block/item income |
| `engineering` Machinery | `machine_crafting` **A**, +10/25/50% piston/dispenser/hopper income | `slime_efficiency` **A**, x2 configured sticky-piston income at max | `machinist` **A**, +50% redstone block/item income |
| `engineering` Rail | `rail_crafting` **A**, +10/25/50% rail craft income | `rail_placement` **A**, +10/25/50% rail placement income | `rail_engineer` **A**, +50% both rail actions |
| `engineering` Automation | `golem_crafting` **A**, +10/25/50% snow/iron golem income | `automation_devices` **N**, +10/20/30% observer/comparator income | `roboticist` **A**, +50% piston/dispenser/hopper income |

Miner is the one topology exception: its four shipped mutually exclusive companion majors remain unchanged in a preserved cluster. The three provider majors exclude one another but do not alter those core exclusions.

### 8.3 Original trees

All original skills use the common three-level costs/gates; the percentages below are level 1/2/3.

| Job/branch | First skill | Second skill | Major |
|---|---|---|---|
| `herbalism` Harvest | `wild_harvest`, +5/10/15% configured plant yield | `rare_cuttings`, +5/10/15% rare material chance | `forager`, +25% wild-plant income |
| `herbalism` Stewardship | `seed_recovery`, +10/20/30% seed recovery | `automatic_replanting`, protected one-block replant | `cultivator`, +25% cultivated-herb income |
| `herbalism` Apothecary | `ingredient_quality`, +5/10/15% potion-material income | `ingredient_preservation`, 5/10/15% conservation | `apothecary`, +50% configured potion-ingredient income |
| `smelting` Fuel | `fuel_economy`, 5/10/15% furnace fuel extension | `heat_retention`, retain 1/2/3 eligible operations after fuel exhaustion | `fuel_master`, +25% fuel efficiency |
| `smelting` Batch | `batch_smelting`, +5/10/15% smelt income | `ore_throughput`, +5/10/15% eligible ore XP | `foundry_worker`, +25% smelting income |
| `smelting` Heat | `fire_resistance`, reduce fire damage 10/20/30% | `lava_handling`, reduce lava damage 10/20/30% | `pyrometallurgist`, 50% fire/lava reduction while operating a furnace |
| `milling` Yield | `grain_yield`, +5/10/15% product yield | `bran_recovery`, +5/10/15% byproduct chance | `miller`, +25% milling income |
| `milling` Batch | `batch_processing`, +5/10/15% milling XP | `hopper_milling`, allowlisted automated-task credit | `provisioner`, +25% milling XP |
| `milling` Refinement | `refined_flour`, three recipe tiers | `fine_grind`, +5/10/15% premium ingredient chance | `master_gristmill`, unlock all premium flour recipes |
| `tanning` Recovery | `hide_recovery`, +5/10/15% eligible hide yield | `clean_skinning`, +5/10/15% pristine-hide chance | `skinner`, +25% hide income |
| `tanning` Curing | `efficient_curing`, 5/10/15% input conservation | `quick_curing`, +5/10/15% tanning XP | `currier`, +25% tanning XP |
| `tanning` Leather | `pristine_leather`, three recipe tiers | `reinforced_leather`, durability recipe tiers | `leatherworker`, unlock all pristine-leather recipes |
| `refining` Yield | `ore_yield`, +5/10/15% refining output | `slag_recovery`, +5/10/15% byproduct chance | `assayer`, +25% refining income |
| `refining` Process | `efficient_refining`, 5/10/15% input conservation | `purity_control`, +5/10/15% pure-material chance | `refiner`, +25% refining XP |
| `refining` Materials | `pure_materials`, three recipe tiers | `alloy_mastery`, three allowlisted alloy tiers | `metallurgist`, unlock all provider alloy recipes |
| `armorsmithing` Material | `armor_efficiency`, three explicit alternate recipe tiers | `scrap_recovery`, +5/10/15% configured scrap chance | `plateworker`, +25% armor craft income |
| `armorsmithing` Durability | `tempered_armor`, +5/10/15% namespaced durability metadata | `repairable_plates`, +5/10/15% repair income | `master_armorer`, +25% repair and armor income |
| `armorsmithing` Wards | `defensive_sets`, three allowlisted set tiers | `ward_stability`, reduce set cooldown 10/20/30% | `ward_smith`, unlock top set tier |
| `tailoring` Fiber | `fiber_economy`, +5/10/15% configured fiber yield | `scrap_reuse`, 5/10/15% input conservation | `weaver`, +25% textile income |
| `tailoring` Dye | `dye_mastery`, three color recipe tiers | `colorfast`, 10/20/30% dye conservation | `dyer`, unlock all provider dye recipes |
| `tailoring` Garments | `utility_garments`, three allowlisted garment tiers | `garment_repair`, +5/10/15% repair income | `outfitter`, unlock top garment tier |

### 8.4 Progression for currently taskless jobs

Eight current jobs have no bundled `job_tasks.csv` rows and would otherwise be unable to reach level-gated perks. The implementation adds conservative core task rows as a supporting change:

- `smelting`: `smelt` configured ores and raw materials;
- `milling`: `craft` configured grain products;
- `tanning`: `craft` configured hide/leather products;
- `refining`: `smelt` configured refined materials;
- `cooking`: `bake` and `craft` configured foods;
- `armorsmithing`: `craft` and `repair` configured armor;
- `tailoring`: `craft` configured textile and leather garments;
- `engineering`: `craft` and `block_place` configured redstone/rail devices.

Exact material rows, XP, and money values are explicit CSV resources and balance-tested against existing jobs. This is a core content update, not provider-side job registration.

### 8.5 Provenance and adaptations

Direct archive values in the table come from the linked May 2015 job page or `Advanced Jobs`; new behavior is marked **N**. Builder branch-major names, Lumberjack felling/replant mechanics, and every unmatched-job tree are new designs, not archived claims. Fisherman and Inventor pages explicitly report no unique base ability; their **A** rows are advanced-job specializations from `Advanced Jobs`.

Farmer husbandry adapts documented Rancher concepts from [Rancher](https://web.archive.org/web/20150529234924id_/http://wiki.mayhem-multiverse.com/index.php/Rancher). If two May captures conflict, the latest capture for that individual page wins; values are never borrowed from an older revision.

Production tree JSON contains only fields accepted by `SkillTreeConfigParser`. Provenance is a separate `perks/src/main/resources/provenance.json` catalog keyed by the complete stable node ID with `source_url`, capture timestamp, page title, archive/new classification, and adaptation note. A test requires one catalog entry for every provider node and verifies that every **A** entry has a Wayback URL.

## 9. Runtime safety and server integration

All Bukkit world, block, entity, inventory, protection, and event interactions run on the server thread unless Paper explicitly documents an API as safe off-thread.

Ore Radar:

1. Verify cooldown and permissions on the main thread.
2. Inspect only already loaded chunks.
3. Capture a bounded immutable block snapshot on the main thread.
4. Run pure nearest-target computation on one dedicated executor with queue capacity 32 and a two-second timeout; cancel on logout, shutdown, or stale generation.
5. Return the result and player feedback on the main thread after validating player/session generation again.

Area farming, block chaining, tree processing, traps, arrows, spawners, and recipes fire or honor normal Bukkit/Paper events and protection checks. Each activation has configurable block/entity/radius/work limits. Operations abort rather than partially bypassing a cancelled event.

Hook-dependent capabilities declare their requirements. If Towny, a region/protection adapter, or another required hook is absent, the affected node is disabled during contribution validation and MapGUI displays the reason. The plugin never silently exposes a nonfunctional perk. Optional hooks use dedicated adapters and no reflection in domain logic.

Defaults are conservative:

- terrain-damaging projectile effects disabled;
- spawner crafting disabled until an entity allowlist and placement policy are configured;
- Town Flight disabled without a supported build-permission adapter;
- tracking hidden players disabled unless the viewer has the configured permission;
- scans never load chunks;
- no unrestricted item duplication or arbitrary command execution.

## 10. Configuration

The provider owns `perks/src/main/resources/config.yml`; tests load `perks/src/test/resources/config/valid.yml`, `invalid.yml`, and `missing-hooks.yml`. The shipped schema is:

```yaml
capabilities:
  ore-radar:
    enabled: true
    radius: 16                 # 1..32 loaded blocks
    max-blocks: 35937          # (2r+1)^3, hard ceiling
    cooldown-seconds: 300      # 1..86400
  area-operations:
    enabled: true
    max-blocks: 81             # 1..256 per activation
  tracking:
    enabled: true
    horizontal-range: 100      # 1..256
    vertical-range: 32         # 1..128
    hidden-player-permission: modularjobs.perks.track.hidden
  projectile-terrain-damage:
    enabled: false
  spawner-crafting:
    enabled: false
    entities: []
    minimum-separation: 32     # 1..256 blocks
  town-flight:
    enabled: false
    protection-adapter: none   # none | towny
worlds:
  allow: []                    # empty means all not denied
  deny: []
reconciliation:
  retry-seconds: 30            # 5..3600
  max-attempts-per-cycle: 32   # 1..256
```

Per-capability material, entity, ore, biome, food, and recipe allowlists are explicit sibling keys in the same file and default to the exact entries referenced by the tree resource. Deny wins over allow; keys are lower-case namespaced registry keys; invalid keys are errors. No runtime config reload is supported in version one.

Tree topology and stable IDs are code-owned JSON definitions. Operators tune bounded behavior but cannot rename nodes or provide capability payloads. A syntactically invalid file or out-of-range safety limit disables the entire perks plugin before installation. A valid disabled capability or absent optional hook keeps its node and handler in the snapshot but records `disabledReason`; MapGUI displays that reason and purchase returns `DISABLED_NODE`. Missing required ModularJobs/MapGUI dependencies reject plugin startup.

## 11. Craftux replacement and MapGUI integration

MapGUI is not implemented on the current branch. The current route is:

`UpgradesCommand` → `UpgradeTreeGui` → `CraftuxUiHost`, with `PluginContext` constructing those objects. The implementation first completes the approved boundary in `docs/superpowers/specs/2026-08-11-mapgui-upgrade-tree-design.md`:

- retain `UpgradeTreeGui` as the command-facing opener name;
- replace its Craftux session implementation with `UpgradeTreeScreen`, a MapGUI `Screen`;
- construct the opener from `PluginContext` with `UpgradeService`, `UpgradeSnapshotStore`, and capability-description service;
- remove upgrade-tree actions from `CraftuxUiHost` only after every caller uses the MapGUI opener;
- keep Craftux for unrelated UIs;
- declare MapGUI API 1.0.0 as `compileOnly` and required in ModularJobs `paper-plugin.yml`.

The MapGUI graph uses `Draw` for prerequisite edges and node hit targets. `UpgradeTreeScreen` captures `UpgradeTreeView(tree, generation, disabledReasons)`. `SkillNodeDetailScreen` describes built-in and capability effects without executing them, and delegates purchase only to generation-aware `UpgradeService`. Major confirmation calls `purchaseMajor` exactly once. Disabled nodes are visible, include their reason, and cannot purchase. Stale results rebuild the screen from the latest view.

Provider nodes need no provider-owned screen and `perks` imports no MapGUI class. They appear because the screen reads the canonical snapshot. The existing Craftux v2 path remains only until the MapGUI smoke test passes, then is deleted in the same clean cutover.

## 12. Failure handling

- Missing ModularJobs or incompatible API: `perks` refuses enablement.
- Invalid contribution: atomic rejection; built-in snapshot stays active.
- Duplicate or foreign key: atomic rejection with owner/job/node diagnostics.
- Unknown persisted node: player/job state becomes unresolved; purchase and reset fail closed.
- Missing hook: affected nodes are disabled before display, with a reason.
- Capability exception: isolate, log, record reconciliation failure, and continue unrelated effects.
- Registration after the initial provider enable transaction: typed lifecycle rejection.
- Stale MapGUI generation: no mutation; screen refresh.
- Database failure: existing `UpgradeService` transaction behavior remains authoritative; provider code never writes upgrade state.
- Unsupported provider reload: reject and instruct the operator to restart both plugins.

## 13. Verification

### 13.1 API and registration tests

- provider can install one complete valid batch;
- second owner installation and second installation by the same owner behave deterministically;
- invalid job, graph, prerequisite, exclusion, state write, node collision, or capability rejects the full transaction;
- provider cannot register a reserved namespace or access purchase/reset persistence methods;
- API class identity works with the real two-plugin Paper descriptors;
- all core consumers observe one snapshot generation after installation.

### 13.2 Persistence tests

- existing built-in Miner IDs, semantics, and levels survive the complete provider replacement;
- provider purchases survive restart with identical stable IDs;
- unknown persisted provider IDs block purchase and reset;
- reset never deletes unresolved IDs;
- point calculation never treats unresolved IDs as free;
- renamed, deleted, or reused IDs reject installation without migration;
- stale-generation purchases never write state.

### 13.3 Graph and MapGUI tests

- all 19 current jobs resolve one positioned graph;
- node and edge geometry is deterministic;
- disabled nodes show their reason;
- archive level requirements and branch prerequisites gate correctly;
- exclusive majors cannot be combined;
- capability descriptions render without executing handlers;
- purchases route through `UpgradeService` exactly once;
- provider trees appear in the same MapGUI graph as compatible core miner nodes.

### 13.4 Capability tests

Each mechanic has contract tests for activation, revocation, cooldown, boundaries, allowlists, cancelled events, protection denial, missing hooks, player logout, and configuration limits. Tests use real Paper/MockBukkit objects where the repository convention supports them; pure calculations remain Paper-free unit tests.

Boundary tests include:

- Ore Radar at scan radius edge, unloaded-chunk boundary, no matching ore, logout during computation, and stale generation;
- area operations at maximum work count and first cancelled block event;
- tracking hidden players and vertical/range boundaries;
- spawner/entity/material allowlist rejection;
- recipe namespace collisions;
- effect handler exception isolation and diagnostic retention.

### 13.5 End-to-end smoke test

Run Paper with PostgreSQL schema, ModularJobs, MapGUI, and the separate perks jar:

1. confirm both plugins enable and one provider installation succeeds;
2. open `/jobs upgrade miner` and verify core/provider nodes share one graph;
3. purchase one built-in-effect node and one capability node;
4. exercise the changed behavior in game;
5. reconnect and confirm effect restoration;
6. restart and confirm levels, points, and effects remain consistent;
7. open representative archive-backed and original trees;
8. remove a required optional hook and confirm affected nodes show disabled reasons;
9. introduce an incompatible persisted node in a test fixture and confirm purchase/reset fail closed.

Run focused module tests, then `./gradlew :api:test :common:test :paper:test :perks:test :paper:build :perks:build`.

## 14. Documentation and release artifacts

Implementation updates these exact artifacts:

- `README.md`: separate jar installation, dependency order, and build outputs;
- `paper/src/main/resources/paper-plugin.yml`: required MapGUI metadata;
- `perks/src/main/resources/paper-plugin.yml`: required ModularJobs metadata;
- `perks/src/main/resources/config.yml`: bounded safety defaults;
- `web/src/content/docs/` perk catalog: archive/new labels, sources, and adaptation notes;
- `web/src/content/docs/` extension API reference: ownership, full replacement, stable IDs, and lifecycle;
- `docs/database-schema.md`: reconciliation table and connect-only rollout;
- `paper/src/main/resources/sql/postgres.sql`: reconciliation DDL;
- `scripts/apply-postgres-schema.sh`: remains the only schema application path;
- `CHANGELOG.md`: separate plugin, API, MapGUI, schema, and behavior changes;
- root release packaging: `paper/build/libs/paper-all.jar` and a separately named `perks/build/libs/modularjobs-perks-all.jar`.

Packaging tests assert both jars exist, plugin descriptors are correct, the perks jar does not embed ModularJobs/MapGUI classes, and release archives contain both artifacts without merging them.

## 15. Non-goals

- Recreating all 20 archived jobs.
- Replacing ModularJobs persistence, purchase gates, upgrade points, or MapGUI.
- Provider-owned GUI screens.
- Arbitrary third-party `NodeEffect` subclasses.
- Runtime hot replacement or supported plugin reload in version one.
- Loading chunks for scans.
- Bypassing protection plugins or cancelled Bukkit events.
- Exact recreation of obsolete Towny, mcMMO, item-ID, or pre-1.21 behavior.
- Automatic refunds for removed provider nodes.
- Operator-editable stable node IDs or arbitrary capability payloads.
