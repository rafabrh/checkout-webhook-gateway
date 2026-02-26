package com.shkgroups.security;

import com.shkgroups.config.properties.ApiKeyProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
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
    private final Environment env;
    private final AntPathMatcher matcher = new AntPathMatcher();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        if (!props.enabled()) return true;

        String path = request.getRequestURI();
        String ctx = request.getContextPath();

        if (ctx != null && !ctx.isBlank() && path.startsWith(ctx)) {
            path = path.substring(ctx.length());
        }

        boolean isDev = env.acceptsProfiles(Profiles.of("dev"));
        boolean swaggerAllowed = isDev && (
                matcher.match("/swagger-ui/**", path)
                        || matcher.match("/swagger-ui.html", path)
                        || matcher.match("/v3/api-docs/**", path)
        );

        boolean actuatorAllowed =
                matcher.match("/actuator/health", path) ||
                matcher.match("/actuator/info", path);

        boolean pairingAllowed = matcher.match("/pair/**", path);

        boolean mpWebhookAllowed =
                matcher.match("/v1/payments/mercadopago/notification", path)
                        && ("POST".equalsIgnoreCase(request.getMethod())
                        || "GET".equalsIgnoreCase(request.getMethod()));

        return swaggerAllowed || actuatorAllowed || pairingAllowed || mpWebhookAllowed;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        String expected = props.key();
        String headerName = props.header();

        if (expected == null || expected.isBlank()) {
            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
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
            res.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            res.setContentType(MediaType.TEXT_PLAIN_VALUE);
            res.getWriter().write("Unauthorized");
            return;
        }

        var auth = new UsernamePasswordAuthenticationToken(
                "service",
                null,
                List.of(new SimpleGrantedAuthority(ROLE))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        try {
            chain.doFilter(req, res);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
