package org.omadac.grid.impl;

import org.omadac.base.ExecutionContextImpl;
import org.omadac.config.ConfigManager;
import org.omadac.make.ExecutionContext;
import org.osgi.service.component.ComponentContext;


public class OmadacGridNode
{
    private static ComponentContext componentContext;
    private static ExecutionContextImpl executionContext;
    private static ConfigManager cm;

    protected void activate(ComponentContext cc)
    {
        OmadacGridNode.componentContext = cc;
    }
    
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
