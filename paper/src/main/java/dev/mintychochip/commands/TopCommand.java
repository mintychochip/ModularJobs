package dev.mintychochip.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.util.Messages;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.Locale;
import dev.mintychochip.commands.top.ChatJobsTopPageConsumerImpl;
import dev.mintychochip.commands.top.ScoreboardJobsTopPageConsumerImpl;
import dev.mintychochip.gui.craftux.CraftuxSurfaces;
import dev.mintychochip.service.JobService;
import dev.mintychochip.util.KeyUtils;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.plugin.Plugin;

/**
 * {@code /jobs top} command: displays paginated job leaderboards in chat or scoreboard mode.
 *
 * <p>Supports {@code /jobs top mode chat|scoreboard} to persist a player's display preference.
 */
public final class TopCommand implements JobsCommand {

  private static final int ENTRIES_PER_QUERY = 100;

  private final JobService jobService;
  private final JobTopPageProvider resultProvider;
  private final Plugin plugin;
  private final CraftuxSurfaces surfaces;

  private static final int PAGE_SIZE = 10;

  public TopCommand(JobService jobService, JobTopPageProvider resultProvider,
      Plugin plugin, CraftuxSurfaces surfaces) {
    this.jobService = jobService;
    this.resultProvider = resultProvider;
    this.plugin = plugin;
    this.surfaces = surfaces;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("top")
        .executes(context -> {
          CommandSourceStack source = context.getSource();
          CommandSender sender = source.getSender();
          displayOverallLeaderboard(sender);
          return 1;
        })
        .then(Commands.literal("mode")
            .then(Commands.argument("displayMode", StringArgumentType.string())
                .suggests((context, builder) -> {
                  builder.suggest("chat");
                  builder.suggest("scoreboard");
                  return builder.buildFuture();
                })
                .executes(context -> {
                  CommandSourceStack source = context.getSource();
                  CommandSender sender = source.getSender();
                  if (!(sender instanceof Player player)) {
                    Messages.send(sender, "<error>This command can only be used by players.");
                    return 0;
                  }
                  String mode = context.getArgument("displayMode", String.class);
                  if (!mode.equalsIgnoreCase("chat") && !mode.equalsIgnoreCase("scoreboard")) {
                    Messages.send(sender, "<error>Invalid display mode. Use 'chat' or 'scoreboard'.");
                    return 0;
                  }
                  player.setMetadata("jobs_display_mode", new FixedMetadataValue(plugin, mode.toLowerCase()));
                  Messages.send(sender, "<success>Display mode set to <secondary>" + mode.toLowerCase());
                  return 1;
                })))
        .then(Commands.argument("job", StringArgumentType.string()).suggests((context, builder) -> {
          jobService.getJobs().stream().map(job -> job.getPlainName().toLowerCase(Locale.ENGLISH))
              .forEach(builder::suggest);
          return builder.buildFuture();
        })
            .then(Commands.argument("pageNumber", IntegerArgumentType.integer()).executes(context -> {
              CommandSourceStack source = context.getSource();
              CommandSender sender = source.getSender();
              String jobKey = context.getArgument("job", String.class);
              int page = context.getArgument("pageNumber", Integer.class);
              Key key = KeyUtils.parseKey(plugin, jobKey);
              if (sender instanceof Player player) {
                String displayMode = getPlayerDisplayMode(player);
                if ("scoreboard".equalsIgnoreCase(displayMode)) {
                  TextScoreboard scoreboard = TextScoreboard.create(
                      surfaces, Component.text("Job Top - " + jobKey));
                  ScoreboardJobsTopPageConsumerImpl consumer = new ScoreboardJobsTopPageConsumerImpl(scoreboard);
                  int maxPages = Math.max(1, (ENTRIES_PER_QUERY + PAGE_SIZE - 1) / PAGE_SIZE);
                  consumer.consume(Component.text(jobKey), resultProvider.getPage(key, page, PAGE_SIZE),
                      sender, maxPages, resultProvider.getAllEntries(key));
                  return 1;
                }
              }
              ChatJobsTopPageConsumerImpl consumer = new ChatJobsTopPageConsumerImpl();
              int maxPages = Math.max(1, (ENTRIES_PER_QUERY + PAGE_SIZE - 1) / PAGE_SIZE);
              consumer.consume(Component.text(jobKey), resultProvider.getPage(key, page, PAGE_SIZE),
                  sender, maxPages, resultProvider.getAllEntries(key));
              return 1;
            }))
            .executes(context -> {
              CommandSourceStack source = context.getSource();
              CommandSender sender = source.getSender();
              String jobKey = context.getArgument("job", String.class);
              int page = 1;
              Key key = KeyUtils.parseKey(plugin, jobKey);
              if (sender instanceof Player player) {
                String displayMode = getPlayerDisplayMode(player);
                if ("scoreboard".equalsIgnoreCase(displayMode)) {
                  TextScoreboard scoreboard = TextScoreboard.create(
                      surfaces, Component.text("Job Top - " + jobKey));
                  ScoreboardJobsTopPageConsumerImpl consumer = new ScoreboardJobsTopPageConsumerImpl(scoreboard);
                  int maxPages = Math.max(1, (ENTRIES_PER_QUERY + PAGE_SIZE - 1) / PAGE_SIZE);
                  consumer.consume(Component.text(jobKey), resultProvider.getPage(key, page, PAGE_SIZE),
                      sender, maxPages, resultProvider.getAllEntries(key));
                  return 1;
                }
              }
              ChatJobsTopPageConsumerImpl consumer = new ChatJobsTopPageConsumerImpl();
              int maxPages = Math.max(1, (ENTRIES_PER_QUERY + PAGE_SIZE - 1) / PAGE_SIZE);
              consumer.consume(Component.text(jobKey), resultProvider.getPage(key, page, PAGE_SIZE),
                  sender, maxPages, resultProvider.getAllEntries(key));
              return 1;
            }));
  }

  private void displayOverallLeaderboard(CommandSender sender) {
    Messages.send(sender,
        "<error>Overall leaderboard is not yet implemented. Please specify a job: <secondary>/jobs top <job>");
  }

  private String getPlayerDisplayMode(Player player) {
    return player.hasMetadata("jobs_display_mode")
        ? player.getMetadata("jobs_display_mode").get(0).asString()
        : "chat";
  }
}
