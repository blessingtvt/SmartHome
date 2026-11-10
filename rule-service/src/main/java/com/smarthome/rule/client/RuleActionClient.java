package com.smarthome.rule.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

/**
 * 规则动作客户端（S2-04）：调用 S3 内部指令接口、S4 内部告警接口。
 *
 * <p>采用 RestClient（与 S1 RuleClient 一致）；响应反序列化为 JsonNode（rule-service
 * 不依赖 device/alarm 服务，故不引用其 VO）。任何失败统一抛 {@link ActionCallException}，
 * 由调用方记录为 execution_result=FAILED。</p>
 */
@Component
public class RuleActionClient {

    private static final Logger log = LoggerFactory.getLogger(RuleActionClient.class);

    /** S3 内部接口要求的来源（common 无此常量，本地定义，与 S3 保持一致） */
    private static final String SOURCE_AUTO_RULE = "AUTO_RULE";
    private static final int SUCCESS_CODE = 200;

    /** S3 手动控制保护拦截的业务码（与 device-service 的 MANUAL_PROTECTED_CODE 保持一致） */
    private static final int MANUAL_PROTECTED_CODE = 409;

    private final ObjectMapper json = new ObjectMapper();
    private final RestClient restClient;
    private final String deviceServiceUrl;
    private final String alarmServiceUrl;

    public RuleActionClient(
            @Value("${smart-home.device-service.url:http://127.0.0.1:8083}") String deviceServiceUrl,
            @Value("${smart-home.alarm-service.url:http://127.0.0.1:8084}") String alarmServiceUrl,
            @Value("${smart-home.action.connect-timeout-ms:500}") int connectTimeoutMs,
            @Value("${smart-home.action.read-timeout-ms:500}") int readTimeoutMs) {
        this.deviceServiceUrl = deviceServiceUrl;
        this.alarmServiceUrl = alarmServiceUrl;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeoutMs);
        factory.setReadTimeout(readTimeoutMs);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * 调用 S3 创建设备指令，成功返回指令 ID（供回填 device_command_id）。
     * 若被 S3 手动控制保护拦截（409），抛 {@link ManualProtectedException}，由调用方记 BLOCKED。
     */
    public Long createCommand(String deviceCode, String action) {
        JsonNode body = json.createObjectNode()
                .put("deviceCode", deviceCode)
                .put("action", action)
                .put("source", SOURCE_AUTO_RULE);
        JsonNode resp = post(deviceServiceUrl + "/internal/device-commands", body, "S3");
        int code = resp.path("code").asInt(-1);
        if (code == MANUAL_PROTECTED_CODE) {
            throw new ManualProtectedException(resp.path("message").asText("手动控制保护期内，自动指令已拦截"));
        }
        if (code != SUCCESS_CODE) {
            String message = resp.path("message").asText("无错误信息");
            throw new ActionCallException("S3 业务失败: " + message);
        }
        JsonNode idNode = resp.path("data").path("id");
        if (idNode.isMissingNode() || idNode.isNull() || !idNode.canConvertToLong()) {
            throw new ActionCallException("S3 响应缺少指令ID");
        }
        return idNode.asLong();
    }

    /**
     * 调用 S3 查询设备当前执行状态（INT03），供 PIR 开灯去重与自动关灯判断。
     * 解析 data 中的 on / onSource / pending 字段，返回本地 {@link DeviceStateResult}。
     */
    public DeviceStateResult getDeviceState(String deviceCode) {
        JsonNode resp = get(deviceServiceUrl + "/internal/devices/" + deviceCode + "/state", "S3");
        int code = resp.path("code").asInt(-1);
        if (code != SUCCESS_CODE) {
            String message = resp.path("message").asText("无错误信息");
            throw new ActionCallException("S3 设备状态查询失败: " + message);
        }
        JsonNode data = resp.path("data");
        DeviceStateResult result = new DeviceStateResult();
        if (!data.isMissingNode() && data.isObject()) {
            result.setOn(data.path("on").isNull() ? null : data.path("on").asBoolean());
            result.setOnSource(data.path("onSource").isNull() ? null : data.path("onSource").asText());
            result.setPending(data.path("pending").isNull() ? null : data.path("pending").asBoolean());
        }
        return result;
    }

    /**
     * 调用 S3 查询单条指令详情（INT04），返回指令状态（PENDING/SUCCESS/FAILED），
     * 供 S2 回填 device_exec_result。指令不存在或业务失败抛 {@link ActionCallException}。
     */
    public String getCommandStatus(Long commandId) {
        JsonNode resp = get(deviceServiceUrl + "/internal/device-commands/" + commandId, "S3");
        int code = resp.path("code").asInt(-1);
        if (code != SUCCESS_CODE) {
            String message = resp.path("message").asText("无错误信息");
            throw new ActionCallException("S3 指令查询失败: " + message);
        }
        JsonNode status = resp.path("data").path("status");
        if (status.isMissingNode() || status.isNull()) {
            throw new ActionCallException("S3 指令响应缺少状态");
        }
        return status.asText();
    }

    /**
     * 调用 S4 创建环境异常告警（类型由 S4 固定为 ENV_ABNORMAL，不在此传入）。
     */
    public void createAlarm(String deviceCode, String content) {
        JsonNode body = json.createObjectNode()
                .put("deviceCode", deviceCode)
                .put("content", content);
        JsonNode resp = post(alarmServiceUrl + "/internal/alarms", body, "S4");
        int code = resp.path("code").asInt(-1);
        if (code != SUCCESS_CODE) {
            String message = resp.path("message").asText("无错误信息");
            throw new ActionCallException("S4 业务失败: " + message);
        }
    }

    /** 发起 GET，校验 HTTP 层（网络/超时/空响应/非 JSON），返回响应 JSON（业务 code 由调用方判断） */
    private JsonNode get(String url, String target) {
        String respBody;
        try {
            respBody = restClient.get().uri(url).retrieve().body(String.class);
        } catch (ResourceAccessException e) {
            String message = classify(e, target);
            log.warn(message, e);
            throw new ActionCallException(message, e);
        } catch (RestClientException e) {
            String message = classify(e, target);
            log.warn(message, e);
            throw new ActionCallException(message, e);
        }
        if (respBody == null || respBody.isBlank()) {
            throw new ActionCallException(target + " 响应为空");
        }
        try {
            return json.readTree(respBody);
        } catch (Exception e) {
            throw new ActionCallException(target + " 响应不是合法 JSON", e);
        }
    }

    /** 发起 POST，校验 HTTP 层（网络/超时/空响应/非 JSON），返回响应 JSON（业务 code 由调用方判断） */
    private JsonNode post(String url, JsonNode body, String target) {
        String respBody;
        try {
            respBody = restClient.post().uri(url).body(body).retrieve().body(String.class);
        } catch (ResourceAccessException e) {
            String message = classify(e, target);
            log.warn(message, e);
            throw new ActionCallException(message, e);
        } catch (RestClientException e) {
            // 读超时可能以「响应体提取失败」形式出现（SimpleClientHttpRequestFactory 读超时）
            String message = classify(e, target);
            log.warn(message, e);
            throw new ActionCallException(message, e);
        }

        if (respBody == null || respBody.isBlank()) {
            throw new ActionCallException(target + " 响应为空");
        }
        JsonNode resp;
        try {
            resp = json.readTree(respBody);
        } catch (Exception e) {
            throw new ActionCallException(target + " 响应不是合法 JSON", e);
        }
        return resp;
    }

    /**
     * 统一区分失败类型：超时（结果不确定，远端可能已创建）、连接拒绝（确定失败）、
     * 其他 HTTP 异常。沿整个 cause 链查找 SocketTimeoutException / ConnectException。
     */
    private String classify(Throwable e, String target) {
        if (containsCause(e, SocketTimeoutException.class)) {
            return target + " 调用超时，结果不确定（远端可能已创建）";
        }
        if (containsCause(e, ConnectException.class)) {
            return target + " 连接失败: " + e.getMessage();
        }
        return target + " HTTP 调用失败: " + e.getMessage();
    }

    private boolean containsCause(Throwable t, Class<? extends Throwable> type) {
        Throwable cur = t;
        while (cur != null) {
            if (type.isInstance(cur)) {
                return true;
            }
            cur = cur.getCause();
        }
        return false;
    }
}
