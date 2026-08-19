# ModularJobs vs JobsReborn — Feature Comparison

> Date: 2026-08-18
> Reference: Zrips/Jobs (JobsReborn) official wiki + zrips.net/jobs
> Method: parallel feature-category analysis against ModularJobs source

## Legend
- ✅ HAS — feature present in ModularJobs (with evidence)
- ❌ LACKS — feature absent (candidate gap)
- 🟡 DIFF — intentional design difference (not a gap)

## 1. Paid actions / job task types

| JobsReborn action | ModularJobs status |
|---|---|
| break | ✅ `ActionTypes.BLOCK_BREAK` → `JobPaymentListener.onBlockBreak` |
| striplogs | ✅ `ActionTypes.STRIP_LOG` → `onStripLog` |
| tntbreak | ✅ `ActionTypes.TNT_BREAK` → `onTntBreak` |
| place | ✅ `ActionTypes.BLOCK_PLACE` → `onBlockPlace` |
| kill | ✅ `ActionTypes.KILL` → `onKill` |
| fish | ✅ `ActionTypes.FISH` → `onFish` |
| craft | ✅ `ActionTypes.CRAFT` → `onCraft` |
| vtrade | ✅ `ActionTypes.VILLAGER_TRADE` → `onVillagerTrade` |
| smelt | ✅ `ActionTypes.SMELT` → `onFurnaceSmelt` |
| brew | ✅ `ActionTypes.BREW` → `onBrewEvent` |
| enchant | ✅ `ActionTypes.ENCHANT` → `onEnchant` |
| repair | ✅ `ActionTypes.REPAIR` → `onCraft` anvil branch |
| breed | ✅ `ActionTypes.BREED` → `onBreedEntity` |
| tame | ✅ `ActionTypes.TAME` → `onTameAnimal` |
| dye | ✅ `ActionTypes.DYE` → `onDyeEntity` + craft dye branch |
| shear | ✅ `ActionTypes.SHEAR` → `onEntityShear` |
| milk | ✅ `ActionTypes.MILK` → `onMilkEntity` |
| explore | ✅ `ActionTypes.EXPLORE` → `onExplore` |
| eat | ✅ `ActionTypes.CONSUME` (`eat`) → `onEat` food branch |
| drink | 🟡 potions share `CONSUME`; no dedicated `drink` type |
| collect | ✅ `ActionTypes.COLLECT` → `onPickupItem`/`onHarvestBerries` |
| bake | ✅ `ActionTypes.BAKE` → `onFurnaceSmelt` edible branch |
| brush | ✅ `ActionTypes.BRUSH` → `onBrushBlock` |
| wax | ✅ `ActionTypes.WAX` → `onWaxBlock` |
| scrape | ❌ no `SCRAPE` constant or listener |
| mmkill (MythicMobs) | ❌ no MythicMobs integration |
| custom kill (kill player w/ profession) | ❌ `KILL` pays any death; no profession filter |
| — | ✅ extra: `BUCKET_ENTITY` (`bucket`) — not in JobsReborn |

## 2. Economy & rewards

| JobsReborn feature | ModularJobs status |
|---|---|
| Vault economy | 🟡 uses **Mint** reflectively (intentional) + blackhole/fail fallback |
| Custom point economy (`/jobs points`, editpoints) | ❌ only `EXPERIENCE` + `ECONOMY` payables; "points" = upgrade skill points |
| Money/Exp/Points boost via permission (`jobs.boost.*`) | ❌ boost sources are items/timed/upgrade-tree; no permission-based boost |
| Money/Exp boost by wearing/using items | ✅ `ItemBoostDataService` + `BoostEngine.aggregateItemSources` |
| Payment limits over time (`/jobs limit`, plimit) | ❌ no limit tracking/command/placeholders |
| Taxes (`jobs.tax.*`) | ❌ no tax deduction in pipeline |
| Dynamic payment / bonus placeholders | ❌ expansion only exposes `experience` |
| Shop / limited items | ❌ no shop, no `/jobs give` |

## 3. Commands & player interfaces

| JobsReborn command | ModularJobs status |
|---|---|
| browse | ✅ `BrowseCommand` |
| info | ✅ `InfoCommand` (chat + craftux GUI) |
| join / leave / leaveall | ✅ `JoinCommand` / `LeaveCommand` |
| stats | ✅ `StatsCommand` (chat + GUI) |
| archive | ✅ `ArchiveCommand` |
| top / gtop | 🟡 per-job `top` (chat + scoreboard); ❌ no global `gtop` |
| exp/level add/take/set | ✅ `ExperienceCommand` / `LevelCommand` |
| boost / itemboost / source | ✅ `BoostCommand` / `ItemBoostCommand` / `SourceCommand` |
| upgrade | ✅ `UpgradesCommand` |
| list | ✅ `ListCommand` |
| points | ❌ |
| quests / resetquest / skipquest | ❌ (no quest system) |
| bonus / limit / placeholders / toggle | ❌ |
| blockinfo / iteminfo / itembonus / bp | ❌ |
| log / explored / clearownership | ❌ |
| promote / demote / employ / fire / fireall / transfer | ❌ |
| give / editpoints / edititembonus / convert | ❌ |
| reload | 🟡 only boost-source reload; no full reload |
| area / entitylist / glog | ❌ |
| Signs (command + leaderboard) | ❌ no sign support |
| GUI | ✅ craftux GUIs (browse, info, stats, upgrade tree) |

## 4. Progression, leveling & limits

| JobsReborn feature | ModularJobs status |
|---|---|
| Higher level → higher income | ✅ `PayableCurve` per job |
| Customizable equations (max exp / exp gain / income) | ✅ `ExpressionCurves` (exp4j) |
| Leveling milestones / skill levels | ✅ `perk-unlocks` in jobs.yml + `PerkSyncService` |
| Leave/rejoin | 🟡 archive + intact restore; ❌ no configurable exp/level loss |
| Level-up sounds | ✅ `JobLevelUpListener` (sounds + title) |
| Level-up commands | ✅ config-driven `level-up-commands` (2026-08-18) |
| Max jobs limit (`jobs.max`) | ✅ `progression-limits.max-jobs` (2026-08-18) |
| Per-job join permission (`jobs.join.<job>`) | ✅ `jobs.join.<job>` checked (2026-08-18) |
| World join restrictions (`jobs.world.<world>`) | ✅ world join restriction via disabled-worlds (2026-08-18) |
| VIP max level | ❌ |
| Auto-join on login (`jobs.autojoin`) | ✅ `auto-join-jobs` (2026-08-18) |
| Spawner pay multiplier + vipspawner bypass | 🟡 spawner mobs blocked entirely; no multiplier |
| Pet pay (MyPet) | ❌ |
| Skill tree | ✅ upgrade/skill trees (JSON-driven, skill points) — richer than JobsReborn's numeric skill levels |

## 5. Integrations, API, placeholders, chat

| JobsReborn feature | ModularJobs status |
|---|---|
| MySQL | ✅ (MySQL 8 only — intentional; SQLite is a repo non-goal) |
| PlaceholderAPI | ✅ expansion registered + full `modular` placeholder set (2026-08-18) |
| McMMO | ✅ `McMMOBoostSourceImpl` |
| Mint economy | ✅ (intentional Vault replacement) |
| Bolt/LWC/Choco block protection | 🟡 Bolt adapter implemented; LWC/Choco soft-depends declared but no adapters |
| MythicMobs | ❌ |
| WorldGuard areas | ❌ |
| MyPet pet pay | ❌ |
| EcoEnchants | ❌ |
| Locale/translation files | ❌ `Messages` uses hardcoded themed tags; no language YAMLs |
| Chat integration (honorific/titles/chat colors) | ❌ jobs expose displayName/description only |
| API events | ✅ Join/Leave/Level/ExpGain/Payment/PrePayment (pure + Bukkit bridge); ❌ no ChunkChange/AreaSelection/Schedule events |
| Placeholders | ❌ expansion exposes only `%modular_experience_<job>%`; JobsReborn has ~40 `jobsr` placeholders |

## 6. Quests, ownership, anti-exploit, signs

| JobsReborn feature | ModularJobs status |
|---|---|
| Daily quests | ❌ none |
| Furnace/brewing-stand ownership + max counts | ❌ (generic placed-block ownership via Bolt only) |
| `clearownership` / `bp` / `blockinfo` | ❌ |
| Command signs + leaderboard signs + player heads | ❌ |
| Exploit protections | ✅ strong set: place→break anti-farm, silk-touch deny, ore-generator, piston, hopper, furnace proximity, mob damage contribution, wax/dye/milk/strip cooldowns, craft gating, chunk explore dedup, eligibility gates |

## Intentional non-goals (not gaps)
- SQLite / PostgreSQL / MariaDB storage — repo rule: **MySQL 8 only**
- In-process DDL / auto schema creation — repo rule: **connect-only schema ownership**
- Vault economy — replaced by **Mint** reflective adapter
- Guice/DI frameworks — manual composition root

## Prioritized gap candidates
1. **PlaceholderAPI expansion completeness** — richest user-facing win, pure Java, no schema
2. **Progression limits** — max jobs, per-job join permission, world join restrictions, auto-join
3. **Level-up commands** — config-driven command execution on level up
4. **Action parity** — `scrape`, dedicated `drink`
5. **Global top (`gtop`)** — scoreboard/chat global leaderboard
6. **Points economy** — new payable type + persistence + schema (larger)
7. **Daily quests** — new domain + schema (largest)
