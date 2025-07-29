package org.alfresco.services.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class RawApiPayload {
    public Response response;
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Response {
        public List<Choice> choices;
        public CustomOutputs custom_outputs;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Choice    { public Message message; }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Message   { public String content;  }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class CustomOutputs {
        public List<SourceNode> source_nodes;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class SourceNode {
        public Node node;     public double score;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Node {
        @JsonProperty("id_") public String id;
        public Extra extra_info;
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static final class Extra { @JsonProperty("object_id") public String objectId; }
}
