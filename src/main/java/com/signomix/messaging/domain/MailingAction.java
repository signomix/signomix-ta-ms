package com.signomix.messaging.domain;

import java.util.Date;

/* import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id; */

//@Entity
public class MailingAction{
    /*@Id @GeneratedValue*/ private Long id;
    String docUid;
    String target;
    Status status;
    Date createdAt;
    Date startedAt;
    Date plannedAt;
    
    public Date getPlannedAt() {
        return plannedAt;
    }
    public void setPlannedAt(Date plannedAt) {
        this.plannedAt = plannedAt;
    }

    public Date getStartedAt() {
        return startedAt;
    }
    public void setStartedAt(Date startedAt) {
        this.startedAt = startedAt;
    }
    public Date getFinishedAt() {
        return finishedAt;
    }
    public void setFinishedAt(Date finishedAt) {
        this.finishedAt = finishedAt;
    }
    Date finishedAt;
    String failures="";
    String error;
    public String getError() {
        return error;
    }
    public void setError(String error) {
        this.error = error;
    }
    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getDocUid() {
        return docUid;
    }
    public void setDocUid(String docUid) {
        this.docUid = docUid;
    }
    public String getTarget() {
        return target;
    }
    public void setTarget(String target) {
        this.target = target;
    }
    public Status getStatus() {
        return status;
    }
    public void setStatus(Status status) {
        this.status = status;
    }
    public Date getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    public String getFailures() {
        return failures;
    }
    public void setFailures(String failures) {
        this.failures = failures;
    }
}
