{
  "answer": "${answer?json_string}",
  "references": [
  <#list references as ref>{
    "referenceId": "${ref.referenceId?json_string}",
    "objectId":    "${ref.objectId?json_string}",
    "rankScore":   ${ref.rankScore}
  }<#if ref_has_next>,</#if></#list>
  ]
}