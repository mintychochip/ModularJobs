package net.aincraft.commands;

import java.util.List;

public record Page<T>(List<T> data, int pageNumber) {

}
