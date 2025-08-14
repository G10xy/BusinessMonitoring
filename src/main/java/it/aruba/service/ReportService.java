package it.aruba.service;

import it.aruba.model.entity.CustomerServiceSubscriptions;
import it.aruba.model.enums.SubscriptionStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    @Value("${expired.services.limit:5}")
    private int expiredServicesLimit;
    @Value("${years.subscription.limit:3}")
    private int yearsOfSubscription;

    private final FileValidationService fileValidationService;
    private final FileParseService fileParseService;
    private final CustomerServiceSubscriptionService customerServiceSubscriptionService;

    public void createReport(MultipartFile file) throws IOException {
        fileValidationService.validateCsvFile(file);
        List<CustomerServiceSubscriptions> records = fileParseService.parseCsvRecordToEntity(file);
        this.checkForExpiredServices(records, expiredServicesLimit);
        this.checkForSubscriptionLength(records, yearsOfSubscription);
        customerServiceSubscriptionService.saveAll(records);
    }


    private void checkForExpiredServices(List<CustomerServiceSubscriptions> records, int expiredServicesLimit) {
        Map<String, Long> expiredCountByCustomer = records.stream()
                .filter(record -> record.getStatus().getCode() == SubscriptionStatusEnum.EXPIRED)
                .collect(Collectors.groupingBy(CustomerServiceSubscriptions::getCustomerId, Collectors.counting()));

        expiredCountByCustomer.forEach((customerId, count) -> {
            if (count > expiredServicesLimit) {
                log.info("Alert: Customer {} has {} expired services.", customerId, count);
            }
        });
    }

    private void checkForSubscriptionLength(List<CustomerServiceSubscriptions> records, int yearsOfSubscription) {
        LocalDate threeYearsAgo = LocalDate.now().minusYears(yearsOfSubscription);
        records.forEach(record -> {
            if ((record.getStatus().getCode() == SubscriptionStatusEnum.ACTIVE || record.getStatus().getCode() == SubscriptionStatusEnum.PENDING_RENEWAL)
                    && record.getActivationDate().isBefore(threeYearsAgo)) {
                log.info("Alert: Upsell opportunity for customer {} about service {}", record.getCustomerId(), record.getServiceType());
            }
        });
    }

}
