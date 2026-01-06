package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import dev.mintychochip.mint.Mint;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.gui.PetSelectionGui;
import net.aincraft.service.JobService;
import net.aincraft.service.PetUpgradeService;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

final class UpgradeCommand implements JobsCommand {

    private final JobService jobService;
    private final PetUpgradeService petUpgradeService;
    private final PetSelectionGui petSelectionGui;

    @Inject
    public UpgradeCommand(JobService jobService, PetUpgradeService petUpgradeService, PetSelectionGui petSelectionGui) {
        this.jobService = jobService;
        this.petUpgradeService = petUpgradeService;
        this.petSelectionGui = petSelectionGui;
    }

    @Override
    public LiteralArgumentBuilder<CommandSourceStack> build() {
        return Commands.literal("upgrade")
            .then(Commands.argument("job", StringArgumentType.string())
                .suggests((context, builder) -> {
                    // Suggest only jobs the player has joined
                    CommandSender sender = context.getSource().getSender();
                    if (sender instanceof Player player) {
                        jobService.getProgressions(player).stream()
                            .map(p -> p.job().key().value())
                            .forEach(builder::suggest);
                    }
                    return builder.buildFuture();
                })
                .executes(context -> {
                    CommandSourceStack source = context.getSource();
                    CommandSender sender = source.getSender();
                    if (!(sender instanceof Player player)) {
                        Mint.sendMessage(sender, "<error>This command can only be used by players");
                        return Command.SINGLE_SUCCESS;
                    }

                    String jobName = context.getArgument("job", String.class);
                    NamespacedKey jobKey = new NamespacedKey("modularjobs", jobName);
                    
                    // Get the job
                    Job job;
                    try {
                        job = jobService.getJob(jobKey.toString());
                    } catch (IllegalArgumentException e) {
                        Mint.sendMessage(player, "<error>Invalid job: " + jobName);
                        return Command.SINGLE_SUCCESS;
                    }

                    // Check player has joined this job
                    List<JobProgression> progressions = jobService.getProgressions(player);
                    JobProgression playerProgression = null;
                    for (JobProgression prog : progressions) {
                        if (prog.job().key().toString().equals(jobKey.toString())) {
                            playerProgression = prog;
                            break;
                        }
                    }

                    if (playerProgression == null) {
                        Mint.sendMessage(player, "<error>You haven't joined the " + job.displayName().toString() + " job!");
                        return Command.SINGLE_SUCCESS;
                    }

                    // Check player has reached upgrade level
                    int currentLevel = playerProgression.level();
                    int upgradeLevel = job.upgradeLevel();
                    if (currentLevel < upgradeLevel) {
                        Mint.sendMessage(player, "<error>You need to reach level <secondary>" + upgradeLevel
                            + "<error> to upgrade. Current: <secondary>" + currentLevel);
                        return Command.SINGLE_SUCCESS;
                    }

                    // Check if available pets exist for this job
                    List<String> availablePets = petUpgradeService.getAvailablePets(jobKey.toString());
                    if (availablePets.isEmpty()) {
                        Mint.sendMessage(player, "<error>No pet upgrades available for this job.");
                        return Command.SINGLE_SUCCESS;
                    }

                    // Check if player already has a pet for this job
                    String existingPet = petUpgradeService.getSelectedPet(player.getUniqueId(), jobKey.toString());
                    if (existingPet != null) {
                        Mint.sendMessage(player, "<info>You already upgraded to: <success>" + formatPetName(existingPet));
                        Mint.sendMessage(player, "<neutral>Job upgrades are permanent.");
                        return Command.SINGLE_SUCCESS;
                    }

                    // Open the pet selection GUI
                    petSelectionGui.open(player, job);
                    return Command.SINGLE_SUCCESS;
                }));
    }

    private String formatPetName(String configName) {
        String[] parts = configName.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!result.isEmpty()) result.append(" ");
            result.append(part.substring(0, 1).toUpperCase()).append(part.substring(1).toLowerCase());
        }
        return result.toString();
    }
}
