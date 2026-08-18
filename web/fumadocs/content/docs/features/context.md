---
title: Task context keys
description: How ModularJobs identifies action targets in task definitions.
---

Task contexts identify the Minecraft object or state associated with an action.
They are serialized as namespaced keys so YAML, JSON, CSV, and editor payloads
can use the same task contract.

## Examples

| Context | Example key |
| --- | --- |
| Block material | `minecraft:stone` |
| Entity type | `minecraft:zombie` |
| Item material | `minecraft:diamond` |
| World | `minecraft:the_nether` |
| Biome | `minecraft:plains` |

Use the action type and context keys accepted by the current Paper release when
adding task records. Unknown keys are rejected or ignored according to the
loader for that action type.