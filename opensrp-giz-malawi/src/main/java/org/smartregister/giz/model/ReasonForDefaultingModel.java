package org.smartregister.giz.model;

public class ReasonForDefaultingModel {
    private String dateCreated;
    private String baseEntityId;
    private String outreachDate;
    private String id;
    private String followupDate;
    private String outreachDefaultingReason;
    private String otherOutreachDefaultingReason;
    private String additionalDefaultingNotes;

    public ReasonForDefaultingModel() {
    }

    public ReasonForDefaultingModel(String id, String baseEntityId, String dateCreated, String outreachDate, String followupDate, String outreachDefaultingReason, String otherOutreachDefaultingReason, String additionalDefaultingNotes) {
        this.id = id;
        this.baseEntityId = baseEntityId;
        this.dateCreated = dateCreated;
        this.outreachDate = outreachDate;
        this.followupDate = followupDate;
        this.outreachDefaultingReason = outreachDefaultingReason;
        this.otherOutreachDefaultingReason = otherOutreachDefaultingReason;
        this.baseEntityId = baseEntityId;
        this.additionalDefaultingNotes = additionalDefaultingNotes;
    }

    public String getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(String dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getBaseEntityId() {
        return baseEntityId;
    }

    public void setBaseEntityId(String baseEntityId) {
        this.baseEntityId = baseEntityId;
    }

    public String getOutreachDate() {
        return outreachDate;
    }

    public void setOutreachDate(String outreachDate) {
        this.outreachDate = outreachDate;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFollowupDate() {
        return followupDate;
    }

    public void setFollowupDate(String followupDate) {
        this.followupDate = followupDate;
    }

    public String getOutreachDefaultingReason() {
        return outreachDefaultingReason;
    }

    public void setOutreachDefaultingReason(String outreachDefaultingReason) {
        this.outreachDefaultingReason = outreachDefaultingReason;
    }

    public String getOtherOutreachDefaultingReason() {
        return otherOutreachDefaultingReason;
    }

    public void setOtherOutreachDefaultingReason(String otherOutreachDefaultingReason) {
        this.otherOutreachDefaultingReason = otherOutreachDefaultingReason;
    }

    public String getAdditionalDefaultingNotes() {
        return additionalDefaultingNotes;
    }

    public void setAdditionalDefaultingNotes(String additionalDefaultingNotes) {
        this.additionalDefaultingNotes = additionalDefaultingNotes;
    }
}
