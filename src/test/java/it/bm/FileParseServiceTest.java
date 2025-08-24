package it.bm;

import it.bm.model.entity.CustomerServiceSubscriptions;
import it.bm.model.entity.SubscriptionStatus;
import it.bm.model.enums.SubscriptionStatusEnum;
import it.bm.service.FileParseService;
import it.bm.service.SubscriptionStatusService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileParseServiceTest {

    @Mock
    private SubscriptionStatusService subscriptionStatusService;

    private FileParseService fileParseService;

    @BeforeEach
    void setUp() {
        fileParseService = new FileParseService(subscriptionStatusService);
    }

    private MockMultipartFile csvFile(String name, String content) {
        return new MockMultipartFile("file", name, "text/csv", content.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void parsesSingleValidRow() {
        String csv =
                "customer_id,service_type,activation_date,expiration_date,amount,status\n" +
                        "C001,hosting,2024-01-01,2024-12-31,49.99,ACTIVE\n";

        SubscriptionStatus status = new SubscriptionStatus(SubscriptionStatusEnum.ACTIVE);
        when(subscriptionStatusService.findByCodeStatus(SubscriptionStatusEnum.ACTIVE)).thenReturn(status);

        MultipartFile file = csvFile("ok.csv", csv);
        List<CustomerServiceSubscriptions> result = fileParseService.parseCsvRecordToEntity(file);

        assertEquals(1, result.size());
        CustomerServiceSubscriptions row = result.get(0);
        assertEquals("C001", row.getCustomerId());
        assertEquals("hosting", row.getServiceType());
        assertEquals(LocalDate.parse("2024-01-01"), row.getActivationDate());
        assertEquals(LocalDate.parse("2024-12-31"), row.getExpirationDate());
        assertEquals(new BigDecimal("49.99"), row.getAmount());
        assertSame(status.getCode().name(), row.getStatus().getCode().name());
    }

    @Test
    void parsesMultipleRowsSkipsInvalidKeepsValid() {
        String csv ="customer_id,service_type,activation_date,expiration_date,amount,status\n" +
                        // valid
                        "C001,hosting,2024-01-01,2025-12-31,49.99,ACTIVE\n" +
                        // missing customer_id -> skip
                        ",pec,2024-02-01,2025-12-31,10,ACTIVE\n" +
                        // invalid date -> skip
                        "C003,pec,not-a-date,2025-12-31,19.99,ACTIVE\n" +
                        // invalid amount -> skip
                        "C004,mail,2024-03-01,2025-12-31,abc,ACTIVE\n" +
                        // status unknown -> skip
                        "C005,hosting,2024-04-01,2024-12-31,9.99,fail-status\n" +
                        // invalid expiration_date -> skip
                        "C006,mail,2024-01-01,2024-00-31,49.99,ACTIVE\n" +
                        // invalid amount -> skip
                        "C007,mail,2024-01-01,2024-12-31,forty,ACTIVE\n" +
                        // valid
                        "C008,spid,2024-05-01,2025-10-31,29.99,PENDING_RENEWAL\n" +
                        // invalid activation_date -> skip
                        "C009,mail,01-FEB-1999,2024-12-31,10.00,ACTIVE\n";

        SubscriptionStatus active = new SubscriptionStatus(SubscriptionStatusEnum.ACTIVE);
        SubscriptionStatus pending = new SubscriptionStatus(SubscriptionStatusEnum.PENDING_RENEWAL);

        when(subscriptionStatusService.findByCodeStatus(SubscriptionStatusEnum.ACTIVE)).thenReturn(active);
        when(subscriptionStatusService.findByCodeStatus(SubscriptionStatusEnum.PENDING_RENEWAL)).thenReturn(pending);

        MultipartFile file = csvFile("mix.csv", csv);
        List<CustomerServiceSubscriptions> result = fileParseService.parseCsvRecordToEntity(file);

        assertEquals(2, result.size());

        CustomerServiceSubscriptions r1 = result.get(0);
        assertEquals("C001", r1.getCustomerId());
        assertSame(active.getCode().name(), r1.getStatus().getCode().name());

        CustomerServiceSubscriptions r2 = result.get(1);
        assertEquals("C008", r2.getCustomerId());
        assertSame(pending.getCode().name(), r2.getStatus().getCode().name());
    }

    @Test
    void headerCaseInsensitiveAndTrim() {
        String csv ="CUSTOMER_ID , SERVICE_TYPE , ACTIVATION_DATE , EXPIRATION_DATE , AMOUNT , STATUS \n" +
                        "C777 , pec , 2024-01-15 , 2024-12-31 ,  100.00  ,  active \n";

        SubscriptionStatus active = new SubscriptionStatus(SubscriptionStatusEnum.ACTIVE);
        when(subscriptionStatusService.findByCodeStatus(SubscriptionStatusEnum.ACTIVE)).thenReturn(active);

        MultipartFile file = csvFile("headers.csv", csv);
        List<CustomerServiceSubscriptions> result = fileParseService.parseCsvRecordToEntity(file);
        assertEquals(1, result.size());
        CustomerServiceSubscriptions row = result.get(0);
        assertEquals("C777", row.getCustomerId());
        assertEquals("pec", row.getServiceType());
        assertEquals(LocalDate.of(2024, 1, 15), row.getActivationDate());
        assertEquals(LocalDate.of(2024, 12, 31), row.getExpirationDate());
        assertEquals(new BigDecimal("100.00"), row.getAmount());
        assertSame(active.getCode().name(), row.getStatus().getCode().name());
    }

    @Test
    void skipsRowWhenSubscriptionStatusServiceReturnsNull() {
        String csv ="customer_id,service_type,activation_date,expiration_date,amount,status\n" +
                        "C001,Internet,2024-01-01,2024-12-31,49.99,ACTIVE\n";

        when(subscriptionStatusService.findByCodeStatus(SubscriptionStatusEnum.ACTIVE)).thenReturn(null);

        MultipartFile file = csvFile("nullstatus.csv", csv);
        List<CustomerServiceSubscriptions> result = fileParseService.parseCsvRecordToEntity(file);
        assertTrue(result.isEmpty());
    }

    @Test
    void returnsEmptyListWhenOnlyHeaderPresent() {
        String csv = "customer_id,service_type,activation_date,expiration_date,amount,status\n";
        MultipartFile file = csvFile("header-only.csv", csv);

        List<CustomerServiceSubscriptions> result = fileParseService.parseCsvRecordToEntity(file);

        assertNotNull(result);
        assertTrue(result.isEmpty());
        verifyNoInteractions(subscriptionStatusService);
    }

}

