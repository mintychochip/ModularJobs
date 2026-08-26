package dev.mintychochip.event;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class EventBusTest {

  @Test
  void eventBusDelivers() {
    EventBus bus = new EventBus();
    AtomicReference<Object> got = new AtomicReference<>();
    bus.subscribe(got::set);

    JobJoinEvent event = new JobJoinEvent(UUID.randomUUID(), null, 1, false);
    JobJoinEvent published = bus.publish(event);

    assertSame(event, published);
    assertSame(event, got.get());
  }

  @Test
  void cancellableEventSharesCancelState() {
    EventBus bus = new EventBus();
    bus.subscribe(
        e -> {
          if (e instanceof JobsPaymentEvent payment) {
            payment.setCancelled(true);
          }
        });

    JobsPaymentEvent event = new JobsPaymentEvent(UUID.randomUUID(), null);
    assertFalse(event.isCancelled());
    bus.publish(event);
    assertTrue(event.isCancelled());
  }
}
