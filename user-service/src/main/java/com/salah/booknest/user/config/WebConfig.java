package com.salah.booknest.user.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        java.io.File dir = new java.io.File("profile");
        String disk = "file:" + dir.getAbsolutePath().replace("\\", "/") + "/";
        log.info("Serving profile images from: {}", disk);

        registry.addResourceHandler("/profile/**")
                .addResourceLocations(disk, "classpath:/static/profile/");
    }
}