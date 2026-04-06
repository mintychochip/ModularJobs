# ModularJobs

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk&color=437291" alt="Java 21">
  <img src="https://img.shields.io/badge/Kotlin-1.9-purple?style=flat-square&logo=kotlin&color=7F52FF" alt="Kotlin">
  <img src="https://img.shields.io/badge/Minecraft-Paper-brightgreen?style=flat-square" alt="Paper">
  <img src="https://img.shields.io/badge/Gradle-Kotlin-blue?style=flat-square&logo=gradle&color=02303A" alt="Gradle">
</p>

<p align="center">
  <strong>Modular Job System for Minecraft Paper Servers</strong>
</p>

<p align="center">
  A flexible, extensible jobs plugin with skill trees, upgrade systems, and RPG-style progression.
</p>

---

## Features

- **Modular Architecture** — Cleanly separated API and Core modules
- **Job Progression** — Level up through experience and unlock rewards
- **Skill Trees** — Wynncraft-inspired upgrade trees with ability unlocking
- **Job Actions** — Trigger rewards through mining, crafting, combat, and more
- **Persistent Storage** — Player progress saved and loaded automatically
- **Pet Upgrades** — Companion system with upgrade paths
- **Exploit Protection** — Built-in protection against grinding exploits
- **Plugin Integration** — Compatible with PlaceholderAPI and other popular plugins

---

## Tech Stack

| Component | Technology |
|-----------|------------|
| **Language** | Java 21 + Kotlin 1.9 |
| **Build Tool** | Gradle (Kotlin DSL) |
| **Platform** | Paper 1.21+ |
| **Dependencies** | Adventure (text), Incendo Cloud (commands) |
| **Integrations** | PlaceholderAPI |

---

## Modules

```
ModularJobs/
├── jobs-api/          # Public API for external plugins
│   ├── JobService     # Core job management interface
│   ├── JobProgression # Player progression tracking
│   ├── JobTask        # Action-reward definitions
│   └── UpgradeTree    # Skill tree system
└── jobs-core/         # Implementation
    ├── Persistence    # Data storage
    ├── Actions        # Event handling
    └── Commands       # Player commands
```

---

## Installation

### Requirements

- Minecraft Paper Server 1.21+
- Java 21 or higher

### Setup

```bash
# Build from source
./gradlew build

# Copy jar to plugins folder
cp jobs-core/build/libs/*.jar /path/to/server/plugins/
```

### Maven Repository Access

The project uses multiple Maven repositories:
- PaperMC (Minecraft server API)
- JitPack (GitHub packages)
- CodeMC (Minecraft plugins)
- Incendo (Cloud command framework)

---

## Key Concepts

### Jobs
A `Job` represents a profession (e.g., Miner, Blacksmith, Hunter). Each job has:
- Unique key identifier
- Display name and description
- Associated tasks and rewards
- Progression levels

### JobProgression
Tracks a player's advancement in a specific job:
- Current level and experience
- Unlocked upgrades
- Archived/completed jobs

### JobTask
Defines actions that grant job experience:
- ActionType (BREAK, PLACE, CRAFT, KILL, etc.)
- Context (item, entity, location filters)
- Experience reward amount

### UpgradeTree
Wynncraft-inspired skill tree system:
- Position-based layout
- Ability metadata
- Connector paths
- Icon configurations

---

## API Usage

```java
// Get the job service
JobService jobService = Bridge.getJobService();

// List all available jobs
List<Job> jobs = jobService.getJobs();

// Get a specific job
Job miner = jobService.getJob("miner");

// Get player's job progression
List<JobProgression> progressions = jobService.getProgressions(player);

// Make player join a job
jobService.joinJob(player.getUniqueId().toString(), "blacksmith");
```

---

## Project Structure

```
jobs-api/src/main/java/net/aincraft/
├── Bridge.java                 # Plugin bridge for service access
├── Job.java                    # Job definition
├── JobProgression.java         # Player job state
├── JobTask.java                # Task definition
├── container/
│   ├── ActionType.java         # Supported action types
│   └── Context.java            # Task context/filters
├── service/
│   ├── JobService.java         # Main service interface
│   ├── PreferencesService.java # Player preferences
│   ├── PetUpgradeService.java  # Pet/companion system
│   └── ExploitProtectionStore.java # Anti-cheat
└── upgrade/
    ├── UpgradeTree.java        # Skill tree structure
    ├── UpgradeEffect.java      # Upgrade bonuses
    └── wynncraft/              # Wynncraft-compatible format
        ├── AbilityMeta.java
        ├── Archetype.java
        └── LayoutItem.java
```

---

## Contributing

Contributions are welcome! This project uses:
- Java 21 features (records, pattern matching, virtual threads where applicable)
- Kotlin for DSL-style configurations
- Gradle for build automation

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes
4. Push to the branch
5. Open a Pull Request

---

## License

This project is available under the MIT License.

---

<p align="center">
  Built for Minecraft server communities
</p>
