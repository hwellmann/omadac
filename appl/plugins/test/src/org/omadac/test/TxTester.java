package org.omadac.test;

import org.omadac.make.Target;


public class TxTester extends Thread
{
    private Target target;

    public void setTarget(Target target) {
        this.target = target;
    }

    @Override
    public void run()
    {
        target.clean();
        target.compile();
    }
}
