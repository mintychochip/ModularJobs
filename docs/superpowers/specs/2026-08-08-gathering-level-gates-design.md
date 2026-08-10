# Gathering Profession Level Gates

**Date:** 2026-08-08
**Status:** Superseded by `2026-08-10-modularjobs-azoth-integration-design.md`

The interaction scope remains approved, but Azoth now owns gate configuration,
predicates, messages, and enforcement; ModularJobs owns progression and payment.

## Goal

Make gathering progression control access to the configured gathering resources,
not only the XP/economy payout. A player below the configured profession level,
or without a level in that profession, cannot perform the gated interaction.
Players at or above the level proceed normally. Operators retain explicit bypass
permissions.

## Scope

Included gathering professions:

- Mining (`miner` storage key)
- Woodcutting (`lumberjack` storage key)
- Herbalism (`herbalism` storage key)
- Farming (`farmer` storage key)
- Fishing (`fisherman` storage key)

Included actions:

- Block breaking for mining blocks, logs/stems, farm crops, and Herbalism plants.
- Log stripping with an axe.
- Mature berry/cocoa harvesting through right-click interaction.
- Vanilla fish catches for cod, salmon, tropical fish, and pufferfish.

Excluded actions:

- Farming breeding, shearing, and milking.
- Fishing junk and treasure (`nautilus_shell`, `enchanted_book`).
- Tool-tier or enchantment rules; normal Minecraft restrictions remain unchanged.
- Wildcards, category fallbacks, GUI/editor configuration, and database schema changes.

Herbalism receives a new resource/task catalog in this change because the job
currently has no task rows.

## Level policy

All gates use explicit material entries. Level 1 gates require the player to have
joined the profession because an absent profession level is treated as level 0.
The selected tier policy is:

### Mining block gates

| Minimum level | Materials |
|---:|---|
| 1 | `stone`, `cobblestone`, `granite`, `diorite`, `andesite`, `netherrack` |
| 5 | `deepslate`, `cobbled_deepslate`, `tuff`, `calcite`, `dripstone_block`, `coal_ore`, `deepslate_coal_ore`, `basalt`, `blackstone`, `end_stone` |
| 10 | `iron_ore`, `deepslate_iron_ore`, `copper_ore`, `deepslate_copper_ore`, `glowstone`, `nether_quartz_ore` |
| 15 | `redstone_ore`, `deepslate_redstone_ore`, `lapis_ore`, `deepslate_lapis_ore` |
| 20 | `gold_ore`, `deepslate_gold_ore`, `nether_gold_ore`, `obsidian`, `crying_obsidian`, `amethyst_block`, `budding_amethyst` |
| 30 | `diamond_ore`, `deepslate_diamond_ore` |
| 35 | `emerald_ore`, `deepslate_emerald_ore` |
| 40 | `ancient_debris` |

### Woodcutting block and strip gates

| Minimum level | Materials |
|---:|---|
| 1 | `oak_log`, `spruce_log`, `birch_log`, `jungle_log`, `acacia_log`, `dark_oak_log` |
| 10 | `mangrove_log`, `cherry_log` |
| 15 | `crimson_stem`, `warped_stem` |

The same material/level table applies to both breaking and stripping. Breaking
uses `block-break-gates`; stripping uses `interaction-gates.strip_log`.

### Farming block gates

| Minimum level | Materials |
|---:|---|
| 1 | `wheat`, `carrots`, `potatoes`, `beetroots` |
| 5 | `sugar_cane`, `cactus` |
| 10 | `melon`, `pumpkin` |
| 15 | `nether_wart` |

Mature right-click plant gates use `interaction-gates.plant_harvest`:

- level 10: `sweet_berry_bush`, `cave_vines`, `cave_vines_plant`
- level 15: `cocoa`

`CAVE_VINES` and `CAVE_VINES_PLANT` are block targets for the existing glow
berry state. There is no separate cave-berry material. The gate matches only
when the existing payment predicate says the vine has berries.

### Fishing gates

The existing fish gate design remains:

| Minimum level | Item |
|---:|---|
| 1 | `cod` |
| 10 | `salmon` |
| 20 | `tropical_fish` |
| 30 | `pufferfish` |

### Herbalism resource catalog

Herbalism tasks use `block_break` contexts and the following gate tiers:

| Minimum level | Materials |
|---:|---|
| 1 | `short_grass`, `fern`, `dandelion`, `poppy`, `brown_mushroom`, `red_mushroom` |
| 5 | `tall_grass`, `large_fern`, `azure_bluet`, `red_tulip`, `orange_tulip`, `white_tulip`, `pink_tulip`, `lily_pad`, `seagrass`, `kelp` |
| 10 | `blue_orchid`, `allium`, `oxeye_daisy`, `cornflower`, `lily_of_the_valley`, `moss_block`, `moss_carpet`, `glow_lichen`, `hanging_roots` |
| 15 | `spore_blossom`, `big_dripleaf`, `small_dripleaf`, `big_dripleaf_stem`, `sea_pickle`, `crimson_fungus`, `warped_fungus`, `nether_sprouts`, `twisting_vines`, `weeping_vines` |
| 20 | `wither_rose`, `torchflower`, `pink_petals`, `pitcher_plant`, `pitcher_crop` |

Herbalism rewards follow the existing task scale: common entries pay 1 XP /
0.1 economy, tier-5 entries pay 2 XP / 0.2 economy, tier-10 entries pay 3 XP /
0.5 economy, tier-15 entries pay 5 XP / 1 economy, and tier-20 entries pay 8
XP / 3 economy. Existing Farmer resources are not duplicated into Herbalism.

## Configuration

Keep the current explicit sections and populate them with the approved material
tables:

```yaml
block-break-gates:
  diamond_ore: { profession: mining, level: 30 }

fish-catch-gates:
  salmon: { profession: fisherman, level: 10 }

interaction-gates:
  strip_log:
    oak_log: { profession: woodcutting, level: 1 }
  plant_harvest:
    sweet_berry_bush: { profession: farming, level: 10 }
    cave_vines: { profession: farming, level: 10 }
    cave_vines_plant: { profession: farming, level: 10 }
    cocoa: { profession: farming, level: 15 }
```

The new interaction loader validates the action key, material key, profession
(alias or canonical id), and positive integer level. Invalid entries warn and
are skipped, matching the existing gate loaders.

## Runtime architecture

- Reuse `BlockBreakGate`, `BlockBreakGateService`, `YamlBlockBreakGateLoader`,
  `BlockBreakGateStore`, and `BlockBreakGateListener` for all block materials.
- Reuse `FishCatchGate` and its existing loader/store/listener for fish.
- Add an API-level `InteractionGate` record with `actionKey`, `materialKey`,
  `professionId`, and `minLevel`, plus a read-only lookup service keyed by
  action and material.
- Add a Paper YAML loader/store and `InteractionGateListener` at
  `EventPriority.NORMAL` with `ignoreCancelled = true`.
- Add `modularjobs.bypassinteractiongate` with `default: op`.
- Resolve configured profession aliases through `ProfessionCatalog`; store
  canonical ids.

The interaction listener shares exact target predicates with the payment
listener rather than approximating them:

- `strip_log`: right-click block, target is an unstripped log/stem/wood/hyphae,
  and the held item is an axe.
- `plant_harvest`: right-click block, target is one of the four existing berry /
  cocoa blocks, and the block is harvestable according to the same age/berry
  state checks used by payment.

For a denied interaction, call `event.setCancelled(true)` and set both
`useInteractedBlock` and `useItemInHand` to `Event.Result.DENY`. The explicit
cancellation satisfies the existing payment listeners' `ignoreCancelled = true`
contract; the two result values also prevent the vanilla action. Tests must
assert both `event.isCancelled()` and that no payment is generated.

## Data lifecycle

The bundled `job_tasks.csv` is the authoritative seed file when the task table
is empty. Startup intentionally skips import when PostgreSQL already contains
tasks. Add the Herbalism rows to the bundled CSV and provide an operator-run,
idempotent data migration for existing databases; do not make the game process
rewrite existing task data or change schema ownership.

Configuration is loaded at startup, so changing gate YAML requires a server
restart. Update the active `paper/run/plugins/ModularJobs` configuration for the
local server as well as the bundled resource template.

## Testing

Add tests for:

- Interaction gate record normalization and lookup by action/material.
- Loader acceptance of valid entries and rejection of unknown actions/materials,
  unknown professions, malformed levels, and non-positive levels.
- Default configuration coverage for every configured gathering material.
- Block gates below level, at level, above level, absent level, and bypass.
- Fish gates below level, at level, above level, absent gate, and bypass.
- Strip predicate boundaries: wrong action, null block, already stripped block,
  non-log target, missing item, non-axe item, and valid axe interaction.
- Plant predicate boundaries: wrong action, non-plant target, immature sweet
  berries/cocoa, non-berry cave vines, and mature berry-bearing vines.
- Interaction gates below/exact/above level and bypass.
- Cancelled interaction events do not reach payment.

## Acceptance criteria

1. Every material in the existing Mining, Woodcutting, and Farming block-break
   task rows has a matching explicit block gate.
2. Every existing Woodcutting strip-log material has a matching strip gate.
3. Mature Farming berry/cocoa interactions are gated without blocking immature
   growth or unrelated clicks.
4. The four fish species use the approved fishing levels; junk and treasure are
   unchanged.
5. Herbalism has resource task rows and matching block gates for the approved
   plant catalog.
6. Below-level actions are physically denied and produce no job payment; valid
   actions retain existing behavior.
7. Existing operator bypass permissions continue to work, and the new
   interaction bypass defaults to operators.
8. Bundled resources, active local configuration, tests, and the existing
   Postgres connect-only/data-migration workflow remain compatible.
