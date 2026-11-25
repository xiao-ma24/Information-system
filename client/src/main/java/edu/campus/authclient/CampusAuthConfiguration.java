package edu.campus.authclient;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * 各组在启动类上加 @Import(CampusAuthConfiguration.class) 即可生效。
 */
@Configuration
@EnableConfigurationProperties(CampusAuthProperties.class)
public class CampusAuthConfiguration {

    @Bean
    public CampusAuthFilter campusAuthFilter(CampusAuthProperties props, ObjectMapper mapper) {
        return new CampusAuthFilter(props, mapper);
    }

    @Bean
    public FilterRegistrationBean<CampusAuthFilter> campusAuthFilterRegistration(CampusAuthFilter filter) {
        FilterRegistrationBean<CampusAuthFilter> registration = new FilterRegistrationBean<>(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }
}
