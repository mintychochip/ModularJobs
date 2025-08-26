package net.aincraft.domain;

import com.google.inject.PrivateModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import net.aincraft.Job;
import net.aincraft.JobProgression;
import net.aincraft.JobTask;
import net.aincraft.container.ActionType;
import net.aincraft.container.Payable;
import net.aincraft.domain.model.JobProgressionRecord;
import net.aincraft.domain.model.JobRecord;
import net.aincraft.domain.model.JobTaskRecord;
import net.aincraft.domain.model.PayableRecord;
import net.aincraft.util.DomainMapper;

public final class DomainModule extends PrivateModule {

  @Override
  protected void configure() {
    bind(new TypeLiteral<DomainMapper<Job, JobRecord>>() {
    })
        .to(JobRecordDomainMapperImpl.class)
        .in(Singleton.class);
    bind(new TypeLiteral<DomainMapper<JobProgression, JobProgressionRecord>>() {
    })
        .to(JobsProgressionRecordDomainMapperImpl.class)
        .in(Singleton.class);
    bind(new TypeLiteral<DomainMapper<ActionType, ActionTypeRecord>>() {
    })
        .to(ActionTypeRecordDomainMapperImpl.class)
        .in(Singleton.class);
    bind(new TypeLiteral<DomainMapper<JobTask, JobTaskRecord>>() {
    })
        .to(JobTaskRecordDomainMapperImpl.class)
        .in(Singleton.class);
    bind(new TypeLiteral<DomainMapper<Payable, PayableRecord>>() {
    })
        .to(PayableRecordDomainMapperImpl.class)
        .in(Singleton.class);
    bind(MappingService.class).to(MappingServiceImpl.class).in(Singleton.class);
    expose(MappingService.class);
  }
}
