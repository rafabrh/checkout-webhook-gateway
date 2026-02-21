package com.shkgroups.shared.exception;

import com.shkgroups.provisioning.ProvisioningService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.Objects;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ProvisioningService.ProvisioningException.class)
    public ProblemDetail provisioning(ProvisioningService.ProvisioningException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatus(ex.getHttpStatus());
        pd.setTitle("Provisioning Error");
        pd.setDetail(ex.getCode());
        pd.setProperty("error", "provisioning_error");
        pd.setProperty("path", req.getRequestURI());
        return pd;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail notReadable(HttpMessageNotReadableException ex, HttpServletRequest req) {
        Throwable root = rootCause(ex);

        if (root instanceof IllegalArgumentException iae) {
            String code = Objects.toString(iae.getMessage(), "");

            if ("invalid_plan".equals(code) || "plan_required".equals(code)) {
                var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
                pd.setTitle("Validation Error");
                pd.setDetail(code);
                pd.setProperty("error", "validation_error");
                pd.setProperty("path", req.getRequestURI());
                return pd;
            }
        }

        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Bad Request");
        pd.setDetail("invalid_json");
        pd.setProperty("error", "bad_request");
        pd.setProperty("path", req.getRequestURI());
        return pd;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badRequest(IllegalArgumentException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Bad Request");
        pd.setDetail(ex.getMessage());
        pd.setProperty("error", "bad_request");
        pd.setProperty("path", req.getRequestURI());
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException ex, HttpServletRequest req) {
        var pd = ProblemDetail.forStatus(HttpStatus.BAD_REQUEST);
        pd.setTitle("Validation Error");
        pd.setDetail(ex.getBindingResult().getAllErrors().stream()
                .findFirst().map(e -> e.getDefaultMessage()).orElse("invalid"));
        pd.setProperty("error", "validation_error");
        pd.setProperty("path", req.getRequestURI());
        return pd;
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ProblemDetail webClient(WebClientResponseException ex, HttpServletRequest req) {
        log.error("Upstream HTTP error on {} {} -> status={} body={}",
                req.getMethod(), req.getRequestURI(), ex.getStatusCode().value(),
                safeBody(ex.getResponseBodyAsString()), ex);

        var pd = ProblemDetail.forStatus(HttpStatus.BAD_GATEWAY);
        pd.setTitle("Upstream Error");
        pd.setDetail("upstream_error");
        pd.setProperty("error", "upstream_error");
        pd.setProperty("path", req.getRequestURI());
        pd.setProperty("upstreamStatus", ex.getStatusCode().value());
        pd.setProperty("upstreamBody", safeBody(ex.getResponseBodyAsString()));
        pd.setProperty("exception", ex.getClass().getName());
        return pd;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail generic(Exception ex, HttpServletRequest req) {
        log.error("Unhandled error on {} {}", req.getMethod(), req.getRequestURI(), ex);

        var pd = ProblemDetail.forStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        pd.setTitle("Internal Error");
        pd.setDetail("unexpected_error");
        pd.setProperty("error", "internal_error");
        pd.setProperty("path", req.getRequestURI());
        pd.setProperty("exception", ex.getClass().getName());


        return pd;
    }

    private Throwable rootCause(Throwable t) {
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur;
    }

    private String safeBody(String body) {
        if (body == null) return null;
        body = body.trim();
        if (body.length() > 800) return body.substring(0, 800) + "...(truncated)";
        return body;
    }
}