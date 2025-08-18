package net.aincraft.container;

import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public record PayableType(PayableHandler handler, Key key) implements Keyed {

}
