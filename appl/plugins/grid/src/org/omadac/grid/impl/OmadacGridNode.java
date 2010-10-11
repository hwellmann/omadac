package org.omadac.grid.impl;

import org.omadac.base.ExecutionContextImpl;
import org.omadac.config.ConfigManager;
import org.omadac.make.ExecutionContext;


public class OmadacGridNode
{
    private static ExecutionContextImpl executionContext;
    private static ConfigManager cm;

    protected void setExecutionContext(ExecutionContext executionContext)
    {
        OmadacGridNode.executionContext = (ExecutionContextImpl) executionContext;
    }
    
    public static synchronized ExecutionContextImpl getExecutionContext()
    {
        if (cm == null)
        {
            cm = new GridConfigManager();
            executionContext.setConfigManager(cm);
        }
        return executionContext;
    }
}
