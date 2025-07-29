package org.alfresco.kd;

import org.alfresco.services.AgentBuilderService;
import org.alfresco.services.dto.Agent;
import org.alfresco.services.dto.AgentResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.extensions.webscripts.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Web‑Script that returns a paginated list of {@link Agent} records obtained from
 * {@link AgentBuilderService}.
 * <p>
 * <strong>Query parameters:</strong>
 * <ul>
 *   <li>{@code offset} –Zero‑based index of the first item to return (default
 *   {@value #DEFAULT_OFFSET}).</li>
 *   <li>{@code limit} –Maximum number of items to return (default
 *   {@value #DEFAULT_LIMIT}).</li>
 * </ul>
 *
 * <p>On success the script responds with HTTP 200 and a model structure similar to:</p>
 * <pre>{@code
 * {
 *   "agents"        : [ {agent‑details} ],
 *   "pagination"    : { ... },
 *   "totalAgents"   : 273,
 *   "currentOffset" : 20,
 *   "currentLimit"  : 10,
 *   "hasMore"       : true,
 *   "agentsByType"   : { "tool": 123, "task": 150 },
 *   "agentsByStatus" : { "CREATED": 270, "DEPRECATED": 3 }
 * }
 * }</pre>
 *
 * <p>If an exception is thrown the script returns HTTP 500 and a minimal error payload:</p>
 * <pre>{@code
 * {
 *   "error"        : true,
 *   "errorMessage" : "...",
 *   "agents"       : []
 * }
 * }</pre>
 *
 */
public final class AgentsWebScript extends DeclarativeWebScript {

    private static final Log LOGGER = LogFactory.getLog(AgentsWebScript.class);

    /** Default value for the {@code offset} request parameter. */
    private static final int DEFAULT_OFFSET = 0;

    /** Default value for the {@code limit} request parameter. */
    private static final int DEFAULT_LIMIT = 500;

    private static final DateTimeFormatter ISO_PARSER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    private static final DateTimeFormatter HUMAN_READABLE =
            DateTimeFormatter.ofPattern("MMM dd, yyyy 'at' HH:mm");

    /** Back‑end service injected by Spring. */
    private AgentBuilderService agentBuilderService;

    /**
     * Executes the Web‑Script and builds the response model.
     * Note: Only returns agents with type="rag".
     *
     * @param req    incoming request
     * @param status mutable HTTP status descriptor
     * @return never‑{@code null} model ready for template rendering
     */
    @Override
    protected Map<String, Object> executeImpl(WebScriptRequest req, Status status, Cache cache) {
        Map<String, Object> model = new HashMap<>();

        try {
            int offset = readIntParam(req.getParameter("offset"), DEFAULT_OFFSET);
            int limit  = readIntParam(req.getParameter("limit"),  DEFAULT_LIMIT);

            // Get RAG agents only
            AgentResponse response = agentBuilderService.getRagAgents(offset, limit);
            List<Agent> ragAgents = response.getAgents();

            model.put("agents", ragAgents.stream()
                    .map(this::toView)
                    .collect(Collectors.toList()));
            model.put("pagination", response.getPagination());

            // Convenience shortcuts for templates / front‑end
            model.put("totalAgents", response.getPagination().getTotalItems());
            model.put("currentOffset", response.getPagination().getOffset());
            model.put("currentLimit", response.getPagination().getLimit());
            model.put("hasMore", response.getPagination().isHasMore());

            // Aggregate counts
            model.put("agentsByType", aggregate(ragAgents, Agent::getType));
            model.put("agentsByStatus", aggregate(ragAgents, Agent::getStatus));

            LOGGER.info(String.format("Retrieved %d RAG agents (offset=%d, limit=%d)",
                    ragAgents.size(), offset, limit));

        } catch (Exception ex) {
            handleError(status, model, ex);
        }

        return model;
    }

    /**
     * Converts a domain {@link Agent} into a view map expected by the template layer.
     *
     * @param agent domain object
     * @return map representing one agent record
     */
    private Map<String, Object> toView(Agent agent) {
        Map<String, Object> view = new HashMap<>();

        view.put("id", agent.getId());
        view.put("name", agent.getName());
        view.put("description", agent.getDescription());
        view.put("type", agent.getType());
        view.put("status", agent.getStatus());
        view.put("isGlobalAgent", agent.isGlobalAgent());
        view.put("currentVersionId", agent.getCurrentVersionId());
        view.put("createdBy", agent.getCreatedBy());
        view.put("modifiedBy", agent.getModifiedBy());

        view.put("createdAt", agent.getCreatedAt());
        view.put("modifiedAt", agent.getModifiedAt());
        view.put("createdAtFormatted", humanDate(agent.getCreatedAt()));
        view.put("modifiedAtFormatted", humanDate(agent.getModifiedAt()));

        // Convenience flags for templates
        view.put("isActive", "CREATED".equals(agent.getStatus()));
        view.put("isTaskAgent", "task".equals(agent.getType()));
        view.put("isToolAgent", "tool".equals(agent.getType()));

        // Display helpers
        view.put("displayName", agent.getName() != null ? agent.getName() : "Unnamed Agent");
        view.put("shortDescription", truncate(agent.getDescription(), 100));

        return view;
    }

    /**
     * Safely formats an ISO‑8601 timestamp to a human‑readable representation.
     *
     * @param iso8601 timestamp in ISO‑8601 format, e.g. {@code 2025-07-28T17:22:39.496818+00:00}
     * @return formatted date or the original string if parsing fails
     */
    private String humanDate(String iso8601) {
        if (iso8601 == null || iso8601.isBlank()) {
            return "N/A";
        }
        try {
            return OffsetDateTime.parse(iso8601, ISO_PARSER).format(HUMAN_READABLE);
        } catch (DateTimeParseException ex) {
            LOGGER.debug("Unable to parse date: " + iso8601, ex);
            return iso8601;
        }
    }

    /**
     * Parses an integer request parameter returning a fallback value on error.
     *
     * @param raw      raw parameter value
     * @param fallback fallback to use when parsing fails
     * @return parsed int or {@code fallback}
     */
    private int readIntParam(String raw, int fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException ex) {
            LOGGER.warn("Ignoring malformed integer parameter '" + raw + "'", ex);
            return fallback;
        }
    }

    /**
     * Builds a histogram of {@code agents} using the supplied {@code classifier}.
     *
     * @param agents     source collection
     * @param classifier function extracting the grouping key
     * @return map from key to count
     */
    private static Map<String, Long> aggregate(List<Agent> agents, Function<Agent, String> classifier) {
        return agents.stream().collect(Collectors.groupingBy(classifier, Collectors.counting()));
    }

    /**
     * Populates the error model and sets HTTP 500 state.
     *
     * @param status mutable status holder
     * @param model  template model (mutable)
     * @param ex     root cause
     */
    private void handleError(Status status, Map<String, Object> model, Exception ex) {
        LOGGER.error("Failed to retrieve agents", ex);
        status.setCode(Status.STATUS_INTERNAL_SERVER_ERROR);
        status.setMessage("Failed to retrieve agents");
        status.setRedirect(true);

        model.put("error", true);
        model.put("errorMessage", ex.getMessage());
        model.put("agents", List.of());
    }

    /**
     * Truncates {@code str} to the specified {@code maxLen}, appending an ellipsis when necessary.
     *
     * @param str    input string (may be {@code null})
     * @param maxLen maximum allowed length, must be ≥ 4
     * @return original or truncated string
     */
    private static String truncate(String str, int maxLen) {
        if (str == null || str.length() <= maxLen) {
            return str;
        }
        return str.substring(0, maxLen - 3) + "...";
    }

    /**
     * Setter invoked by Spring for dependency injection.
     *
     * @param agentBuilderService service instance to delegate to
     */
    public void setAgentBuilderService(AgentBuilderService agentBuilderService) {
        this.agentBuilderService = agentBuilderService;
    }
}