package org.alfresco.services.dto;

import java.util.List;

public class ChatResponse {
    private final String answer;
    private final List<Reference> references;

    public ChatResponse(String answer, List<Reference> references) {
        this.answer     = answer;
        this.references = references;
    }
    public String getAnswer()                 { return answer; }
    public List<Reference> getReferences() { return references; }
}
