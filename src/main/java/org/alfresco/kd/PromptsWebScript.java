package org.alfresco.kd;

import org.alfresco.services.AgentBuilderService;
import org.alfresco.services.dto.ChatResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.extensions.webscripts.Cache;
import org.springframework.extensions.webscripts.DeclarativeWebScript;
import org.springframework.extensions.webscripts.Status;
import org.springframework.extensions.webscripts.WebScriptException;
import org.springframework.extensions.webscripts.WebScriptRequest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Web‑Script that forwards a user <em>prompt</em> to an AI {@code Agent} identified by
 * {@code agentId} and returns the agent's textual answer together with its reference list.
 *
 * <h2>Expected request</h2>
 * <p>The HTTP body <strong>must</strong> be a JSON object with two properties:</p>
 * <pre>{@code
 * {
 *   "agentId" : "string",   // required, identifies the agent to invoke
 *   "prompt"  : "string"    // required, the question or instruction
 * }
 * }</pre>
 *
 * <h2>Successful response (HTTP 200)</h2>
 * <pre>{@code
 * {
 *   "answer"     : "... textual response ...",
 *   "references" : [ { "title": "...", "url": "..." }, ... ]
 * }
 * }</pre>
 *
 * <h2>Error Handling</h2>
 * <ul>
 *   <li>{@link Status#STATUS_BAD_REQUEST 400} – malformed or incomplete request body.</li>
 *   <li>{@link Status#STATUS_INTERNAL_SERVER_ERROR 500} – the agent invocation failed.</li>
 * </ul>
 *
 */
public final class PromptsWebScript extends DeclarativeWebScript {

    private static final Log LOGGER = LogFactory.getLog(PromptsWebScript.class);

    private static final String JSON_AGENT_ID = "agentId";
    private static final String JSON_PROMPT   = "prompt";

    private AgentBuilderService agentBuilderService;

    /**
     * Processes the Web‑Script call.
     *
     * @param req   current request
     * @param status mutable object used to influence the HTTP response
     * @param cache cache control descriptor (ignored)
     * @return model for template rendering
     * @throws WebScriptException translated errors with appropriate HTTP status
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        String body = readRequestBody(req);
        JSONObject json = parseJson(body);

        String agentId = getRequired(json, JSON_AGENT_ID);
        String prompt  = getRequired(json, JSON_PROMPT);

        ChatResponse response;
        try {
            response = agentBuilderService.invokeAgent(agentId, prompt);
        } catch (Exception ex) {
            LOGGER.error("Agent invocation failed", ex);
            throw new WebScriptException(Status.STATUS_INTERNAL_SERVER_ERROR,
                    "Unable to invoke agent.", ex);
        }

        Map<String, Object> model = new HashMap<>();
        model.put("answer",     response.getAnswer());
        model.put("references", response.getReferences());
        return model;
    }

    /**
     * Reads the full request body as a string.
     *
     * @throws WebScriptException with status 400 when reading fails
     */
    private static String readRequestBody(WebScriptRequest req) {
        try {
            return req.getContent().getContent();
        } catch (IOException iox) {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST,
                    "Could not read request body.", iox);
        }
    }

    /**
     * Parses a JSON string into a {@link JSONObject}.
     *
     * @throws WebScriptException with status 400 when parsing fails
     */
    private static JSONObject parseJson(String raw) {
        try {
            return new JSONObject(new JSONTokener(raw));
        } catch (JSONException jex) {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST,
                    "Request body is not valid JSON.", jex);
        }
    }

    /**
     * Extracts a mandatory field from the {@code json} object.
     *
     * @param json    source object
     * @param key     property name expected to be present and non‑blank
     * @return the trimmed string value
     * @throws WebScriptException with status 400 when the property is missing or blank
     */
    private static String getRequired(JSONObject json, String key) {
        String value;
        try {
            value = json.getString(key);
        } catch (JSONException jex) {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST,
                    "Missing JSON property '" + key + "'.", jex);
        }
        if (value == null || value.isBlank()) {
            throw new WebScriptException(Status.STATUS_BAD_REQUEST,
                    "JSON property '" + key + "' must not be blank.");
        }
        return value.trim();
    }

    /**
     * Setter invoked by Spring for dependency injection.
     *
     * @param agentBuilderService back‑end service used to invoke agents
     */
    public void setAgentBuilderService(AgentBuilderService agentBuilderService) {
        this.agentBuilderService = agentBuilderService;
    }
}
