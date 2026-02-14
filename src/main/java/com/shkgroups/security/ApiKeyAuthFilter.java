package com.shkgroups.security;

import com.shkgroups.config.ApiKeyProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    private static final String ROLE = "ROLE_SERVICE";

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

        if (matcher.match("/pair/**", path)) return true;
        if (matcher.match("/actuator/health", path) || matcher.match("/actuator/info", path)) return true;
        if (matcher.match("/v1/payments/mercadopago/notification", path)) return true;

        return !matcher.match("/v1/**", path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        String expected = props.key();
        String headerName = props.header();

        if (expected == null || expected.isBlank()) {
            res.setStatus(500);
            res.setContentType(MediaType.TEXT_PLAIN_VALUE);
            res.getWriter().write("API key not configured");
            return;
        }

        String got = req.getHeader(headerName);

        boolean ok = got != null && MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                got.getBytes(StandardCharsets.UTF_8)
        );

        if (!ok) {
            res.setStatus(401);
            res.setContentType(MediaType.TEXT_PLAIN_VALUE);
            res.getWriter().write("Unauthorized");
            return;
        }

        var auth = new UsernamePasswordAuthenticationToken(
                "service", null, List.of(new SimpleGrantedAuthority(ROLE))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(req, res);
    }
}
