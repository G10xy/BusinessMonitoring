package it.bm.model.response;

import io.swagger.v3.oas.annotations.media.Schema;
import it.bm.model.projection.AvgCustomerSpending;

import java.util.List;
import java.util.Map;

public record ReportSummaryResponse(
        @Schema(description = "Map of active services by service type")
        Map<String, Long> activeByServiceType,
        @Schema(description = "List of average spending per customer")
        List<AvgCustomerSpending> averageSpendingPerCustomer,
        @Schema(description = "List of customers with more than one expired service")
        List<String> customersWithMoreThanOneServiceExpired,
        @Schema(description = "List of customers with expiring services within 15 days")
        List<String> customersWithServicesExpiringSoon
) {}
