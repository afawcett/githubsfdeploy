package com.andyinthecloud.githubsfdeploy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(OAuth2AuthenticationException.class)
    public ModelAndView handleOAuthError(OAuth2AuthenticationException ex, HttpServletRequest request) {
        logger.error("OAuth2AuthenticationException caught: {}", ex.getMessage(), ex);
        logger.error("Error Code: {}", ex.getError().getErrorCode());
        logger.error("Error Description: {}", ex.getError().getDescription());
        logger.error("Error URI: {}", ex.getError().getUri());
        logger.error("Request URL: {}", request.getRequestURL());
        logger.error("Request Parameters: {}", request.getParameterMap());
        
        ModelAndView mav = new ModelAndView("error");
        String errorMessage = "OAuth authentication error: " + ex.getMessage();
        mav.addObject("errorMessage", errorMessage);
        request.setAttribute("errorMessage", errorMessage);
        return mav;
    }

    @ExceptionHandler(AuthenticationException.class)
    public ModelAndView handleAuthenticationError(AuthenticationException ex, HttpServletRequest request) {
        logger.error("AuthenticationException caught: {}", ex.getMessage(), ex);
        logger.error("Request URL: {}", request.getRequestURL());
        logger.error("Request Parameters: {}", request.getParameterMap());
        logger.error("Request Headers: {}", request.getHeaderNames());
        
        ModelAndView mav = new ModelAndView("error");
        String errorMessage = "Authentication error: " + ex.getMessage();
        mav.addObject("errorMessage", errorMessage);
        request.setAttribute("errorMessage", errorMessage);
        return mav;
    }

    @ExceptionHandler(Exception.class)
    public ModelAndView handleGeneralError(Exception ex, HttpServletRequest request) {
        logger.error("General Exception caught: {}", ex.getMessage(), ex);
        ModelAndView mav = new ModelAndView("error");
        String errorMessage = "An unexpected error occurred: " + ex.getMessage();
        mav.addObject("errorMessage", errorMessage);
        request.setAttribute("errorMessage", errorMessage);
        return mav;
    }
} 