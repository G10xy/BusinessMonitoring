package it.aruba.service;

import it.aruba.model.entity.CustomerServiceSubscriptions;
import it.aruba.repository.CustomerServiceSubscriptionsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomerServiceSubscriptionService {

    private final CustomerServiceSubscriptionsRepository  customerServiceSubscriptionsRepository;
    private final SubscriptionStatusService subscriptionStatusService;

    public CustomerServiceSubscriptionService(CustomerServiceSubscriptionsRepository customerServiceSubscriptionsRepository, SubscriptionStatusService subscriptionStatusService) {
        this.customerServiceSubscriptionsRepository = customerServiceSubscriptionsRepository;
        this.subscriptionStatusService = subscriptionStatusService;
    }

    @Transactional
    public void saveAll(List<CustomerServiceSubscriptions> list) {
        customerServiceSubscriptionsRepository.saveAll(list);
    }
}
