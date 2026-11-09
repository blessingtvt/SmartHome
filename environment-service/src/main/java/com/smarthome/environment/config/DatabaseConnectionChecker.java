package com.smarthome.environment.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动时校验数据库连接（阶段1 验收：至少一个服务能成功连接 MySQL）。
 * 连接失败会抛异常导致启动失败，便于快速定位配置问题。
 */
@Component
public class DatabaseConnectionChecker implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionChecker.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseConnectionChecker(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        log.info("数据库连接成功，SELECT 1 结果: {}", result);
    }
}
