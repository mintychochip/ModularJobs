package net.aincraft.service;

import java.util.Optional;
import net.aincraft.Job;
import net.kyori.adventure.key.Key;

public interface JobService {

  Optional<Job> getJob(Key jobKey);
}
