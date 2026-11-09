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
     */
    public Long createCommand(String deviceCode, String action) {
        JsonNode body = json.createObjectNode()
                .put("deviceCode", deviceCode)
                .put("action", action)
                .put("source", SOURCE_AUTO_RULE);
        JsonNode resp = post(deviceServiceUrl + "/internal/device-commands", body, "S3");
        JsonNode idNode = resp.path("data").path("id");
        if (idNode.isMissingNode() || idNode.isNull() || !idNode.canConvertToLong()) {
            throw new ActionCallException("S3 响应缺少指令ID");
        }
        return idNode.asLong();
    }

    /**
     * 调用 S4 创建环境异常告警（类型由 S4 固定为 ENV_ABNORMAL，不在此传入）。
     */
    public void createAlarm(String deviceCode, String content) {
        JsonNode body = json.createObjectNode()
                .put("deviceCode", deviceCode)
                .put("content", content);
        post(alarmServiceUrl + "/internal/alarms", body, "S4");
    }

    /** 发起 POST，校验 HTTP 与业务 code，返回响应 JSON；失败抛 ActionCallException */
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

        JsonNode codeNode = resp.get("code");
        if (codeNode == null || !codeNode.canConvertToInt()) {
            throw new ActionCallException(target + " 响应格式异常（缺少 code）");
        }
        if (codeNode.asInt() != SUCCESS_CODE) {
            String message = resp.path("message").asText("无错误信息");
            throw new ActionCallException(target + " 业务失败: " + message);
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
