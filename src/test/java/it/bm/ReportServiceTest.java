package it.bm;

import it.bm.model.entity.CustomerServiceSubscriptions;
import it.bm.model.entity.SubscriptionStatus;
import it.bm.model.enums.SubscriptionStatusEnum;
import it.bm.model.kafka.ExpiredServicesDTO;
import it.bm.model.kafka.UpsellingServiceDTO;
import it.bm.model.projection.AvgCustomerSpending;
import it.bm.model.projection.ServiceTypeCount;
import it.bm.model.response.ReportSummaryResponse;
import it.bm.service.CustomerServiceSubscriptionService;
import it.bm.service.FileParseService;
import it.bm.service.FileValidationService;
import it.bm.service.NotificationService;
import it.bm.service.ReportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private FileValidationService fileValidationService;

    @Mock
    private FileParseService fileParseService;

    @Mock
    private CustomerServiceSubscriptionService customerServiceSubscriptionService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private MultipartFile multipartFile;

    private ReportService reportService;

    @BeforeEach
    void setUp() {
        reportService = new ReportService(fileValidationService, fileParseService,
                customerServiceSubscriptionService, notificationService);

        ReflectionTestUtils.setField(reportService, "expiredServicesLimit", 5);
        ReflectionTestUtils.setField(reportService, "yearsOfSubscription", 3);
    }

    private CustomerServiceSubscriptions createSubscription(String customerId, String serviceType,
                                                            SubscriptionStatusEnum status, LocalDate activationDate) {
        CustomerServiceSubscriptions subscription = new CustomerServiceSubscriptions();
        subscription.setCustomerId(customerId);
        subscription.setServiceType(serviceType);
        subscription.setActivationDate(activationDate);
        subscription.setExpirationDate(activationDate.plusYears(1));
        subscription.setAmount(new BigDecimal("50.00"));

        SubscriptionStatus subscriptionStatus = new SubscriptionStatus(status);
        subscription.setStatus(subscriptionStatus);

        return subscription;
    }

    @Test
    void createReport_Success() throws IOException {
        List<CustomerServiceSubscriptions> parsedRecords = Arrays.asList(
                createSubscription("C001", "hosting", SubscriptionStatusEnum.ACTIVE, LocalDate.now().minusYears(1)),
                createSubscription("C002", "email", SubscriptionStatusEnum.EXPIRED, LocalDate.now().minusYears(2))
        );

        when(fileParseService.parseCsvRecordToEntity(multipartFile)).thenReturn(parsedRecords);
        reportService.createReport(multipartFile);

        verify(fileValidationService).validateCsvFile(multipartFile);
        verify(fileParseService).parseCsvRecordToEntity(multipartFile);
        verify(customerServiceSubscriptionService).saveAll(parsedRecords);
    }

    @Test
    void createReport_FileValidation_ThrowsException() throws IOException {
        IOException validationException = new IOException("Invalid file format");
        doThrow(validationException).when(fileValidationService).validateCsvFile(multipartFile);

        IOException thrownException = assertThrows(IOException.class, () ->
                reportService.createReport(multipartFile));

        assertEquals("Invalid file format", thrownException.getMessage());
        verify(fileValidationService).validateCsvFile(multipartFile);
        verifyNoInteractions(fileParseService);
        verifyNoInteractions(customerServiceSubscriptionService);
    }

    @Test
    void createReport_FileParse_ThrowsException() throws IOException {
        RuntimeException parseException = new RuntimeException("Parse error");
        when(fileParseService.parseCsvRecordToEntity(multipartFile)).thenThrow(parseException);

        RuntimeException thrownException = assertThrows(RuntimeException.class, () ->
                reportService.createReport(multipartFile));

        assertEquals("Parse error", thrownException.getMessage());
        verify(fileValidationService).validateCsvFile(multipartFile);
        verify(fileParseService).parseCsvRecordToEntity(multipartFile);
        verifyNoInteractions(customerServiceSubscriptionService);
    }

    @Test
    void createReport_TriggersExpiredServicesNotification() throws IOException {
        ReflectionTestUtils.setField(reportService, "expiredServicesLimit", 2);

        List<CustomerServiceSubscriptions> expiredRecords = Arrays.asList(
                createSubscription("C001", "hosting", SubscriptionStatusEnum.EXPIRED, LocalDate.now()),
                createSubscription("C001", "email", SubscriptionStatusEnum.EXPIRED, LocalDate.now()),
                createSubscription("C001", "domain", SubscriptionStatusEnum.EXPIRED, LocalDate.now())
        );

        when(fileParseService.parseCsvRecordToEntity(multipartFile)).thenReturn(expiredRecords);

        reportService.createReport(multipartFile);

        verify(notificationService).sendExpiredServicesNotification(
                argThat(dto -> "C001".equals(dto.customerId()) && dto.numberOfExpiredServices() == 3L));
    }

    @Test
    void createReport_DoesNotTriggerExpiredServicesNotificationWhenBelowLimit() throws IOException {
        ReflectionTestUtils.setField(reportService, "expiredServicesLimit", 5);

        List<CustomerServiceSubscriptions> expiredRecords = Arrays.asList(
                createSubscription("C001", "hosting", SubscriptionStatusEnum.EXPIRED, LocalDate.now()),
                createSubscription("C001", "email", SubscriptionStatusEnum.EXPIRED, LocalDate.now())
        );

        when(fileParseService.parseCsvRecordToEntity(multipartFile)).thenReturn(expiredRecords);

        reportService.createReport(multipartFile);

        verify(notificationService, never()).sendExpiredServicesNotification(any());
    }

    @Test
    void createReport_TriggersUpsellingNotification() throws IOException {
        ReflectionTestUtils.setField(reportService, "yearsOfSubscription", 3);
        LocalDate fourYearsAgo = LocalDate.now().minusYears(4);

        List<CustomerServiceSubscriptions> oldSubscriptions = Arrays.asList(
                createSubscription("C001", "hosting", SubscriptionStatusEnum.ACTIVE, fourYearsAgo),
                createSubscription("C002", "email", SubscriptionStatusEnum.PENDING_RENEWAL, fourYearsAgo)
        );

        when(fileParseService.parseCsvRecordToEntity(multipartFile)).thenReturn(oldSubscriptions);

        reportService.createReport(multipartFile);

        verify(notificationService).sendUpsellingNotification(
                argThat(dto -> "C001".equals(dto.customerId()) && "hosting".equals(dto.upsellingService())));
        verify(notificationService).sendUpsellingNotification(
                argThat(dto -> "C002".equals(dto.customerId()) && "email".equals(dto.upsellingService())));
    }

    @Test
    void createReport_DoesNotTriggerUpsellingForExpiredSubscriptions() throws IOException {
        LocalDate fourYearsAgo = LocalDate.now().minusYears(4);

        List<CustomerServiceSubscriptions> expiredSubscriptions = List.of(
                createSubscription("C001", "hosting", SubscriptionStatusEnum.EXPIRED, fourYearsAgo)
        );

        when(fileParseService.parseCsvRecordToEntity(multipartFile)).thenReturn(expiredSubscriptions);
        reportService.createReport(multipartFile);
        verify(notificationService, never()).sendUpsellingNotification(any());
    }

    @Test
    void getReportSummary_Success() {
        List<ServiceTypeCount> serviceTypeCounts = Arrays.asList(
                new ServiceTypeCount("hosting", 10L),
                new ServiceTypeCount("email", 15L)
        );

        List<AvgCustomerSpending> avgSpending = Arrays.asList(
                new AvgCustomerSpending("C001", new BigDecimal("100.50")),
                new AvgCustomerSpending("C002", new BigDecimal("75.25"))
        );

        List<String> expiredCustomers = Arrays.asList("C003", "C004");
        List<String> expiringCustomers = Arrays.asList("C005", "C006");

        when(customerServiceSubscriptionService.countServicesByTypeWithStatus(any())).thenReturn(serviceTypeCounts);
        when(customerServiceSubscriptionService.averageSpendingPerCustomer()).thenReturn(avgSpending);
        when(customerServiceSubscriptionService.findCustomersWithMultipleExpiredServices(any(), any())).thenReturn(expiredCustomers);
        when(customerServiceSubscriptionService.findCustomersWithServicesExpiringWithinDays(any(), any())).thenReturn(expiringCustomers);

        ReportSummaryResponse response = reportService.getReportSummary();
        assertNotNull(response);

        Map<String, Long> activeByType = response.activeByServiceType();
        assertEquals(2, activeByType.size());
        assertEquals(10L, activeByType.get("hosting"));
        assertEquals(15L, activeByType.get("email"));

        assertEquals(2, response.averageSpendingPerCustomer().size());
        assertEquals(new BigDecimal("100.50"), response.averageSpendingPerCustomer().get(0).avgAmount());

        assertEquals(Arrays.asList("C003", "C004"), response.customersWithMoreThanOneServiceExpired());
        assertEquals(Arrays.asList("C005", "C006"), response.customersWithServicesExpiringSoon());
    }

    @Test
    void createReport_MultipleCustomersWithExpiredServices() throws IOException {
        ReflectionTestUtils.setField(reportService, "expiredServicesLimit", 1);

        List<CustomerServiceSubscriptions> mixedRecords = Arrays.asList(
                createSubscription("C001", "hosting", SubscriptionStatusEnum.EXPIRED, LocalDate.now()),
                createSubscription("C001", "email", SubscriptionStatusEnum.EXPIRED, LocalDate.now()),
                createSubscription("C002", "hosting", SubscriptionStatusEnum.EXPIRED, LocalDate.now()),
                createSubscription("C002", "pec", SubscriptionStatusEnum.EXPIRED, LocalDate.now()),
                createSubscription("C002", "spid", SubscriptionStatusEnum.EXPIRED, LocalDate.now()),
                createSubscription("C003", "hosting", SubscriptionStatusEnum.ACTIVE, LocalDate.now())
        );

        when(fileParseService.parseCsvRecordToEntity(multipartFile)).thenReturn(mixedRecords);

        reportService.createReport(multipartFile);

        verify(notificationService).sendExpiredServicesNotification(
                argThat(dto -> "C001".equals(dto.customerId()) && dto.numberOfExpiredServices() == 2L));
        verify(notificationService).sendExpiredServicesNotification(
                argThat(dto -> "C002".equals(dto.customerId()) && dto.numberOfExpiredServices() == 3L));
        verify(notificationService, times(2)).sendExpiredServicesNotification(any(ExpiredServicesDTO.class));
    }

    @Test
    void createReport_CustomLimitsFromProperties() throws IOException {
        ReflectionTestUtils.setField(reportService, "expiredServicesLimit", 3);
        ReflectionTestUtils.setField(reportService, "yearsOfSubscription", 2);

        LocalDate threeYearsAgo = LocalDate.now().minusYears(3);

        List<CustomerServiceSubscriptions> records = Arrays.asList(
                createSubscription("C001", "hosting", SubscriptionStatusEnum.EXPIRED, LocalDate.now()),
                createSubscription("C001", "email", SubscriptionStatusEnum.EXPIRED, LocalDate.now()),
                createSubscription("C001", "domain", SubscriptionStatusEnum.EXPIRED, LocalDate.now()),
                createSubscription("C001", "spid", SubscriptionStatusEnum.EXPIRED, LocalDate.now()),
                createSubscription("C002", "hosting", SubscriptionStatusEnum.ACTIVE, threeYearsAgo)
        );

        when(fileParseService.parseCsvRecordToEntity(multipartFile)).thenReturn(records);

        reportService.createReport(multipartFile);

        verify(notificationService).sendExpiredServicesNotification(
                argThat(dto -> "C001".equals(dto.customerId()) && dto.numberOfExpiredServices() == 4L));
        verify(notificationService).sendUpsellingNotification(
                argThat(dto -> "C002".equals(dto.customerId()) && "hosting".equals(dto.upsellingService())));
    }

    @Test
    void createReport_MixedSubscriptionStatuses() throws IOException {
        ReflectionTestUtils.setField(reportService, "expiredServicesLimit", 2);
        ReflectionTestUtils.setField(reportService, "yearsOfSubscription", 3);

        LocalDate fourYearsAgo = LocalDate.now().minusYears(4);
        LocalDate twoYearsAgo = LocalDate.now().minusYears(2);

        List<CustomerServiceSubscriptions> mixedRecords = Arrays.asList(
                // C001: 3 expired services (should trigger expired notification)
                createSubscription("C001", "hosting", SubscriptionStatusEnum.EXPIRED, LocalDate.now()),
                createSubscription("C001", "email", SubscriptionStatusEnum.EXPIRED, LocalDate.now()),
                createSubscription("C001", "domain", SubscriptionStatusEnum.EXPIRED, LocalDate.now()),
                // C002: old active subscription (should trigger upselling)
                createSubscription("C002", "hosting", SubscriptionStatusEnum.ACTIVE, fourYearsAgo),
                // C003: recent active subscription (no notifications)
                createSubscription("C003", "email", SubscriptionStatusEnum.ACTIVE, twoYearsAgo),
                // C004: old expired subscription (no upselling)
                createSubscription("C004", "ssl", SubscriptionStatusEnum.EXPIRED, fourYearsAgo)
        );

        when(fileParseService.parseCsvRecordToEntity(multipartFile)).thenReturn(mixedRecords);
        reportService.createReport(multipartFile);

        verify(notificationService).sendExpiredServicesNotification(
                argThat(dto -> "C001".equals(dto.customerId()) && dto.numberOfExpiredServices() == 3L));

        verify(notificationService).sendUpsellingNotification(
                argThat(dto -> "C002".equals(dto.customerId()) && "hosting".equals(dto.upsellingService())));

        verify(notificationService, times(1)).sendExpiredServicesNotification(any());
        verify(notificationService, times(1)).sendUpsellingNotification(any());
    }

    @Test
    void createReport_BoundaryConditions() throws IOException {
        ReflectionTestUtils.setField(reportService, "expiredServicesLimit", 2);
        ReflectionTestUtils.setField(reportService, "yearsOfSubscription", 3);

        LocalDate exactlyThreeYearsAgo = LocalDate.now().minusYears(3);
        LocalDate justOverThreeYears = LocalDate.now().minusYears(3).minusDays(1);

        List<CustomerServiceSubscriptions> boundaryRecords = Arrays.asList(
                // Exactly at the limit (2 expired services - should not trigger)
                createSubscription("C001", "hosting", SubscriptionStatusEnum.EXPIRED, LocalDate.now()),
                createSubscription("C001", "email", SubscriptionStatusEnum.EXPIRED, LocalDate.now()),
                // Exactly 3 years old (should not trigger upselling)
                createSubscription("C002", "hosting", SubscriptionStatusEnum.ACTIVE, exactlyThreeYearsAgo),
                // Just over 3 years old (should trigger upselling)
                createSubscription("C003", "email", SubscriptionStatusEnum.ACTIVE, justOverThreeYears)
        );

        when(fileParseService.parseCsvRecordToEntity(multipartFile)).thenReturn(boundaryRecords);

        reportService.createReport(multipartFile);

        verify(notificationService, never()).sendExpiredServicesNotification(any());
        verify(notificationService).sendUpsellingNotification(
                argThat(dto -> "C003".equals(dto.customerId()) && "email".equals(dto.upsellingService())));
        verify(notificationService, times(1)).sendUpsellingNotification(any());
    }

    @Test
    void getReportSummary_ServiceExceptions() {
        when(customerServiceSubscriptionService.countServicesByTypeWithStatus(any()))
                .thenThrow(new RuntimeException("Database connection failed"));

        assertThrows(RuntimeException.class, () -> reportService.getReportSummary());
    }
}