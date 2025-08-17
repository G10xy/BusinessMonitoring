package it.bm.service;

import it.bm.model.entity.SubscriptionStatus;
import it.bm.model.enums.SubscriptionStatusEnum;
import it.bm.repository.SubscriptionStatusRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SubscriptionStatusService {

    private final SubscriptionStatusRepository subscriptionStatusRepository;

    private Map<SubscriptionStatusEnum, SubscriptionStatus> cacheMap;

    @PostConstruct
    private void postConstruct() {
        cacheMap = subscriptionStatusRepository.findAll().stream()
                .collect(Collectors.toMap(SubscriptionStatus::getCode, Function.identity()));
    }

    public SubscriptionStatus findByCodeStatus(SubscriptionStatusEnum status) {
        var result = cacheMap.get(status);
        if (result == null) {
            result = subscriptionStatusRepository.findByCode(status).orElseThrow();
            cacheMap.put(status, result);
        }
        return result;
    }
}
