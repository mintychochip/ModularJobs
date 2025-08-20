package net.aincraft.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;

/**
 * /jobs area add wg:worldGuardArea 2	jobs.area.add	Adds a new restricted area with 2 amount of bonus and in the specific worldGuard area.
 * /jobs area add 2.1	jobs.area.add	Adds a new restricted area with 2.1 amount of bonus.
 * /jobs area remove areaName	jobs.area.remove	Removes the given restricted area.
 * /jobs area info	-	Prints the current informations about area if the player standing in one of them.
 * /jobs area list	-	Lists all the available restricted areas by location.
 */

/**
 * /jobs edititembonus add itemName	Sets the item bonus NBT to what is in the player hand.
 * /jobs edititembonus remove	Removes the NBT from the item what the player is holding currently.
 * /jobs edititembonus list	Lists the item bonuses that the player is holding currently.
 *Permissions: jobs.command.expboost, jobs.command.moneyboost, jobs.command.pointboost
 *
 * Name	Explanation
 * /jobs expboost jobName 2.5	Adds 2.5 amount of experience boost to the specified job.
 * /jobs pointboost jobName 1hour5m30second 2.5	Adds 2.5 amount of point boost to the specified job with the specified time.
 * /jobs moneyboost all 5 10m	Sets the Global money bonus to 500% for all the available jobs for 10 minutes.
 * /jobs expboost reset jobName	Resets the experience boost for the specified job.
 * /jobs pointboost reset all	Resets the point boost for all jobs.
 *
 */

/**
 * Experience, level
 * Permissions: jobs.command.exp, jobs.command.level
 *
 * Name	Explanation
 * /jobs exp playerName jobName add 2.5	Adds 2.5 amount of experience to the given player.
 * /jobs level playerName jobName take 8	Takes 8 amount of level from the given player.
 * /jobs expplayerName jobName set 10	Sets the player exp to 10
 */

/**
 * Jobs Top/Info Chat/Scoreboard/Gui options
 * 
 */
public class TopCommand implements JobsCommand {

  @Override
  public LiteralArgumentBuilder<CommandSourceStack> build() {
    return null;
  }
}
