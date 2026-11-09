package com.smarthome.common.constant;

/**
 * 跨服务共享的常量：统一的状态枚举、动作枚举、告警类型等。
 * 这些值被 S2/S3/S4 共同使用，放在 common 中保证各服务取值一致。
 */
public final class CommonConstant {

    private CommonConstant() {
    }

    /** 统一成功状态码 */
    public static final int SUCCESS_CODE = 200;

    /** 设备指令状态 */
    public static final String COMMAND_PENDING = "PENDING";
    public static final String COMMAND_SUCCESS = "SUCCESS";
    public static final String COMMAND_FAILED = "FAILED";

    /** 告警状态 */
    public static final String ALARM_STATUS_PENDING = "PENDING";
    public static final String ALARM_STATUS_PROCESSING = "PROCESSING";
    public static final String ALARM_STATUS_PROCESSED = "PROCESSED";

    /** 告警类型 */
    public static final String ALARM_ENV_ABNORMAL = "ENV_ABNORMAL";
    public static final String ALARM_MANUAL_HELP = "MANUAL_HELP";

    /** 设备控制动作（统一枚举，前后端/硬件一致） */
    public static final String ACTION_TURN_ON = "TURN_ON";
    public static final String ACTION_TURN_OFF = "TURN_OFF";
    public static final String ACTION_SET_LEVEL = "SET_LEVEL";

    /** 指令来源 */
    public static final String SOURCE_RULE = "RULE";
    public static final String SOURCE_REMOTE = "REMOTE";
}
