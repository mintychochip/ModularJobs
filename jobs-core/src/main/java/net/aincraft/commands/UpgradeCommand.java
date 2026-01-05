package net.aincraft.commands;

import com.google.inject.Inject;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import java.util.List;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.gui.PetSelectionGui;
import net.aincraft.service.JobService;
import net.aincraft.service.PetUpgradeService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
                        sender.sendMessage("This command can only be used by players");
                        return Command.SINGLE_SUCCESS;
                    }

                    String jobName = context.getArgument("job", String.class);
                    NamespacedKey jobKey = new NamespacedKey("modularjobs", jobName);
                    
                    // Get the job
                    Job job;
                    try {
                        job = jobService.getJob(jobKey.toString());
                    } catch (IllegalArgumentException e) {
                        player.sendMessage(Component.text("Invalid job: " + jobName, NamedTextColor.RED));
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
                        player.sendMessage(Component.text()
                            .append(Component.text("You haven't joined the ", NamedTextColor.RED))
                            .append(job.displayName())
                            .append(Component.text(" job!", NamedTextColor.RED))
                            .build());
                        return Command.SINGLE_SUCCESS;
                    }

                    // Check player has reached upgrade level
                    int currentLevel = playerProgression.level();
                    int upgradeLevel = job.upgradeLevel();
                    if (currentLevel < upgradeLevel) {
                        player.sendMessage(Component.text()
                            .append(Component.text("You need to reach level ", NamedTextColor.RED))
                            .append(Component.text(String.valueOf(upgradeLevel), NamedTextColor.YELLOW))
                            .append(Component.text(" to upgrade. Current: ", NamedTextColor.RED))
                            .append(Component.text(String.valueOf(currentLevel), NamedTextColor.YELLOW))
                            .build());
                        return Command.SINGLE_SUCCESS;
                    }

                    // Check if available pets exist for this job
                    List<String> availablePets = petUpgradeService.getAvailablePets(jobKey.toString());
                    if (availablePets.isEmpty()) {
                        player.sendMessage(Component.text("No pet upgrades available for this job.", NamedTextColor.RED));
                        return Command.SINGLE_SUCCESS;
                    }

                    // Check if player already has a pet for this job
                    String existingPet = petUpgradeService.getSelectedPet(player.getUniqueId(), jobKey.toString());
                    if (existingPet != null) {
                        player.sendMessage(Component.text()
                            .append(Component.text("You already upgraded to: ", NamedTextColor.YELLOW))
                            .append(Component.text(formatPetName(existingPet), NamedTextColor.GREEN))
                            .build());
                        player.sendMessage(Component.text("Job upgrades are permanent.", NamedTextColor.GRAY));
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
