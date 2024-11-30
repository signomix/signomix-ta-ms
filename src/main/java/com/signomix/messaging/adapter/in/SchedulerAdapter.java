package com.signomix.messaging.adapter.in;

import com.signomix.common.annotation.InboundAdapter;
import com.signomix.messaging.adapter.out.MailingActionRepository;
import com.signomix.messaging.domain.MailingAction;
import com.signomix.messaging.domain.mailing.MailingLogic;
import io.quarkus.scheduler.Scheduled;
import static io.quarkus.scheduler.Scheduled.ConcurrentExecution.SKIP;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@InboundAdapter
@ApplicationScoped
public class SchedulerAdapter {

    @Inject
    MailingActionRepository mailingRepository;

    @Inject
    MailingLogic mailingPort;
    
    @Scheduled(every="${com.signomix.messaging.scheduler.every:off}", delayed="60s", concurrentExecution = SKIP)     
    void getMailing() {
        MailingAction action=mailingRepository.findWaiting();
        if(null!=action){
            System.out.println(action.getId()+" "+action.getPlannedAt());
            mailingPort.runMailingAction(action);
        }else{
            System.out.println("no mailing waiting");
        }
    }
}
