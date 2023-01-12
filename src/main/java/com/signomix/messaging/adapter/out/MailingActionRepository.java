package com.signomix.messaging.adapter.out;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.signomix.messaging.domain.MailingAction;
import com.signomix.messaging.domain.Status;
import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class MailingActionRepository implements PanacheRepository<MailingAction>{
    public List<MailingAction> findFailed(){
        return list("status", Status.Failed);
    }

    public MailingAction findWaiting(){
        return find("select a from MailingAction a where a.status=?1 order by plannedAt desc", Status.Planned).firstResult();
    }
}
