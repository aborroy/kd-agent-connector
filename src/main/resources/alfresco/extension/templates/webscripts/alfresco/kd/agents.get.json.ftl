<#setting number_format="0">
{
  "agents": [
    <#list agents as agent>
    {
      "id": "${agent.id}",
      "name": "${agent.displayName}",
      "description": "${agent.description!''}",
      "shortDescription": "${agent.shortDescription!''}",
      "type": "${agent.type}",
      "status": "${agent.status}",
      "currentVersionId": "${agent.currentVersionId}",
      "isGlobalAgent": ${agent.isGlobalAgent?c},
      "isActive": ${agent.isActive?c},
      "isTaskAgent": ${agent.isTaskAgent?c},
      "isToolAgent": ${agent.isToolAgent?c},
      "createdBy": "${agent.createdBy!''}",
      "modifiedBy": "${agent.modifiedBy!''}",
      "createdAt": "${agent.createdAt!''}",
      "modifiedAt": "${agent.modifiedAt!''}",
      "createdAtFormatted": "${agent.createdAtFormatted!''}",
      "modifiedAtFormatted": "${agent.modifiedAtFormatted!''}"
    }<#if agent_has_next>,</#if>
    </#list>
  ],
  "pagination": {
    "totalItems": ${pagination.totalItems},
    "offset": ${pagination.offset},
    "limit": ${pagination.limit},
    "hasMore": ${pagination.hasMore?c}
  },
  "summary": {
    "totalAgents": ${totalAgents},
    "currentOffset": ${currentOffset},
    "currentLimit": ${currentLimit},
    "hasMore": ${hasMore?c},
    "agentsByType": {
      <#if agentsByType??>
        <#assign typeKeys = agentsByType?keys>
        <#list typeKeys as type>
        "${type}": ${agentsByType[type]}<#if type_has_next>,</#if>
        </#list>
      </#if>
    },
    "agentsByStatus": {
      <#if agentsByStatus??>
        <#assign statusKeys = agentsByStatus?keys>
        <#list statusKeys as status>
        "${status}": ${agentsByStatus[status]}<#if status_has_next>,</#if>
        </#list>
      </#if>
    }
  }<#if error??>,
  "error": {
    "hasError": ${error?c},
    "message": "${errorMessage!''}"
  }</#if>
}