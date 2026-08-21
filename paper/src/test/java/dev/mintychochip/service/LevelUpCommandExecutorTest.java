package dev.mintychochip.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import dev.mintychochip.config.LevelUpCommandsConfig;
import dev.mintychochip.config.LevelUpCommandsConfig.LevelUpCommand;
import org.junit.jupiter.api.Test;

class LevelUpCommandExecutorTest {

  @Test
  void substitutesPlaceholdersAndFiltersByMinLevel() {
    RecordingExecutor executor = new RecordingExecutor();
    LevelUpCommandExecutor service = new LevelUpCommandExecutor(
        new LevelUpCommandsConfig(List.of(
            new LevelUpCommand("say {player} hit {level} in {job}", 1),
            new LevelUpCommand("say too-early", 50))),
        executor::dispatch);
    service.execute("Steve", "Miner", 10);
    assertEquals(List.of("say Steve hit 10 in Miner"), executor.commands);
  }

  @Test
  void runsAllCommandsWhenMinLevelSatisfied() {
    RecordingExecutor executor = new RecordingExecutor();
    LevelUpCommandExecutor service = new LevelUpCommandExecutor(
        new LevelUpCommandsConfig(List.of(
            new LevelUpCommand("say a {player}", 1),
            new LevelUpCommand("say b {level}", 1))),
        executor::dispatch);
    service.execute("Steve", "Miner", 10);
    assertEquals(List.of("say a Steve", "say b 10"), executor.commands);
  }

  private static final class RecordingExecutor {
    final List<String> commands = new java.util.ArrayList<>();

    void dispatch(String command) {
      commands.add(command);
    }
  }
}
