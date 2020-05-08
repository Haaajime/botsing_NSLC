package eu.stamp.botsing.fitnessfunction;

import eu.stamp.botsing.StackTrace;
import eu.stamp.botsing.fitnessfunction.calculator.CrashCoverageFitnessCalculator;
import eu.stamp.botsing.fitnessfunction.utils.CrashDistanceEvolution;
import org.evosuite.coverage.exception.ExceptionCoverageHelper;
import org.evosuite.testcase.TestChromosome;
import org.evosuite.testcase.TestFitnessFunction;
import org.evosuite.testcase.execution.ExecutionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Resource;

public class IntegrationTestingFF extends TestFitnessFunction {
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTestingFF.class);
    @Resource
    CrashCoverageFitnessCalculator fitnessCalculator;
    protected StackTrace targetCrash;

    public IntegrationTestingFF(StackTrace crash){
        fitnessCalculator = new CrashCoverageFitnessCalculator(crash);
        targetCrash = crash;
    }
    @Override
    public double getFitness(TestChromosome testChromosome, ExecutionResult executionResult) {
        if(fitnessCalculator.sameException(executionResult)){
            return 0;
        }

        int targetFrame = targetCrash.getPublicTargetFrameLevel();
        double fitnessValue=0;
        boolean covering = true;
        for(int frameLevel = targetFrame; frameLevel > 0 ; frameLevel--){
            if(covering){
                double lineCoverageFitness = fitnessCalculator.getLineCoverageForFrame(executionResult,frameLevel);
                if(lineCoverageFitness != 0){
                    fitnessValue = lineCoverageFitness;
                    covering=false;
                }
            }else{
                fitnessValue++;
            }
        }

        if(fitnessValue == 0.0){
            // We have reached to the deepest frame target line. So, need to check if the target exception has been thrown.
            fitnessValue = exceptionCoverage(executionResult);
        }else {
            // We have not reached to the deepest frame target line. So, we set the target exception to 1 as the penalty.
            fitnessValue++;
        }

//        double fitnessValue = lineCoverageFitness;
        LOG.debug("Fitness Function: "+fitnessValue);
        testChromosome.setFitness(this,fitnessValue);
        testChromosome.increaseNumberOfEvaluations();
        CrashDistanceEvolution.getInstance().inform(fitnessValue);
        return fitnessValue;
    }

    protected double exceptionCoverage(ExecutionResult executionResult) {
        double exceptionCoverage = 1.0;
        for (Integer exceptionLocator : executionResult.getPositionsWhereExceptionsWereThrown()) {
            Throwable resultException = executionResult.getExceptionThrownAtPosition(exceptionLocator);
            if(resultException.getStackTrace().length == 0 ){
                continue;
            }

            int frame = -1;
            String crashingClass;
            do {
                crashingClass = resultException.getStackTrace()[++frame].getClassName();
            }
            while (frame < resultException.getStackTrace().length && (crashingClass.startsWith("java.") || crashingClass.startsWith("javax.")));
            if (frame == resultException.getStackTrace().length) {
                continue;
            }

            int crashingLine = resultException.getStackTrace()[frame].getLineNumber();
            if(targetCrash.getFrame(1).getLineNumber() != crashingLine || !targetCrash.getFrame(1).getClassName().equals(crashingClass)){
                continue;
            }
            String thrownException = ExceptionCoverageHelper.getExceptionClass(executionResult, exceptionLocator).getName();
            if (thrownException.equals(targetCrash.getExceptionType())){
                exceptionCoverage = 0.0;
                break;
            }
        }
        return exceptionCoverage;
    }

    @Override
    public int compareTo(TestFitnessFunction testFitnessFunction) {
        return 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (targetCrash.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (this == obj) {
            return true;
        }
        return getClass() == obj.getClass();
    }

    @Override
    public String getTargetClass() {
        return targetCrash.getFrame(1).getClassName();
    }

    @Override
    public String getTargetMethod() {
        return targetCrash.getFrame(1).getMethodName();
    }
}
