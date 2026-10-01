package com.salah.booknest.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        java.io.File dir = new java.io.File("profile");
        String disk = "file:" + dir.getAbsolutePath().replace("\\", "/") + "/";
        System.out.println("Serving profile images from: " + disk);

        registry.addResourceHandler("/profile/**")
                .addResourceLocations(disk, "classpath:/static/profile/");
    }
}