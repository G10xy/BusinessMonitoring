package it.bm.service;

import it.bm.model.csv.CsvRecordDto;
import it.bm.model.entity.CustomerServiceSubscriptions;
import it.bm.model.entity.SubscriptionStatus;
import it.bm.model.enums.SubscriptionStatusEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.bm.util.Constant.HEADER_CUSTOMER_ID;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileParseService {

    private final SubscriptionStatusService subscriptionStatusService;

    public List<CustomerServiceSubscriptions> parseCsvRecordToEntity(MultipartFile file) {
        List<CustomerServiceSubscriptions> validRecords = new ArrayList<>();

        try (BufferedReader fileReader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
             CSVParser csvParser = CSVParser.parse(fileReader,
                     CSVFormat.DEFAULT.builder()
                             .setHeader()
                             .setSkipHeaderRecord(true)
                             .setIgnoreHeaderCase(true)
                             .setTrim(true)
                             .get()))  {

            for (CSVRecord csvRecord : csvParser) {
                CsvRecordDto dto = new CsvRecordDto(csvRecord);
                Optional<CustomerServiceSubscriptions> optionalCsvData = validateAndMapToEntity(dto);
                optionalCsvData.ifPresent(validRecords::add);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV data: " + e.getMessage());
        }
        return validRecords;
    }

    private Optional<CustomerServiceSubscriptions> validateAndMapToEntity(CsvRecordDto dto) {
        try {
            String customerId = dto.getCustomerId();
            if (customerId == null || customerId.isBlank()) {
                throw new IllegalArgumentException(HEADER_CUSTOMER_ID + " is missing or blank");
            }

            SubscriptionStatusEnum statusEnum = SubscriptionStatusEnum.valueOf(dto.getStatus().toUpperCase());
            SubscriptionStatus status = subscriptionStatusService.findByCodeStatus(statusEnum);
            if (status == null) {
                throw new IllegalArgumentException("Invalid status: " + dto.getStatus());
            }

            LocalDate activationDate = LocalDate.parse(dto.getActivationDate());
            LocalDate expirationDate = LocalDate.parse(dto.getExpirationDate());
            BigDecimal amount = new BigDecimal(dto.getAmount());

            CustomerServiceSubscriptions csvData = new CustomerServiceSubscriptions();
            csvData.setCustomerId(customerId);
            csvData.setServiceType(dto.getServiceType());
            csvData.setActivationDate(activationDate);
            csvData.setExpirationDate(expirationDate);
            csvData.setAmount(amount);
            csvData.setStatus(status);

            return Optional.of(csvData);

        } catch (IllegalArgumentException | DateTimeParseException | NullPointerException e) {
            log.warn("Skipping invalid row number {} because {}", dto.getRecordNumber(), e.getMessage());
            return Optional.empty();
        }
    }
}
