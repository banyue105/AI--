package com.ican.assistant.modules.decisionsandbox;

public class DecisionCalculationException extends RuntimeException {
    public DecisionCalculationException(Throwable cause) {
        super("方案计算失败，请核对条件后重试。", cause);
    }
}
