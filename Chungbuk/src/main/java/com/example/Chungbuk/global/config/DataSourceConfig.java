package com.example.Chungbuk.global.config;

import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class DataSourceConfig {

    private final MySqlDataSourceProperties mySqlDataSourceProperties;

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create()
                .driverClassName(mySqlDataSourceProperties.getDriverClassName())
                .url(mySqlDataSourceProperties.getJdbcUrl())
                .username(mySqlDataSourceProperties.getUsername())
                .password(mySqlDataSourceProperties.getPassword())
                .build();
    }
}
