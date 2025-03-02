package com.andyinthecloud.githubsfdeploy.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class AppConfig implements WebMvcConfigurer {

    @Override
    public void addViewControllers(@NonNull ViewControllerRegistry registry) {
        registry.addViewController("/").setViewName("index");
        registry.addViewController("/login").setViewName("login");
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/favicon.ico")
            .addResourceLocations("classpath:/static/");
        registry.addResourceHandler("/resources/**")
            .addResourceLocations("/resources/");
        registry.addResourceHandler("/css/**")
            .addResourceLocations("/resources/css/");
        registry.addResourceHandler("/js/**")
            .addResourceLocations("/resources/js/");
        registry.addResourceHandler("/images/**")
            .addResourceLocations("/resources/images/");
        registry.addResourceHandler("/fonts/**")
            .addResourceLocations("/resources/assets/fonts/webfonts/");
        registry.addResourceHandler("/assets/**")
            .addResourceLocations("/resources/assets/");
    }
}
