package org.alfresco.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.alfresco.auth.OAuthTokenManager;
import org.alfresco.services.dto.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service façade that encapsulates the HTTP communication with the "Agent Platform" back‑end.
 * <p>
 * Two main operations are exposed:
 * <ol>
 *   <li>{@link #getRagAgents(int, int)} – retrieves a paginated list of available agents.</li>
 *   <li>{@link #invokeAgent(String, String)} – invokes the <em>latest</em> version of a single
 *       agent with a user prompt and returns the assistant's reply.</li>
 * </ol>
 * <p>
 * The service is <strong>state‑less</strong>: it caches nothing and delegates all token
 * management to an {@link OAuthTokenManager}. For testing purposes a custom
 * {@link RestTemplate} can be injected.
 *
 * @author Angel
 */
public final class AgentBuilderService {

    private static final Log LOGGER = LogFactory.getLog(AgentBuilderService.class);

    /** Base URL of the Agent Platform REST API, e.g. {@code https://api.ai.dev.experience.hyland.com}. */
    private String apiUrl;

    /** Environment identifier forwarded to the invoke API. */
    private String hxEnvId;

    /** Component that supplies valid OAuth 2 access‑tokens. */
    private OAuthTokenManager oauthTokenManager;

    /** HTTP client used for all REST calls. */
    private RestTemplate restTemplate = new RestTemplate();

    /** JSON Formatter. */
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Fetches a paginated list of RAG agents from the platform.
     * NOTE: API doesn't support type filtering, so pagination applies to ALL agents
     * before filtering. This may result in fewer RAG agents per page than requested.
     *
     * @param offset zero‑based index of the first element to return (must be ≥ 0)
     * @param limit  maximum number of elements to return (must be ≥ 1)
     * @return populated {@link AgentResponse}
     * @throws RuntimeException when the HTTP request fails or the JSON cannot be deserialized
     */
    public AgentResponse getRagAgents(int offset, int limit) {
        try {

            String token = oauthTokenManager.getAccessToken();
            String url = String.format("%s/agent-platform/v1/agents/?offset=%d&limit=%d",
                    apiUrl, offset, limit);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            LOGGER.info("Fetching agents from: " + url);

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Failed to retrieve agents – HTTP " + response.getStatusCode());
            }

            AgentResponse agentResp = objectMapper.readValue(response.getBody(), AgentResponse.class);
            int originalCount = agentResp.getAgents().size();

            // Filter for RAG agents only
            List<Agent> ragAgents = agentResp.getAgents()
                    .stream()
                    .filter(a -> "rag".equalsIgnoreCase(a.getType()))
                    .collect(Collectors.toList());
            agentResp.setAgents(ragAgents);

            LOGGER.info("Successfully retrieved " + agentResp.getAgents().size() + " RAG agents");
            return agentResp;

        } catch (Exception ex) {
            LOGGER.error("Unexpected error while fetching agents", ex);
            throw new RuntimeException("Error while fetching agents", ex);
        }
    }

    /**
     * Sends {@code prompt} to the <em>latest</em> version of the specified agent and returns the
     * conversation result.
     *
     * @param agentId UUID of the agent to invoke (non‑null, non‑blank)
     * @param prompt  user question or instruction (non‑null, non‑blank)
     * @return the assistant's answer and its reference list
     * @throws RuntimeException when the HTTP call fails or the response cannot be parsed
     */
    public ChatResponse invokeAgent(String agentId, String prompt) {
        try {
            String token = oauthTokenManager.getAccessToken();
            String url   = String.format("%s/agent-platform/v1/agents/%s/versions/latest/invoke",
                    apiUrl, agentId);

            // Build request body
            ObjectNode body  = objectMapper.createObjectNode();
            ArrayNode  msgs  = body.putArray("messages");
            ObjectNode msg   = msgs.addObject();
            msg.put("role", "user");
            msg.put("content", prompt);
            body.putObject("filterValue");  // required but empty
            body.put("hx_env_id", hxEnvId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(token);

            ResponseEntity<String> resp = restTemplate.postForEntity(
                    url, new HttpEntity<>(objectMapper.writeValueAsString(body), headers), String.class);

            if (!resp.getStatusCode().is2xxSuccessful()) {
                throw new RuntimeException("Agent call failed – HTTP " + resp.getStatusCode());
            }

            RawApiPayload api = objectMapper.readValue(resp.getBody(), RawApiPayload.class);
            String answer = api.response.choices.get(0).message.content;

            List<Reference> refs = api.response.custom_outputs.source_nodes.stream()
                    .map(sn -> new Reference(
                            sn.node.id,
                            sn.node.extra_info.objectId,
                            sn.score))
                    .collect(Collectors.toList());

            LOGGER.info("Agent reply: " + answer);
            return new ChatResponse(answer, refs);

        } catch (Exception ex) {
            LOGGER.error("Error while invoking agent", ex);
            throw new RuntimeException("Error during agent invocation", ex);
        }
    }

    /**
     * Sets the base API URL, e.g. {@code https://api.example.com}.
     */
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    /**
     * Sets the environment identifier to be forwarded in agent invocations.
     */
    public void setHxEnvId(String hxEnvId) {
        this.hxEnvId = hxEnvId;
    }

    /**
     * Injects the token manager used to obtain OAuth 2 access tokens.
     */
    public void setOauthTokenManager(OAuthTokenManager oauthTokenManager) {
        this.oauthTokenManager = oauthTokenManager;
    }

    /**
     * Injects a custom {@link RestTemplate}. Useful for mocking in unit tests.
     */
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
}
