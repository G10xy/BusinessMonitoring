package it.bm.repository;

import it.bm.model.entity.CustomerServiceSubscriptions;
import it.bm.model.enums.SubscriptionStatusEnum;
import it.bm.model.projection.AvgCustomerSpending;
import it.bm.model.projection.ServiceTypeCount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CustomerServiceSubscriptionsRepository extends JpaRepository<CustomerServiceSubscriptions, Long> {

    @Query("""
            SELECT new it.bm.model.projection.ServiceTypeCount(c.serviceType, COUNT(c))
            FROM CustomerServiceSubscriptions c
            JOIN c.status s
            WHERE s.code in :statuses 
            GROUP BY c.serviceType
        """)
    List<ServiceTypeCount> countServicesByTypeWithStatus(@Param("statuses") List<SubscriptionStatusEnum> activeStatus);

    @Query("""
        SELECT new it.bm.model.projection.AvgCustomerSpending(c.customerId, avg(c.amount))
        FROM CustomerServiceSubscriptions c
        GROUP BY c.customerId
        """)
    List<AvgCustomerSpending> averageSpendingPerCustomer();

    @Query("""
            SELECT c.customerId
            FROM CustomerServiceSubscriptions c 
            JOIN c.status s 
            WHERE s.code = :status 
            GROUP BY c.customerId 
            HAVING COUNT(c) > :limitCount
        """)
    List<String> findCustomersWithMultipleExpiredServices(@Param("status") SubscriptionStatusEnum status,
                                                          @Param("limitCount") Integer limitCount);

    @Query("""
            SELECT DISTINCT c.customerId 
            FROM CustomerServiceSubscriptions c 
            JOIN c.status s 
            WHERE s.code in :statuses 
            AND c.expirationDate BETWEEN CURRENT_DATE AND :futureDate
        """)
    List<String> findCustomersWithServicesExpiringWithinDays(
            @Param("statuses") List<SubscriptionStatusEnum> activeStatus,
            @Param("futureDate") LocalDate futureDate);
}
