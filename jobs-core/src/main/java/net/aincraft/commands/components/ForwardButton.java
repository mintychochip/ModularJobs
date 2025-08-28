package net.aincraft.commands.components;

import java.util.function.Function;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.event.ClickEvent;
import org.jetbrains.annotations.NotNull;

public class ForwardButton implements ComponentLike {

  private final int pageNumber;
  private final int maxPages;
  private final Function<Integer, ClickEvent> pageFunction;

  public ForwardButton(int pageNumber, int maxPages, Function<Integer, ClickEvent> pageFunction) {
    this.pageNumber = pageNumber;
    this.maxPages = maxPages;
    this.pageFunction = pageFunction;
  }

  @Override
  public @NotNull Component asComponent() {
    return Component.text("Forward »").clickEvent();
  }
}
