package vn.tuphap.automation.flow;

/**
 * Biểu mẫu chặn chuyển bước — hệ thống hiển thị thông báo lỗi/validate.
 * Đã ghi log và chụp ảnh trước khi ném.
 */
public final class StepBlockedException extends RuntimeException {

    private final int stepNumber;
    private final String stepName;
    private final String systemMessage;
    private final String screenshotBase64;

    public StepBlockedException(int stepNumber, String stepName, String systemMessage, String screenshotBase64) {
        super(buildMessage(stepNumber, stepName, systemMessage));
        this.stepNumber = stepNumber;
        this.stepName = stepName == null ? "" : stepName;
        this.systemMessage = systemMessage == null ? "" : systemMessage;
        this.screenshotBase64 = screenshotBase64;
    }

    private static String buildMessage(int stepNumber, String stepName, String systemMessage) {
        return "❌ Bước " + stepNumber + " — " + (stepName == null ? "" : stepName)
                + " — hệ thống báo lỗi: " + (systemMessage == null ? "" : systemMessage);
    }

    public int stepNumber() {
        return stepNumber;
    }

    public String stepName() {
        return stepName;
    }

    public String systemMessage() {
        return systemMessage;
    }

    public String screenshotBase64() {
        return screenshotBase64;
    }
}
