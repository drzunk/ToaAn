package vn.tuphap.automation.core;

import org.testng.IAlterSuiteListener;
import org.testng.xml.XmlSuite;
import vn.tuphap.automation.config.RunFlowConfig;

import java.util.List;

/**
 * Đồng bộ {@code thread-count} / {@code data-provider-thread-count} với
 * {@code run.browsers} trong {@code run-flow.properties} (và {@code run.slots}).
 */
public final class ParallelSuiteAdjuster implements IAlterSuiteListener {

    @Override
    public void alter(List<XmlSuite> suites) {
        RunFlowConfig.applyKnownSystemAliases();
        int n = RunFlowConfig.browsers();
        boolean parallel = RunFlowConfig.parallel() && n > 1;
        for (XmlSuite suite : suites) {
            if (parallel) {
                suite.setParallel(XmlSuite.ParallelMode.METHODS);
                suite.setThreadCount(n);
                suite.setDataProviderThreadCount(n);
                System.out.println("⚙ TestNG parallel: methods | threads=" + n
                        + " | suite=" + suite.getName());
            } else {
                suite.setParallel(XmlSuite.ParallelMode.NONE);
                suite.setThreadCount(1);
                suite.setDataProviderThreadCount(1);
            }
        }
    }
}
