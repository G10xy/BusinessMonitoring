package it.bm.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.CommonsRequestLoggingFilter;

import static it.bm.util.Constant.CORRELATION_ID_HEADER_NAME;

@Configuration
public class RequestLoggingConfig {

    @Bean
    public CommonsRequestLoggingFilter commonsRequestLoggingFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeClientInfo(true);
        filter.setIncludeQueryString(true);
        filter.setIncludeHeaders(true);
        filter.setIncludePayload(false);
        filter.setBeforeMessagePrefix("REQUEST DATA: ");
        return filter;
    }

    @Bean
    public FilterRegistrationBean<CommonsRequestLoggingFilter> requestLoggingFilterRegistration(
            CommonsRequestLoggingFilter filter) {
        FilterRegistrationBean<CommonsRequestLoggingFilter> reg = new FilterRegistrationBean<>(filter);
        reg.addUrlPatterns("/api/*");
        return reg;
    }

}

