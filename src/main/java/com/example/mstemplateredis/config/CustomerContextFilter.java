package com.example.mstemplateredis.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomerContextFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            // Extract customerId from request (e.g., header, parameter)
            String customerId = ((HttpServletRequest) request).getParameter("customerId");
            CustomerContextHolder.setCustomerId(customerId);

            chain.doFilter(request, response);
        } finally {
            // Clear after the ENTIRE request finishes (including exception handling)
            CustomerContextHolder.clear();
        }
    }
}