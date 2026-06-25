package com.gymtracker.core.config;

import com.gymtracker.core.Service.TelegramAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class TelegramAuthFilter extends OncePerRequestFilter {
    public static final String TELEGRAM_USER_ID_ATTRIBUTE = "telegramUserId";
    private static final String INIT_DATA_HEADER = "X-Telegram-Init-Data";
    private static final Set<String> PUBLIC_GET_PATHS = Set.of(
            "/api/workouts/exercises",
            "/api/workouts/achievements/catalog",
            "/api/workouts/predefined"
    );

    private final TelegramAuthService telegramAuthService;
    private final boolean authEnabled;

    public TelegramAuthFilter(TelegramAuthService telegramAuthService,
                              @Value("${telegram.auth.enabled:true}") boolean authEnabled) {
        this.telegramAuthService = telegramAuthService;
        this.authEnabled = authEnabled;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!authEnabled) {
            return true;
        }

        String path = request.getRequestURI();
        if (!path.startsWith("/api/workouts")) {
            return true;
        }

        return "GET".equalsIgnoreCase(request.getMethod()) && PUBLIC_GET_PATHS.contains(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            Long telegramUserId = telegramAuthService.validateAndGetTelegramId(request.getHeader(INIT_DATA_HEADER));
            request.setAttribute(TELEGRAM_USER_ID_ATTRIBUTE, telegramUserId);
            rejectIfQueryUserIdWasSpoofed(request, telegramUserId);
            filterChain.doFilter(request, response);
        } catch (RuntimeException e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Telegram authentication failed");
        }
    }

    private void rejectIfQueryUserIdWasSpoofed(HttpServletRequest request, Long telegramUserId) {
        assertParamMatches(request, "telegramId", telegramUserId);
    }

    private void assertParamMatches(HttpServletRequest request, String paramName, Long telegramUserId) {
        String value = request.getParameter(paramName);
        if (value == null || value.isBlank()) {
            return;
        }

        if (!telegramUserId.equals(Long.parseLong(value))) {
            throw new RuntimeException("Telegram id mismatch");
        }
    }
}
