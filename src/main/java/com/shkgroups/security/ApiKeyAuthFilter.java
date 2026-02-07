package com.shkgroups.security;

import com.shkgroups.config.ApiKeyProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private final ApiKeyProperties props;
    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!props.enabled()) return true;

        String path = request.getRequestURI();
        String ctx = request.getContextPath();
        if (ctx != null && !ctx.isBlank() && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }

        // públicos
        if (matcher.match("/pair/**", path) || matcher.match("/public/**", path) || matcher.match("/dev/**", path)) return true;
        if (matcher.match("/webhooks/**", path)) return true; // IMPORTANTE p/ Mercado Pago
        if (matcher.match("/actuator/health", path) || matcher.match("/actuator/info", path)) return true;

        // protege /v1 e actuator
        return !(matcher.match("/v1/**", path) || matcher.match("/actuator/**", path));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        String expected = props.key();
        String got = req.getHeader(props.header());

        if (expected == null || expected.isBlank()) {
            res.setStatus(500);
            res.setContentType("text/plain;charset=UTF-8");
            res.getWriter().write("API key not configured");
            return;
        }

        if (got == null || !MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), got.getBytes(StandardCharsets.UTF_8))) {
            res.setStatus(401);
            res.setContentType("text/plain;charset=UTF-8");
            res.getWriter().write("Unauthorized");
            return;
        }

        chain.doFilter(req, res);
    }
}