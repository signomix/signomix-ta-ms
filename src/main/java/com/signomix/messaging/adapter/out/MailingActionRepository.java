package com.signomix.messaging.adapter.out;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;

import com.signomix.messaging.domain.MailingAction;
import com.signomix.messaging.domain.Status;
//import io.quarkus.hibernate.orm.panache.PanacheRepository;

@ApplicationScoped
public class MailingActionRepository /*implements PanacheRepository<MailingAction>*/{
    public List<MailingAction> findFailed(){
        //return list("status", Status.Failed);
        return new ArrayList<>();
    }

    public MailingAction findWaiting(){
        //return find("select a from MailingAction a where a.status=?1 order by plannedAt desc", Status.Planned).firstResult();
        return null;
    }

    public void persist(MailingAction action){
        //TODO: implement
        //persist(action);
    }

    public MailingAction findById(Long id){
        //return findById(id);
        return null;
    }

    public List<MailingAction> listAll(){
        //return list("status", Status.Planned);
        return new ArrayList<>();
    }
}
