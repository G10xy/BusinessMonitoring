package it.bm.config;

import it.bm.util.MDCUtil;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

import static it.bm.util.Constant.CORRELATION_ID_HEADER_NAME;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class MDCCorrelationIdFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String correlationId = httpRequest.getHeader(CORRELATION_ID_HEADER_NAME);

        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }

        MDCUtil.setCorrelationId(correlationId);
        try {
            chain.doFilter(request, response);
        }
        finally {
            MDCUtil.clearContext();
        }
    }
}