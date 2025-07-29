package org.alfresco.services.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Agent {
    private String id;
    private String type;
    private String name;
    private String description;
    private String status;

    @JsonProperty("isGlobalAgent")
    private boolean isGlobalAgent;

    @JsonProperty("currentVersionId")
    private String currentVersionId;

    @JsonProperty("createdAt")
    private String createdAt;

    @JsonProperty("createdBy")
    private String createdBy;

    @JsonProperty("modifiedAt")
    private String modifiedAt;

    @JsonProperty("modifiedBy")
    private String modifiedBy;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isGlobalAgent() {
        return isGlobalAgent;
    }

    public void setGlobalAgent(boolean globalAgent) {
        isGlobalAgent = globalAgent;
    }

    public String getCurrentVersionId() {
        return currentVersionId;
    }

    public void setCurrentVersionId(String currentVersionId) {
        this.currentVersionId = currentVersionId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getModifiedAt() {
        return modifiedAt;
    }

    public void setModifiedAt(String modifiedAt) {
        this.modifiedAt = modifiedAt;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Override
    public String toString() {
        return String.format("Agent{id='%s', name='%s', type='%s', status='%s'}",
                id, name, type, status);
    }
}
