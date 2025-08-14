package it.aruba.repository;

import it.aruba.model.entity.CustomerServiceSubscriptions;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CustomerServiceSubscriptionsRepository extends JpaRepository<CustomerServiceSubscriptions, Long> {
}
