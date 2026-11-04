package ru.practicum.blog.config;

import jakarta.servlet.MultipartConfigElement;
import jakarta.servlet.ServletRegistration;
import org.springframework.web.servlet.support.AbstractAnnotationConfigDispatcherServletInitializer;

public class WebInitializer extends AbstractAnnotationConfigDispatcherServletInitializer {
    @Override protected Class<?>[] getRootConfigClasses() { return new Class<?>[]{AppConfig.class}; }
    @Override protected Class<?>[] getServletConfigClasses() { return new Class<?>[]{WebConfig.class}; }
    @Override protected String[] getServletMappings() { return new String[]{"/"}; }

    @Override protected void customizeRegistration(ServletRegistration.Dynamic registration) {
        registration.setMultipartConfig(new MultipartConfigElement("", 10 * 1024 * 1024, 12 * 1024 * 1024, 0));
    }
}
