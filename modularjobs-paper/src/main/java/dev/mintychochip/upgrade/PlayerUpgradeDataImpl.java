package dev.mintychochip.upgrade;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Implementation of PlayerUpgradeData. Mutable for use in services; immutable view exposed via
 * interface. Backed by an internal {@link SkillTreeState}; the legacy unlocked-node view is derived
 * from the state's node levels.
 */
public final class PlayerUpgradeDataImpl implements PlayerUpgradeData {

  private SkillTreeState state;
  private final Map<String, Integer> perkLevels;
  private final Map<String, Integer> maxLevels; // perkId -> max achievable level
  private @org.jetbrains.annotations.Nullable SkillTree skillTree;

  /** Legacy constructor: treats each unlocked node as level 1. */
  public PlayerUpgradeDataImpl(
      @NotNull String playerId,
      @NotNull String jobKey,
      int totalSkillPoints,
      @NotNull Set<String> unlockedNodes) {
    this(toState(playerId, jobKey, totalSkillPoints, unlockedNodes));
  }

  /** State-backed constructor (v2 format). */
  public PlayerUpgradeDataImpl(@NotNull SkillTreeState state) {
    this.state = state;
    this.perkLevels = new HashMap<>();
    this.maxLevels = new HashMap<>();
  }

  /** Bind the v2 tree so {@link #spentSkillPoints()} uses real level costs. */
  public void bindSkillTree(@org.jetbrains.annotations.Nullable SkillTree tree) {
    this.skillTree = tree;
  }

  private static SkillTreeState toState(
      @NotNull String playerId,
      @NotNull String jobKey,
      int totalSkillPoints,
      @NotNull Set<String> unlockedNodes) {
    Map<String, Integer> levels = new HashMap<>();
    for (String nodeKey : unlockedNodes) {
      levels.put(nodeKey, 1);
    }
    return new SkillTreeState(playerId, jobKey, totalSkillPoints, levels, Map.of());
  }

  /** Create empty upgrade data for a new player-job combination. */
  public static PlayerUpgradeDataImpl empty(@NotNull String playerId, @NotNull String jobKey) {
    return new PlayerUpgradeDataImpl(playerId, jobKey, 0, Set.of());
  }

  @Override
  public @NotNull String playerId() {
    return state.playerId();
  }

  @Override
  public @NotNull String jobKey() {
    return state.jobKey();
  }

  @Override
  public int totalSkillPoints() {
    return state.totalSkillPoints();
  }

  @Override
  public int availableSkillPoints() {
    return totalSkillPoints() - spentSkillPoints();
  }

  @Override
  public int spentSkillPoints() {
    if (skillTree != null) {
      return skillTree.spentPoints(state);
    }
    // Legacy path: one cost unit per unlocked node key when no tree is bound.
    return state.nodeLevels().values().stream().mapToInt(Integer::intValue).sum();
  }

  @Override
  public @NotNull SkillTreeState state() {
    return state;
  }

  @Override
  public @NotNull Map<String, Integer> nodeLevels() {
    return state.nodeLevels();
  }

  @Override
  public @NotNull Set<String> unlockedNodes() {
    return Collections.unmodifiableSet(state.nodeLevels().keySet());
  }

  @Override
  public @NotNull Map<String, Integer> perkLevels() {
    return Collections.unmodifiableMap(perkLevels);
  }

  @Override
  public boolean hasUnlocked(@NotNull String nodeKey) {
    return state.hasUnlocked(nodeKey);
  }

  // Mutators for service use

  /** Add skill points (e.g., on level up). */
  public void addSkillPoints(int points) {
    this.state =
        new SkillTreeState(
            state.playerId(),
            state.jobKey(),
            state.totalSkillPoints() + points,
            state.nodeLevels(),
            state.state(),
            state.currentJobLevel(),
            state.permissionCheck());
  }

  /** Set total skill points directly. */
  public void setTotalSkillPoints(int points) {
    this.state =
        new SkillTreeState(
            state.playerId(),
            state.jobKey(),
            points,
            state.nodeLevels(),
            state.state(),
            state.currentJobLevel(),
            state.permissionCheck());
  }

  /**
   * Unlock a node at level 1.
   *
   * @return true if newly unlocked, false if already unlocked
   */
  public boolean unlock(@NotNull String nodeKey) {
    if (state.hasUnlocked(nodeKey)) {
      return false;
    }
    Map<String, Integer> levels = new HashMap<>(state.nodeLevels());
    levels.put(nodeKey, 1);
    this.state =
        new SkillTreeState(
            state.playerId(),
            state.jobKey(),
            state.totalSkillPoints(),
            levels,
            state.state(),
            state.currentJobLevel(),
            state.permissionCheck());
    return true;
  }

  /**
   * Remove an unlocked node (for respec functionality).
   *
   * @return true if was unlocked, false if wasn't
   */
  public boolean lock(@NotNull String nodeKey) {
    if (!state.hasUnlocked(nodeKey)) {
      return false;
    }
    Map<String, Integer> levels = new HashMap<>(state.nodeLevels());
    levels.remove(nodeKey);
    this.state =
        new SkillTreeState(
            state.playerId(),
            state.jobKey(),
            state.totalSkillPoints(),
            levels,
            state.state(),
            state.currentJobLevel(),
            state.permissionCheck());
    return true;
  }

  /**
   * Set the level of a perk. Stores the max level.
   *
   * @param perkId perk identifier
   * @param level level to set
   */
  public void setPerkLevel(@NotNull String perkId, int level) {
    int current = perkLevels.getOrDefault(perkId, 0);
    if (level > current) {
      perkLevels.put(perkId, level);
    }
  }

  /**
   * Remove a perk level entry (for respec).
   *
   * @return the previous level, or 0 if not set
   */
  public int removePerkLevel(@NotNull String perkId) {
    Integer previous = perkLevels.remove(perkId);
    return previous != null ? previous : 0;
  }

  @Override
  public int getMaxLevel(@NotNull String perkId) {
    return maxLevels.getOrDefault(perkId, 1);
  }

  /**
   * Set the max level for a perk (from upgrade tree config).
   *
   * @param perkId perk identifier
   * @param maxLevel maximum achievable level
   */
  public void setMaxLevel(@NotNull String perkId, int maxLevel) {
    maxLevels.put(perkId, maxLevel);
  }
}
