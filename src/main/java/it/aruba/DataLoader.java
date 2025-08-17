package it.aruba;

import it.aruba.model.entity.SubscriptionStatus;
import it.aruba.model.enums.SubscriptionStatusEnum;
import it.aruba.repository.SubscriptionStatusRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataLoader implements ApplicationRunner {

    private final SubscriptionStatusRepository subscriptionStatusRepository;


    @Override
    public void run(ApplicationArguments args) throws Exception {

        for (SubscriptionStatusEnum status : SubscriptionStatusEnum.values()) {
            if (!subscriptionStatusRepository.existsByCode(status)) {
                SubscriptionStatus subscriptionStatus = new SubscriptionStatus();
                subscriptionStatus.setCode(status);
                subscriptionStatusRepository.save(subscriptionStatus);
            }
        }
    }
}
