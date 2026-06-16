package com.example.Chungbuk.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "spring.datasource.mysql")
public class MySqlDataSourceProperties {

    private String driverClassName;
    private String jdbcUrl;
    private String username;
    private String password;
}
