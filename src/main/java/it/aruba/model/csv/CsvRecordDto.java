package it.aruba.model.csv;

import lombok.Getter;
import org.apache.commons.csv.CSVRecord;

import static it.aruba.util.Constant.HEADER_ACTIVATION_DATE;
import static it.aruba.util.Constant.HEADER_AMOUNT;
import static it.aruba.util.Constant.HEADER_CUSTOMER_ID;
import static it.aruba.util.Constant.HEADER_EXPIRATION_DATE;
import static it.aruba.util.Constant.HEADER_SERVICE_TYPE;
import static it.aruba.util.Constant.HEADER_STATUS;

@Getter
public class CsvRecordDto {

    private final String customerId;
    private final String serviceType;
    private final String activationDate;
    private final String expirationDate;
    private final String amount;
    private final String status;
    private final long recordNumber;

    public CsvRecordDto(CSVRecord csvRecord) {
        this.customerId = csvRecord.get(HEADER_CUSTOMER_ID);
        this.serviceType = csvRecord.get(HEADER_SERVICE_TYPE);
        this.activationDate = csvRecord.get(HEADER_ACTIVATION_DATE);
        this.expirationDate = csvRecord.get(HEADER_EXPIRATION_DATE);
        this.amount = csvRecord.get(HEADER_AMOUNT);
        this.status = csvRecord.get(HEADER_STATUS);
        this.recordNumber = csvRecord.getRecordNumber();
    }
}
