package eu.stamp.cbc.extraction;

import org.evosuite.testcase.execution.ExecutionTracer;
import org.junit.runner.Description;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TestCaseRunListener extends RunListener {
    private static final Logger LOG = LoggerFactory.getLogger(TestCaseRunListener.class);
    @Override
    public void testStarted(Description description){
        ExecutionTracer.setThread(null);
        ExecutionTracer.getExecutionTracer().clear();
//        ExecutionTracer.getExecutionTracer().enable();
    }


    @Override
    public void testFinished(Description description){
        String testName = description.getClassName()+"."+description.getMethodName();
        LOG.info("Collecting traces of test {}",testName);
        Map<String, Map<String, Map<Integer, Integer>>> capturedCoverageData = ExecutionTracer.getExecutionTracer().getTrace().getCoverageData();

        CoverageDataPool.getInstance().registerNewCoverageData(testName,capturedCoverageData);
//        ExecutionTracer.getExecutionTracer().disable();
    }
}
