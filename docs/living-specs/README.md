# Living specs — ModularJobs

Domain catalogs agents read before designing or implementing, and update when
intent or progress changes. Checkboxes in these files are the persistence layer.

## Catalogs

| Domain | File | Owns |
|--------|------|------|
| **Product / platform** | [modularjobs.md](./modularjobs.md) | Module seams, wiring, quality bar, cross-cutting roadmap |
| **Persistence** | [persistence.md](./persistence.md) | MySQL 8-only, connect-only schema, repositories |
| **Jobs progression** | [jobs-progression.md](./jobs-progression.md) | Jobs, tasks, actions, payment eligibility, XP |
| **Payables & economy** | [payables-economy.md](./payables-economy.md) | Payable awards, Mint ledger bridge |
| **Boosts** | [boosts.md](./boosts.md) | Timed/item boosts, boost engine |
| **Skill tree** | [skill-tree.md](./skill-tree.md) | JSON-driven job upgrade graphs |
| **Secure sessions** | [secure-sessions.md](./secure-sessions.md) | REST API, React editor, Paper export/apply |
| **Professions** | [professions.md](./professions.md) | Profession catalog, block-break gates, Bukkit services |

One-shot design dumps live under `docs/superpowers/specs/` and `docs/superpowers/plans/`.
Those are historical; **horizons and checkboxes here are authoritative for “what’s next”.**

## Agent protocol

1. Open the domain living spec **before** coding in that domain.
2. Map work to **Current** (or promote from Next/Future with agreement).
3. Follow **Invariants** + **Implementation guidance**.
4. Flip checkboxes in the file when verified; update `Last updated`.
5. Do not implement **Future** items without promoting them first.

Location convention: `docs/living-specs/<domain-slug>.md`.
