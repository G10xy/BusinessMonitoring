package it.aruba.model;

import it.aruba.model.projection.AvgCustomerSpending;

import java.util.List;
import java.util.Map;

public record ReportSummaryResponse(
        Map<String, Long> activeByServiceType,
        List<AvgCustomerSpending> averageSpendingPerCustomer,
        List<String> customersWithMoreThanOneServiceExpired,
        List<String> customersWithServicesExpiringSoon
) {}
