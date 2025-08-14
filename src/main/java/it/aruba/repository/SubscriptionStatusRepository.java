package it.aruba.repository;

import it.aruba.model.entity.SubscriptionStatus;
import it.aruba.model.enums.SubscriptionStatusEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;


@Repository
public interface SubscriptionStatusRepository extends JpaRepository<SubscriptionStatus, Long> {

    Optional<SubscriptionStatus> findByCode(SubscriptionStatusEnum status);

    boolean existsByCode(SubscriptionStatusEnum code);
}