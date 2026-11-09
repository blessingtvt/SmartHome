package com.smarthome.environment.client;

import com.smarthome.environment.dto.EnvironmentReadingRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;

/**
 * S2 规则服务调用客户端。
 *
 * 阶段2 S2 尚未部署，调用采用「尽力而为」策略：任何异常（连接超时、拒绝、
 * S2 返回错误）都被捕获并降级为日志告警，绝不阻断 S1 的数据保存主流程。
 * 阶段5 S2 正式接入后，此处的容错仍保留，以保证 S1 在上游故障时可用。
 */
@Component
public class RuleClient {

    private static final Logger log = LoggerFactory.getLogger(RuleClient.class);

    @Value("${smart-home.rule-service.url}")
    private String ruleServiceUrl;

    private RestClient restClient;

    @PostConstruct
    public void init() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 短超时：S2 未就绪时快速失败降级，避免拖慢上报接口
        factory.setConnectTimeout(500);
        factory.setReadTimeout(500);
        this.restClient = RestClient.builder().requestFactory(factory).build();
    }

    /**
     * 尽力通知 S2 对一次上报数据进行规则评估。
     * 失败时仅记录日志，不影响主流程。
     */
    public void evaluate(EnvironmentReadingRequest reading) {
        try {
            restClient.post()
                    .uri(ruleServiceUrl + "/internal/rules/evaluate")
                    .body(reading)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("调用 S2 规则服务失败（已降级，不影响数据保存）: deviceCode={}, reason={}",
                    reading.getDeviceCode(), e.getMessage());
        }
    }
}
