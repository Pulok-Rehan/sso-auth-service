//package com.bracepl.reporting_service.configs;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.CorsRegistry;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
//
//@Configuration
//public class GlobalCorsConfig {
//
//    @Bean
//    public WebMvcConfigurer corsConfigurer() {
//        return new WebMvcConfigurer() {
//            @Override
//            public void addCorsMappings(CorsRegistry registry) {
//                registry.addMapping("/**") // allow CORS for all endpoints
//                        .allowedOrigins("*") // allow all origins
//                        .allowedMethods("*") // allow GET, POST, PUT, DELETE etc.
//                        .allowedHeaders("*") // allow all headers
//                        .allowCredentials(false); // credentials not allowed with *
//            }
//        };
//    }
//}
