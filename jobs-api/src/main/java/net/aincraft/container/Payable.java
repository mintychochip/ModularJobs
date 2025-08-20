package net.aincraft.container;

import org.jetbrains.annotations.ApiStatus.NonExtendable;

@NonExtendable
public record Payable(PayableType type, PayableAmount amount) {

}
