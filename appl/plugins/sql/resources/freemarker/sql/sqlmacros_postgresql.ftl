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
<#macro SqlType column>
<#if column.@auto_increment[0]?? && column.@auto_increment == "true">SERIAL<#rt>
<#else>
  <#switch column.@type>
    <#case "int8">smallint<#break>
    <#case "int16">smallint<#break>
    <#case "int32">int<#break>
    <#case "int64">bigint<#break>
    <#case "char">char(${column.@length})<#break>
    <#case "varchar">varchar(${column.@length})<#break>
    <#case "blob">bytea<#break>
    <#case "boolean">boolean<#break>
    <#case "decimal">decimal(${column.@precision}, ${column.@scale})<#break>    
    <#case "text">text<#break>
    <#case "datetime">datetime<#break>
    <#default><#stop "Unhandled column type ${column.@type}">
  </#switch>
</#if>  
</#macro>

<#macro SqlNull column>
<#if column.@nullable = "false"> NOT NULL</#if><#rt>
</#macro>

<#macro SqlAuto column></#macro>

<#macro SqlPrimaryKey table>
<#list table["D:column[@primary_key='true']"] as pk>
${pk.@name}<#if pk_has_next>, </#if></#list><#t>
</#macro>

<#macro SqlIndexName table, index>
IX_${table.@name?replace('_','')}_<#t>
<#list index.column as col>
${col.@name?replace('_','')}<#if col_has_next>_</#if><#t>
</#list>
</#macro>

<#macro SqlIndexColumns index>
<#list index.column as col>
${col.@name}<#if col.@order[0]??> ${col.@order?upper_case}</#if><#t>
<#if col_has_next>, </#if></#list><#t>
</#macro>

<#assign ifExists = "if exists">
<#assign cascade = " cascade">


