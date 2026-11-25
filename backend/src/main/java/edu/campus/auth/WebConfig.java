package edu.campus.auth;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** 登录页由静态资源提供；/portal 重定向到门户页面，方便直接输入地址。 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addRedirectViewController("/portal", "/portal.html");
        registry.addRedirectViewController("/portal/", "/portal.html");
    }
}
