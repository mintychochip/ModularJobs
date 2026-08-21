package net.aincraft.common.condition;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.nio.charset.StandardCharsets;
import java.util.List;
import net.aincraft.common.boost.BoostDataDocument;
import net.aincraft.common.boost.BoostDataDocument.BoostDocument;
import net.aincraft.common.boost.BoostDataDocument.RuleDocument;
import net.aincraft.common.boost.BoostDataDocument.SourceDocument;
import dev.conditions.Condition;
import dev.conditions.ConditionSerializer;
import dev.conditions.Conditions;
import dev.conditions.SneakingCondition;
import dev.conditions.gson.GsonConditionSerializer;
import org.junit.jupiter.api.Test;

class BoostDataDocumentTest {

  private final ConditionSerializer serializer = GsonConditionSerializer.gson();

  @Test
  void roundTripStoresPriorityAndConditionBytes() {
    byte[] conditionBytes = serializer.write(Conditions.sneaking(true));
    BoostDataDocument document = new BoostDataDocument(
        "passive",
        "all",
        null,
        new SourceDocument(
            "modularjobs:mining_helmet",
            "helmet",
            List.of(RuleDocument.of(100, conditionBytes, new BoostDocument("multiplicative", 1.25)))
        )
    );

    byte[] json = BoostDataDocument.toJson(document);
    BoostDataDocument back = BoostDataDocument.fromJson(json);

    assertEquals("passive", back.kind());
    assertEquals("all", back.slots());
    RuleDocument rule = back.source().rules().getFirst();
    assertEquals(100, rule.priority());
    assertArrayEquals(conditionBytes, rule.conditionBytes());
    Condition decoded = serializer.read(rule.conditionBytes());
    assertInstanceOf(SneakingCondition.class, decoded);
    assertEquals("multiplicative", rule.boost().type());
  }

  @Test
  void jsonContainsBase64ConditionsNotNestedObject() {
    byte[] conditionBytes = serializer.write(Conditions.sneaking(true));
    BoostDataDocument document = new BoostDataDocument(
        "consumable",
        null,
        "PT1H",
        new SourceDocument(
            "modularjobs:timed",
            null,
            List.of(RuleDocument.of(1, conditionBytes, new BoostDocument("additive", 5)))
        )
    );
    String json = new String(BoostDataDocument.toJson(document), StandardCharsets.UTF_8);
    assertEquals(true, json.contains("\"priority\":1") || json.contains("\"priority\": 1"));
    assertEquals(true, json.contains("\"conditions\":"));
    assertEquals(false, json.contains("minecraft:entity_properties"));
  }
}
