package net.aincraft.commands;

import net.aincraft.JobProgression;
import net.kyori.adventure.key.Key;

public interface JobTopPageProvider {

  Page<JobProgression> getPage(Key jobKey, int pageNumber, int pageSize);
}
