package net.aincraft.service;

import java.util.List;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface PetUpgradeService {
    boolean hasUnlockedUpgrade(@NotNull UUID playerId, @NotNull String jobKey);
    void setSelectedPet(@NotNull UUID playerId, @NotNull String jobKey, @NotNull String petConfigName);
    @Nullable String getSelectedPet(@NotNull UUID playerId, @NotNull String jobKey);
    @NotNull List<String> getAvailablePets(@NotNull String jobKey);
    void registerJobPets(@NotNull String jobKey, @NotNull List<String> petConfigNames);
}
