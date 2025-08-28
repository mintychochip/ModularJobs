package net.aincraft.commands;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import net.aincraft.JobProgression;
import net.aincraft.commands.top.ChatJobsTopPageConsumerImpl;
import net.aincraft.service.JobService;
import net.aincraft.util.KeyFactory;
import net.kyori.adventure.key.Key;

/**
 * /jobs area add wg:worldGuardArea 2	jobs.area.add	Adds a new restricted area with 2 amount of
 * bonus and in the specific worldGuard area. /jobs area add 2.1	jobs.area.add	Adds a new restricted
 * area with 2.1 amount of bonus. /jobs area remove areaName	jobs.area.remove	Removes the given
 * restricted area. /jobs area info	-	Prints the current informations about area if the player
 * standing in one of them. /jobs area list	-	Lists all the available restricted areas by location.
 * <p>
 * /jobs edititembonus add itemName	Sets the item bonus NBT to what is in the player hand. /jobs
 * edititembonus remove	Removes the NBT from the item what the player is holding currently. /jobs
 * edititembonus list	Lists the item bonuses that the player is holding currently. Permissions:
 * jobs.command.expboost, jobs.command.moneyboost, jobs.command.pointboost
 * <p>
 * Name	Explanation /jobs expboost jobName 2.5	Adds 2.5 amount of experience boost to the specified
 * job. /jobs pointboost jobName 1hour5m30second 2.5	Adds 2.5 amount of point boost to the specified
 * job with the specified time. /jobs moneyboost all 5 10m	Sets the Global money bonus to 500% for
 * all the available jobs for 10 minutes. /jobs expboost reset jobName	Resets the experience boost
 * for the specified job. /jobs pointboost reset all	Resets the point boost for all jobs.
 * <p>
 * <p>
 * Experience, level Permissions: jobs.command.exp, jobs.command.level
 * <p>
 * Name	Explanation /jobs exp playerName jobName add 2.5	Adds 2.5 amount of experience to the given
 * player. /jobs level playerName jobName take 8	Takes 8 amount of level from the given player.
 * /jobs expplayerName jobName set 10	Sets the player exp to 10
 * <p>
 * /jobs edititembonus add itemName	Sets the item bonus NBT to what is in the player hand. /jobs
 * edititembonus remove	Removes the NBT from the item what the player is holding currently. /jobs
 * edititembonus list	Lists the item bonuses that the player is holding currently. Permissions:
 * jobs.command.expboost, jobs.command.moneyboost, jobs.command.pointboost
 * <p>
 * Name	Explanation /jobs expboost jobName 2.5	Adds 2.5 amount of experience boost to the specified
 * job. /jobs pointboost jobName 1hour5m30second 2.5	Adds 2.5 amount of point boost to the specified
 * job with the specified time. /jobs moneyboost all 5 10m	Sets the Global money bonus to 500% for
 * all the available jobs for 10 minutes. /jobs expboost reset jobName	Resets the experience boost
 * for the specified job. /jobs pointboost reset all	Resets the point boost for all jobs.
 * <p>
 * <p>
 * Experience, level Permissions: jobs.command.exp, jobs.command.level
 * <p>
 * Name	Explanation /jobs exp playerName jobName add 2.5	Adds 2.5 amount of experience to the given
 * player. /jobs level playerName jobName take 8	Takes 8 amount of level from the given player.
 * /jobs expplayerName jobName set 10	Sets the player exp to 10
 * <p>
 * /jobs edititembonus add itemName	Sets the item bonus NBT to what is in the player hand. /jobs
 * edititembonus remove	Removes the NBT from the item what the player is holding currently. /jobs
 * edititembonus list	Lists the item bonuses that the player is holding currently. Permissions:
 * jobs.command.expboost, jobs.command.moneyboost, jobs.command.pointboost
 * <p>
 * Name	Explanation /jobs expboost jobName 2.5	Adds 2.5 amount of experience boost to the specified
 * job. /jobs pointboost jobName 1hour5m30second 2.5	Adds 2.5 amount of point boost to the specified
 * job with the specified time. /jobs moneyboost all 5 10m	Sets the Global money bonus to 500% for
 * all the available jobs for 10 minutes. /jobs expboost reset jobName	Resets the experience boost
 * for the specified job. /jobs pointboost reset all	Resets the point boost for all jobs.
 * <p>
 * <p>
 * Experience, level Permissions: jobs.command.exp, jobs.command.level
 * <p>
 * Name	Explanation /jobs exp playerName jobName add 2.5	Adds 2.5 amount of experience to the given
 * player. /jobs level playerName jobName take 8	Takes 8 amount of level from the given player.
 * /jobs expplayerName jobName set 10	Sets the player exp to 10
 * <p>
 * /jobs edititembonus add itemName	Sets the item bonus NBT to what is in the player hand. /jobs
 * edititembonus remove	Removes the NBT from the item what the player is holding currently. /jobs
 * edititembonus list	Lists the item bonuses that the player is holding currently. Permissions:
 * jobs.command.expboost, jobs.command.moneyboost, jobs.command.pointboost
 * <p>
 * Name	Explanation /jobs expboost jobName 2.5	Adds 2.5 amount of experience boost to the specified
 * job. /jobs pointboost jobName 1hour5m30second 2.5	Adds 2.5 amount of point boost to the specified
 * job with the specified time. /jobs moneyboost all 5 10m	Sets the Global money bonus to 500% for
 * all the available jobs for 10 minutes. /jobs expboost reset jobName	Resets the experience boost
 * for the specified job. /jobs pointboost reset all	Resets the point boost for all jobs.
 * <p>
 * <p>
 * Experience, level Permissions: jobs.command.exp, jobs.command.level
 * <p>
 * Name	Explanation /jobs exp playerName jobName add 2.5	Adds 2.5 amount of experience to the given
 * player. /jobs level playerName jobName take 8	Takes 8 amount of level from the given player.
 * /jobs expplayerName jobName set 10	Sets the player exp to 10
 * <p>
 * /jobs edititembonus add itemName	Sets the item bonus NBT to what is in the player hand. /jobs
 * edititembonus remove	Removes the NBT from the item what the player is holding currently. /jobs
 * edititembonus list	Lists the item bonuses that the player is holding currently. Permissions:
 * jobs.command.expboost, jobs.command.moneyboost, jobs.command.pointboost
 * <p>
 * Name	Explanation /jobs expboost jobName 2.5	Adds 2.5 amount of experience boost to the specified
 * job. /jobs pointboost jobName 1hour5m30second 2.5	Adds 2.5 amount of point boost to the specified
 * job with the specified time. /jobs moneyboost all 5 10m	Sets the Global money bonus to 500% for
 * all the available jobs for 10 minutes. /jobs expboost reset jobName	Resets the experience boost
 * for the specified job. /jobs pointboost reset all	Resets the point boost for all jobs.
 * <p>
 * <p>
 * Experience, level Permissions: jobs.command.exp, jobs.command.level
 * <p>
 * Name	Explanation /jobs exp playerName jobName add 2.5	Adds 2.5 amount of experience to the given
 * player. /jobs level playerName jobName take 8	Takes 8 amount of level from the given player.
 * /jobs expplayerName jobName set 10	Sets the player exp to 10
 * <p>
 * /jobs edititembonus add itemName	Sets the item bonus NBT to what is in the player hand. /jobs
 * edititembonus remove	Removes the NBT from the item what the player is holding currently. /jobs
 * edititembonus list	Lists the item bonuses that the player is holding currently. Permissions:
 * jobs.command.expboost, jobs.command.moneyboost, jobs.command.pointboost
 * <p>
 * Name	Explanation /jobs expboost jobName 2.5	Adds 2.5 amount of experience boost to the specified
 * job. /jobs pointboost jobName 1hour5m30second 2.5	Adds 2.5 amount of point boost to the specified
 * job with the specified time. /jobs moneyboost all 5 10m	Sets the Global money bonus to 500% for
 * all the available jobs for 10 minutes. /jobs expboost reset jobName	Resets the experience boost
 * for the specified job. /jobs pointboost reset all	Resets the point boost for all jobs.
 * <p>
 * <p>
 * Experience, level Permissions: jobs.command.exp, jobs.command.level
 * <p>
 * Name	Explanation /jobs exp playerName jobName add 2.5	Adds 2.5 amount of experience to the given
 * player. /jobs level playerName jobName take 8	Takes 8 amount of level from the given player.
 * /jobs expplayerName jobName set 10	Sets the player exp to 10
 * <p>
 * /jobs edititembonus add itemName	Sets the item bonus NBT to what is in the player hand. /jobs
 * edititembonus remove	Removes the NBT from the item what the player is holding currently. /jobs
 * edititembonus list	Lists the item bonuses that the player is holding currently. Permissions:
 * jobs.command.expboost, jobs.command.moneyboost, jobs.command.pointboost
 * <p>
 * Name	Explanation /jobs expboost jobName 2.5	Adds 2.5 amount of experience boost to the specified
 * job. /jobs pointboost jobName 1hour5m30second 2.5	Adds 2.5 amount of point boost to the specified
 * job with the specified time. /jobs moneyboost all 5 10m	Sets the Global money bonus to 500% for
 * all the available jobs for 10 minutes. /jobs expboost reset jobName	Resets the experience boost
 * for the specified job. /jobs pointboost reset all	Resets the point boost for all jobs.
 * <p>
 * <p>
 * Experience, level Permissions: jobs.command.exp, jobs.command.level
 * <p>
 * Name	Explanation /jobs exp playerName jobName add 2.5	Adds 2.5 amount of experience to the given
 * player. /jobs level playerName jobName take 8	Takes 8 amount of level from the given player.
 * /jobs expplayerName jobName set 10	Sets the player exp to 10
 * <p>
 * /jobs edititembonus add itemName	Sets the item bonus NBT to what is in the player hand. /jobs
 * edititembonus remove	Removes the NBT from the item what the player is holding currently. /jobs
 * edititembonus list	Lists the item bonuses that the player is holding currently. Permissions:
 * jobs.command.expboost, jobs.command.moneyboost, jobs.command.pointboost
 * <p>
 * Name	Explanation /jobs expboost jobName 2.5	Adds 2.5 amount of experience boost to the specified
 * job. /jobs pointboost jobName 1hour5m30second 2.5	Adds 2.5 amount of point boost to the specified
 * job with the specified time. /jobs moneyboost all 5 10m	Sets the Global money bonus to 500% for
 * all the available jobs for 10 minutes. /jobs expboost reset jobName	Resets the experience boost
 * for the specified job. /jobs pointboost reset all	Resets the point boost for all jobs.
 * <p>
 * <p>
 * Experience, level Permissions: jobs.command.exp, jobs.command.level
 * <p>
 * Name	Explanation /jobs exp playerName jobName add 2.5	Adds 2.5 amount of experience to the given
 * player. /jobs level playerName jobName take 8	Takes 8 amount of level from the given player.
 * /jobs expplayerName jobName set 10	Sets the player exp to 10
 * <p>
 * /jobs edititembonus add itemName	Sets the item bonus NBT to what is in the player hand. /jobs
 * edititembonus remove	Removes the NBT from the item what the player is holding currently. /jobs
 * edititembonus list	Lists the item bonuses that the player is holding currently. Permissions:
 * jobs.command.expboost, jobs.command.moneyboost, jobs.command.pointboost
 * <p>
 * Name	Explanation /jobs expboost jobName 2.5	Adds 2.5 amount of experience boost to the specified
 * job. /jobs pointboost jobName 1hour5m30second 2.5	Adds 2.5 amount of point boost to the specified
 * job with the specified time. /jobs moneyboost all 5 10m	Sets the Global money bonus to 500% for
 * all the available jobs for 10 minutes. /jobs expboost reset jobName	Resets the experience boost
 * for the specified job. /jobs pointboost reset all	Resets the point boost for all jobs.
 * <p>
 * <p>
 * Experience, level Permissions: jobs.command.exp, jobs.command.level
 * <p>
 * Name	Explanation /jobs exp playerName jobName add 2.5	Adds 2.5 amount of experience to the given
 * player. /jobs level playerName jobName take 8	Takes 8 amount of level from the given player.
 * /jobs expplayerName jobName set 10	Sets the player exp to 10
 * <p>
 * /jobs edititembonus add itemName	Sets the item bonus NBT to what is in the player hand. /jobs
 * edititembonus remove	Removes the NBT from the item what the player is holding currently. /jobs
 * edititembonus list	Lists the item bonuses that the player is holding currently. Permissions:
 * jobs.command.expboost, jobs.command.moneyboost, jobs.command.pointboost
 * <p>
 * Name	Explanation /jobs expboost jobName 2.5	Adds 2.5 amount of experience boost to the specified
 * job. /jobs pointboost jobName 1hour5m30second 2.5	Adds 2.5 amount of point boost to the specified
 * job with the specified time. /jobs moneyboost all 5 10m	Sets the Global money bonus to 500% for
 * all the available jobs for 10 minutes. /jobs expboost reset jobName	Resets the experience boost
 * for the specified job. /jobs pointboost reset all	Resets the point boost for all jobs.
 * <p>
 * <p>
 * Experience, level Permissions: jobs.command.exp, jobs.command.level
 * <p>
 * Name	Explanation /jobs exp playerName jobName add 2.5	Adds 2.5 amount of experience to the given
 * player. /jobs level playerName jobName take 8	Takes 8 amount of level from the given player.
 * /jobs expplayerName jobName set 10	Sets the player exp to 10
 * <p>
 * /jobs edititembonus add itemName	Sets the item bonus NBT to what is in the player hand. /jobs
 * edititembonus remove	Removes the NBT from the item what the player is holding currently. /jobs
 * edititembonus list	Lists the item bonuses that the player is holding currently. Permissions:
 * jobs.command.expboost, jobs.command.moneyboost, jobs.command.pointboost
 * <p>
 * Name	Explanation /jobs expboost jobName 2.5	Adds 2.5 amount of experience boost to the specified
 * job. /jobs pointboost jobName 1hour5m30second 2.5	Adds 2.5 amount of point boost to the specified
 * job with the specified time. /jobs moneyboost all 5 10m	Sets the Global money bonus to 500% for
 * all the available jobs for 10 minutes. /jobs expboost reset jobName	Resets the experience boost
 * for the specified job. /jobs pointboost reset all	Resets the point boost for all jobs.
 * <p>
 * <p>
 * Experience, level Permissions: jobs.command.exp, jobs.command.level
 * <p>
 * Name	Explanation /jobs exp playerName jobName add 2.5	Adds 2.5 amount of experience to the given
 * player. /jobs level playerName jobName take 8	Takes 8 amount of level from the given player.
 * /jobs expplayerName jobName set 10	Sets the player exp to 10
 * <p>
 * /jobs edititembonus add itemName	Sets the item bonus NBT to what is in the player hand. /jobs
 * edititembonus remove	Removes the NBT from the item what the player is holding currently. /jobs
 * edititembonus list	Lists the item bonuses that the player is holding currently. Permissions:
 * jobs.command.expboost, jobs.command.moneyboost, jobs.command.pointboost
 * <p>
 * Name	Explanation /jobs expboost jobName 2.5	Adds 2.5 amount of experience boost to the specified
 * job. /jobs pointboost jobName 1hour5m30second 2.5	Adds 2.5 amount of point boost to the specified
 * job with the specified time. /jobs moneyboost all 5 10m	Sets the Global money bonus to 500% for
 * all the available jobs for 10 minutes. /jobs expboost reset jobName	Resets the experience boost
 * for the specified job. /jobs pointboost reset all	Resets the point boost for all jobs.
 * <p>
 * <p>
 * Experience, level Permissions: jobs.command.exp, jobs.command.level
 * <p>
 * Name	Explanation /jobs exp playerName jobName add 2.5	Adds 2.5 amount of experience to the given
 * player. /jobs level playerName jobName take 8	Takes 8 amount of level from the given player.
 * /jobs expplayerName jobName set 10	Sets the player exp to 10
 * <p>
 * /jobs edititembonus add itemName	Sets the item bonus NBT to what is in the player hand. /jobs
 * edititembonus remove	Removes the NBT from the item what the player is holding currently. /jobs
 * edititembonus list	Lists the item bonuses that the player is holding currently. Permissions:
 * jobs.command.expboost, jobs.command.moneyboost, jobs.command.pointboost
 * <p>
 * Name	Explanation /jobs expboost jobName 2.5	Adds 2.5 amount of experience boost to the specified
 * job. /jobs pointboost jobName 1hour5m30second 2.5	Adds 2.5 amount of point boost to the specified
 * job with the specified time. /jobs moneyboost all 5 10m	Sets the Global money bonus to 500% for
 * all the available jobs for 10 minutes. /jobs expboost reset jobName	Resets the experience boost
 * for the specified job. /jobs pointboost reset all	Resets the point boost for all jobs.
 * <p>
 * <p>
 * Experience, level Permissions: jobs.command.exp, jobs.command.level
 * <p>
 * Name	Explanation /jobs exp playerName jobName add 2.5	Adds 2.5 amount of experience to the given
 * player. /jobs level playerName jobName take 8	Takes 8 amount of level from the given player.
 * /jobs expplayerName jobName set 10	Sets the player exp to 10
 * <p>
 * /jobs edititembonus add itemName	Sets the item bonus NBT to what is in the player hand. /jobs
 * edititembonus remove	Removes the NBT from the item what the player is holding currently. /jobs
 * edititembonus list	Lists the item bonuses that the player is holding currently. Permissions:
 * jobs.command.expboost, jobs.command.moneyboost, jobs.command.pointboost
 * <p>
 * Name	Explanation /jobs expboost jobName 2.5	Adds 2.5 amount of experience boost to the specified
 * job. /jobs pointboost jobName 1hour5m30second 2.5	Adds 2.5 amount of point boost to the specified
 * job with the specified time. /jobs moneyboost all 5 10m	Sets the Global money bonus to 500% for
 * all the available jobs for 10 minutes. /jobs expboost reset jobName	Resets the experience boost
 * for the specified job. /jobs pointboost reset all	Resets the point boost for all jobs.
 * <p>
 * <p>
 * Experience, level Permissions: jobs.command.exp, jobs.command.level
 * <p>
 * Name	Explanation /jobs exp playerName jobName add 2.5	Adds 2.5 amount of experience to the given
 * player. /jobs level playerName jobName take 8	Takes 8 amount of level from the given player.
 * /jobs expplayerName jobName set 10	Sets the player exp to 10
 * <p>
 * /jobs edititembonus add itemName	Sets the item bonus NBT to what is in the player hand. /jobs
 * edititembonus remove	Removes the NBT from the item what the player is holding currently. /jobs
 * edititembonus list	Lists the item bonuses that the player is holding currently. Permissions:
 * jobs.command.expboost, jobs.command.moneyboost, jobs.command.pointboost
 * <p>
 * Name	Explanation /jobs expboost jobName 2.5	Adds 2.5 amount of experience boost to the specified
 * job. /jobs pointboost jobName 1hour5m30second 2.5	Adds 2.5 amount of point boost to the specified
 * job with the specified time. /jobs moneyboost all 5 10m	Sets the Global money bonus to 500% for
 * all the available jobs for 10 minutes. /jobs expboost reset jobName	Resets the experience boost
 * for the specified job. /jobs pointboost reset all	Resets the point boost for all jobs.
 * <p>
 * <p>
 * Experience, level Permissions: jobs.command.exp, jobs.command.level
 * <p>
 * Name	Explanation /jobs exp playerName jobName add 2.5	Adds 2.5 amount of experience to the given
 * player. /jobs level playerName jobName take 8	Takes 8 amount of level from the given player.
 * /jobs expplayerName jobName set 10	Sets the player exp to 10
 * <p>
 * /jobs edititembonus add itemName	Sets the item bonus NBT to what is in the player hand. /jobs
 * edititembonus remove	Removes the NBT from the item what the player is holding currently. /jobs
 * edititembonus list	Lists the item bonuses that the player is holding currently. Permissions:
 * jobs.command.expboost, jobs.command.moneyboost, jobs.command.pointboost
 * <p>
 * Name	Explanation /jobs expboost jobName 2.5	Adds 2.5 amount of experience boost to the specified
 * job. /jobs pointboost jobName 1hour5m30second 2.5	Adds 2.5 amount of point boost to the specified
 * job with the specified time. /jobs moneyboost all 5 10m	Sets the Global money bonus to 500% for
 * all the available jobs for 10 minutes. /jobs expboost reset jobName	Resets the experience boost
 * for the specified job. /jobs pointboost reset all	Resets the point boost for all jobs.
 * <p>
 * <p>
 * Experience, level Permissions: jobs.command.exp, jobs.command.level
 * <p>
 * Name	Explanation /jobs exp playerName jobName add 2.5	Adds 2.5 amount of experience to the given
 * player. /jobs level playerName jobName take 8	Takes 8 amount of level from the given player.
 * /jobs expplayerName jobName set 10	Sets the player exp to 10
 */

/**
 * /jobs edititembonus add itemName	Sets the item bonus NBT to what is in the player hand. /jobs
 * edititembonus remove	Removes the NBT from the item what the player is holding currently. /jobs
 * edititembonus list	Lists the item bonuses that the player is holding currently. Permissions:
 * jobs.command.expboost, jobs.command.moneyboost, jobs.command.pointboost
 * <p>
 * Name	Explanation /jobs expboost jobName 2.5	Adds 2.5 amount of experience boost to the specified
 * job. /jobs pointboost jobName 1hour5m30second 2.5	Adds 2.5 amount of point boost to the specified
 * job with the specified time. /jobs moneyboost all 5 10m	Sets the Global money bonus to 500% for
 * all the available jobs for 10 minutes. /jobs expboost reset jobName	Resets the experience boost
 * for the specified job. /jobs pointboost reset all	Resets the point boost for all jobs.
 */

/**
 * Experience, level Permissions: jobs.command.exp, jobs.command.level
 * <p>
 * Name	Explanation /jobs exp playerName jobName add 2.5	Adds 2.5 amount of experience to the given
 * player. /jobs level playerName jobName take 8	Takes 8 amount of level from the given player.
 * /jobs expplayerName jobName set 10	Sets the player exp to 10
 */

/**
 * Jobs Top/Info Chat/Scoreboard/Gui options
 */
final class TopCommand implements JobsCommand {

  private static final int ENTRIES_PER_QUERY = 100;

  private final JobService jobService;
  private final JobsTopPageProvider resultProvider;
  private final KeyFactory keyFactory;

  private static final int PAGE_SIZE = 10;

  @Inject
  public TopCommand(JobService jobService, JobsTopPageProvider resultProvider,
      KeyFactory keyFactory) {
    this.jobService = jobService;
    this.resultProvider = resultProvider;
    this.keyFactory = keyFactory;
  }

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return Commands.literal("top")
        .then(Commands.argument("job", StringArgumentType.string()).suggests((context, builder) -> {
          jobService.getJobs().stream().map(job -> job.getPlainName().toLowerCase(Locale.ENGLISH))
              .forEach(builder::suggest);
          return builder.buildFuture();
        }).then(Commands.argument("pageNumber", IntegerArgumentType.integer()).executes(context -> {
          String jobKey = context.getArgument("job", String.class);
          int page = context.getArgument("pageNumber", Integer.class);
          Key key = keyFactory.create(jobKey);
          //TODO: add limits
          ChatJobsTopPageConsumerImpl consumer = new ChatJobsTopPageConsumerImpl();
          consumer.consume(resultProvider.getPage(key, page, PAGE_SIZE),
              context.getSource().getSender());
          return 1;
        })));
  }

  static final class JobTopPageProviderImpl implements JobTopPageProvider {

    private final JobService jobService;
    private final Cache<Key, List<JobProgression>> readCache = Caffeine.newBuilder()
        .expireAfterWrite(Duration.ofMinutes(10)).build();

    JobTopPageProviderImpl(JobService jobService) {
      this.jobService = jobService;
    }

    @Override
    public Page<JobProgression> getPage(Key jobKey, int pageNumber, int pageSize) {
      List<JobProgression> progressions = readCache.get(jobKey,
          __ -> jobService.getProgressions(jobKey, ENTRIES_PER_QUERY));
      if (progressions == null) {
        return new Page<>(List.of(), -1);
      }
      int size = progressions.size();
      int maxPages = Math.max(1, (size + pageSize - 1) / pageSize);
      int clamped = Math.min(Math.max(pageNumber, 1), maxPages);
      int from = (clamped - 1) * pageSize;
      int to = Math.min(from + pageSize, size);
      List<JobProgression> slice = from < to ? progressions.subList(from, to) : List.of();
      return new Page<>(slice, pageSize);
    }
  }
}
