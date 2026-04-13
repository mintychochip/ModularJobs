package net.aincraft.upgrade.tree;

import java.util.List;
import java.util.Map;
import net.aincraft.upgrade.PerkPolicy;
import net.aincraft.upgrade.Position;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration for a skill tree.
 * This format supports archetypes, connectors, and explicit positioning.
 *
 * @param treeId      unique identifier for this tree
 * @param displayName human-readable name
 * @param description optional description of the tree
 * @param job         the job this tree belongs to (e.g., "miner")
 * @param skillPointsPerLevel skill points earned per level
 * @param root        the root node ID
 * @param paths       list of path coordinates (walkable connections between nodes)
 * @param archetypes  list of archetypes (thematic groupings)
 * @param layout      list of all layout items (abilities and connectors)
 * @param perkPolicies optional map of perkId -> policy (max/additive) for how perk levels stack
 */
public record TreeConfig(
    @NotNull String treeId,
    @NotNull String displayName,
    @Nullable String description,
    @NotNull String job,
    int skillPointsPerLevel,
    @NotNull String root,
    @NotNull List<Position> paths,
    @NotNull List<Archetype> archetypes,
    @NotNull List<LayoutItem> layout,
    @Nullable Map<String, PerkPolicy> perkPolicies
) {
}
