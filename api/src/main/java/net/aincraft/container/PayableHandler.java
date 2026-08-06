package net.aincraft.container;

import java.util.UUID;
import net.aincraft.JobProgression;

public interface PayableHandler {

  void pay(PayableContext context) throws IllegalArgumentException;

  record PayableContext(UUID playerId, Payable payable, JobProgression jobProgression) {}

  interface PayableVisualController {
    void display(PayableContext context);
  }

}
