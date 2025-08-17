package it.bm.service;

import it.bm.model.entity.CustomerServiceSubscriptions;
import it.bm.model.enums.SubscriptionStatusEnum;
import it.bm.model.projection.AvgCustomerSpending;
import it.bm.model.projection.ServiceTypeCount;
import it.bm.repository.CustomerServiceSubscriptionsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerServiceSubscriptionService {

    private final CustomerServiceSubscriptionsRepository  customerServiceSubscriptionsRepository;

    @Transactional
    public void saveAll(List<CustomerServiceSubscriptions> list) {
        customerServiceSubscriptionsRepository.saveAll(list);
    }

    @Transactional(readOnly = true)
    public List<ServiceTypeCount> countServicesByTypeWithStatus(List<SubscriptionStatusEnum> statuses) {
        return customerServiceSubscriptionsRepository.countServicesByTypeWithStatus(statuses);
    }

    @Transactional(readOnly = true)
    public List<AvgCustomerSpending> averageSpendingPerCustomer() {
        return customerServiceSubscriptionsRepository.averageSpendingPerCustomer();
    }

    @Transactional(readOnly = true)
    public List<String> findCustomersWithMultipleExpiredServices(SubscriptionStatusEnum status, Integer limitCount) {
        return customerServiceSubscriptionsRepository.findCustomersWithMultipleExpiredServices(status, limitCount == null ? 1 : limitCount);
    }

    @Transactional(readOnly = true)
    public List<String> findCustomersWithServicesExpiringWithinDays(List<SubscriptionStatusEnum> statuses, LocalDate futureDate) {
        return customerServiceSubscriptionsRepository.findCustomersWithServicesExpiringWithinDays(statuses, futureDate);
    }
}
