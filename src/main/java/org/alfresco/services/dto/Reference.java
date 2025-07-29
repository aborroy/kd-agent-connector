package org.alfresco.services.dto;

public class Reference {
    private final String referenceId;
    private final String objectId;
    private final double rankScore;

    public Reference(String referenceId, String objectId, double rankScore) {
        this.referenceId = referenceId;
        this.objectId    = objectId;
        this.rankScore   = rankScore;
    }
    public String getReferenceId() { return referenceId; }
    public String getObjectId()    { return objectId; }
    public double getRankScore()   { return rankScore; }
}
