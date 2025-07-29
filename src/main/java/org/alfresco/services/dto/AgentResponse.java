package org.alfresco.services.dto;

import java.util.List;

public class AgentResponse {
    private List<Agent> agents;
    private Pagination pagination;

    public List<Agent> getAgents() {
        return agents;
    }

    public void setAgents(List<Agent> agents) {
        this.agents = agents;
    }

    public Pagination getPagination() {
        return pagination;
    }

    public void setPagination(Pagination pagination) {
        this.pagination = pagination;
    }
}
