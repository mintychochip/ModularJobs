package net.aincraft.upgrade;

/**
 * Policy for how multiple levels of the same perk are applied.
 */
public enum PerkPolicy {
  /**
   * Only the highest unlocked level applies.
   * Lower levels are ignored when higher levels are unlocked.
   * Example: efficiency_2 replaces efficiency_1
   */
  MAX,

  /**
   * All unlocked levels stack additively.
   * Each level contributes to the final effect.
   * Example: crit_chance_1 (10%) + crit_chance_2 (15%) = 25%
   */
  ADDITIVE
}
