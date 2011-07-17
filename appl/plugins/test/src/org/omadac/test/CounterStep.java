package org.omadac.test;

import java.util.ArrayList;
import java.util.List;

import org.omadac.make.ComplexStep;
import org.omadac.make.util.NumberRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CounterStep implements ComplexStep<CounterComplexTarget, CounterSubtarget>
{
    private static Logger log = LoggerFactory.getLogger(CounterStep.class);

    @Override
    public void compile(CounterSubtarget target)
    {
        int minId = target.getRange().getMinId();
        int maxId = target.getRange().getMaxId();
        
        for (int i = minId; i <= maxId; i++) {
            log.info("tick {}", i);
            try
            {
                Thread.sleep(100);
            }
            catch (InterruptedException exc)
            {
                log.error("interrupted", exc);
            }
        }
    }

    @Override
    public void clean(CounterSubtarget target)
    {
        log.info("clean");
    }

    @Override
    public List<CounterSubtarget> split(CounterComplexTarget target)
    {
        int minId = target.getStart();
        int maxId = minId + target.getNumTicks() - 1;
        List<Integer> ids = new ArrayList<Integer>(target.getNumTicks());
        for (int i = minId; i <= maxId; i++) {
            ids.add(i);
        }
        List<NumberRange<Integer>> ranges = NumberRange.split(ids, 10);
        List<CounterSubtarget> subtargets = new ArrayList<CounterSubtarget>(ranges.size());
        for (NumberRange<Integer> range : ranges) {
            CounterSubtarget subtarget = new CounterSubtarget(range);
            subtargets.add(subtarget);
        }
        return subtargets;
    }

    @Override
    public void merge(CounterComplexTarget target)
    {
        log.info("merge");
    }

    @Override
    public void cleanAll(CounterComplexTarget target)
    {
        log.info("cleanAll");
    }
}
