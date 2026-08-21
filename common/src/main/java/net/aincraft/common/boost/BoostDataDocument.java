package net.aincraft.common.boost;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.jetbrains.annotations.Nullable;

/**
 * JSON persistence document for {@code SerializableBoostData}. Rule
 * {@code conditions} are opaque bytes from {@link dev.conditions.ConditionSerializer}.
 */
public record BoostDataDocument(
    String kind,
    @Nullable String slots,
    @Nullable String duration,
    SourceDocument source
) {

  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

  public record SourceDocument(
      String key,
      @Nullable String description,
      List<RuleDocument> rules
  ) {}

  /**
   * One ruled boost: priority, condition serializer bytes (base64), boost amount.
   */
  public record RuleDocument(
      int priority,
      String conditions,
      BoostDocument boost
  ) {

    public static RuleDocument of(int priority, byte[] conditionBytes, BoostDocument boost) {
      return new RuleDocument(
          priority, Base64.getEncoder().encodeToString(conditionBytes), boost);
    }

    public byte[] conditionBytes() {
      return Base64.getDecoder().decode(conditions);
    }
  }

  public record BoostDocument(
      String type,
      double amount
  ) {}

  public static byte[] toJson(BoostDataDocument document) {
    return GSON.toJson(document).getBytes(StandardCharsets.UTF_8);
  }

  public static BoostDataDocument fromJson(byte[] bytes) {
    BoostDataDocument document =
        GSON.fromJson(new String(bytes, StandardCharsets.UTF_8), BoostDataDocument.class);
    if (document == null || document.source() == null) {
      throw new IllegalArgumentException("invalid boost data JSON");
    }
    return document;
  }
}
