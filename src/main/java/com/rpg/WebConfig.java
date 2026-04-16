package com.rpg;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Configures static file serving from the /web directory.
 * NOTE: Do NOT add @EnableWebMvc here — it disables Spring Boot auto-config.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Serve web/ directory contents (HTML, CSS, JS)
        String webDir = Paths.get("web").toAbsolutePath().toUri().toString();
        registry.addResourceHandler("/index.html", "/style.css", "/game.js", "/favicon.ico")
                .addResourceLocations(webDir)
                .setCachePeriod(0); // No cache during development
    }

    /**
     * Redirect bare "/" to "/index.html"
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest req, HttpServletResponse res, Object handler)
                    throws IOException {
                if ("/".equals(req.getRequestURI())) {
                    res.sendRedirect("/index.html");
                    return false;
                }
                return true;
            }
        });
    }
}
