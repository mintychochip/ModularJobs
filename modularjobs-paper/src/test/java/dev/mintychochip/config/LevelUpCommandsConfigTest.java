package dev.mintychochip.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class LevelUpCommandsConfigTest {

  @Test
  void parsesCommandsWithPlaceholders() {
    LevelUpCommandsConfig config =
        LevelUpCommandsConfig.fromMap(
            java.util.Map.of(
                "level-up-commands",
                List.of(
                    java.util.Map.of(
                        "command", "say {player} reached {level} in {job}", "min-level", 5))));
    List<LevelUpCommandsConfig.LevelUpCommand> commands = config.commands();
    assertEquals(1, commands.size());
    assertEquals("say {player} reached {level} in {job}", commands.get(0).command());
    assertEquals(5, commands.get(0).minLevel());
  }

  @Test
  void defaultsToEmptyWhenAbsent() {
    LevelUpCommandsConfig config = LevelUpCommandsConfig.fromMap(java.util.Map.of());
    assertTrue(config.commands().isEmpty());
  }

  @Test
  void ignoresBlankCommandsAndClampsMinLevel() {
    LevelUpCommandsConfig config =
        LevelUpCommandsConfig.fromMap(
            java.util.Map.of(
                "level-up-commands",
                List.of(
                    java.util.Map.of("command", "  ", "min-level", 3),
                    java.util.Map.of("command", "say hi", "min-level", -2))));
    List<LevelUpCommandsConfig.LevelUpCommand> commands = config.commands();
    assertEquals(1, commands.size());
    assertEquals("say hi", commands.get(0).command());
    assertEquals(1, commands.get(0).minLevel());
  }
}
