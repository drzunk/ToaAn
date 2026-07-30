package vn.tuphap.automation.core;

import vn.tuphap.automation.config.RunFlowConfig;

/**
 * Cấu hình chạy song song nhiều trình duyệt.
 * Nguồn chính: {@code run-flow.properties} ({@link RunFlowConfig}).
 */
public final class ParallelConfig {

    public static final int DEFAULT_PARALLEL_THREADS = 3;

    static {
        RunFlowConfig.applyKnownSystemAliases();
    }

    private ParallelConfig() {
    }

    public static int threadCount() {
        return RunFlowConfig.browsers();
    }

    public static boolean isParallel() {
        return RunFlowConfig.parallel() && threadCount() > 1;
    }
}
