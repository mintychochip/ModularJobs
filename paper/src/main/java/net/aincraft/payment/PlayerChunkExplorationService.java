package net.aincraft.payment;

import com.google.gson.Gson;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import org.bukkit.Chunk;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.persistence.PersistentDataAdapterContext;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

/**
 * Tracks which players have explored which chunks, persisted as JSON in each chunk's
 * {@link PersistentDataContainer}. The empty set is represented by deleting the tag, so
 * unexplored chunks carry no data.
 */
public final class PlayerChunkExplorationService {

  private static final NamespacedKey CHUNK_KEY = new NamespacedKey("jobs", "chunk");

  private static final PersistentDataType<String, PersistentChunkData> DATA_TYPE = new PersistentChunkDataType();

  /**
   * Reports whether this player has explored {@code chunk}.
   *
   * @return true when this player's UUID is recorded as having explored {@code chunk}
   */
  public boolean hasExplored(OfflinePlayer player, Chunk chunk) {
    PersistentChunkData data = chunk.getPersistentDataContainer().get(CHUNK_KEY, DATA_TYPE);
    return data != null && data.getPlayers().contains(player.getUniqueId());
  }

  /** Records that {@code player} explored {@code chunk}. */
  public void addExploration(OfflinePlayer player, Chunk chunk) {
    updateData(chunk, data -> data.getPlayers().add(player.getUniqueId()));
  }

  /** Forgets that {@code player} explored {@code chunk}, removing the tag when no one remains. */
  public void removeExploration(OfflinePlayer player, Chunk chunk) {
    updateData(chunk, data -> data.getPlayers().remove(player.getUniqueId()));
  }

  private void updateData(Chunk chunk, Function<PersistentChunkData, Boolean> mutator) {
    PersistentDataContainer pdc = chunk.getPersistentDataContainer();
    PersistentChunkData data = pdc.get(CHUNK_KEY, DATA_TYPE);

    if (data == null) {
      data = new PersistentChunkData();
    }

    if (!mutator.apply(data)) {
      return;
    }

    if (data.getPlayers().isEmpty()) {
      pdc.remove(CHUNK_KEY);
    } else {
      pdc.set(CHUNK_KEY, DATA_TYPE, data);
    }
  }

  private static final class PersistentChunkData {

    private final Set<UUID> players = new HashSet<>();

    Set<UUID> getPlayers() {
      return players;
    }

    @Override
    public String toString() {
      return "PersistentChunkData[players=" + players + "]";
    }
  }

  private static final class PersistentChunkDataType implements
      PersistentDataType<String, PersistentChunkData> {

    private static final Gson GSON = new Gson();

    @Override
    public @NotNull Class<String> getPrimitiveType() {
      return String.class;
    }

    @Override
    public @NotNull Class<PersistentChunkData> getComplexType() {
      return PersistentChunkData.class;
    }

    @Override
    public @NotNull String toPrimitive(@NotNull PersistentChunkData complex,
        @NotNull PersistentDataAdapterContext context) {
      return GSON.toJson(complex);
    }

    @Override
    public @NotNull PersistentChunkData fromPrimitive(@NotNull String primitive,
        @NotNull PersistentDataAdapterContext context) {
      return GSON.fromJson(primitive, PersistentChunkData.class);
    }
  }
}
