<#ftl ns_prefixes={"D":"http://omadac.org/xsd/sqldatabase"}>
<#--

    Omadac - The Open Map Database Compiler
    http://omadac.org
 
    (C) 2010, Harald Wellmann and Contributors

    This library is free software; you can redistribute it and/or
    modify it under the terms of the GNU Lesser General Public
    License as published by the Free Software Foundation;
    version 2.1 of the License.

    This library is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
    Lesser General Public License for more details.
-->
<#include "sqlmacros_${dialect}.ftl">
<#list xml.database.schema as schema>
  <#list schema["D:table[D:index]"] as table>
    <#if table.@name == tableName>
      <#list table.index as index>
        <#assign indexname><@SqlIndexName table, index/></#assign>
create index ${indexname} on ${schema.@name}.${table.@name} (<@SqlIndexColumns index/>);

      </#list>
    </#if>
  </#list>    
</#list>

