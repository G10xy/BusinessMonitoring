package it.bm.service;

import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

import static it.bm.util.Constant.EXTENSION_CSV;
import static it.bm.util.Constant.EXTENSION_TXT;
import static it.bm.util.Constant.HEADER_ACTIVATION_DATE;
import static it.bm.util.Constant.HEADER_AMOUNT;
import static it.bm.util.Constant.HEADER_CUSTOMER_ID;
import static it.bm.util.Constant.HEADER_EXPIRATION_DATE;
import static it.bm.util.Constant.HEADER_SERVICE_TYPE;
import static it.bm.util.Constant.HEADER_STATUS;
import static it.bm.util.Constant.MIME_TYPE_APPLICATION_CSV;
import static it.bm.util.Constant.MIME_TYPE_APPLICATION_VND_MS_EXCEL;
import static it.bm.util.Constant.MIME_TYPE_TEXT_CSV;
import static it.bm.util.Constant.MIME_TYPE_TEXT_PLAIN;

@Service
public class FileValidationService {

    private final Tika tika;

    public FileValidationService() {
        this.tika = new Tika();
    }

    public FileValidationService(Tika tika) {
        this.tika = tika;
    }

    private static final List<String> CSV_MIME_TYPES = Arrays.asList(
            MIME_TYPE_TEXT_CSV,
            MIME_TYPE_TEXT_PLAIN,
            MIME_TYPE_APPLICATION_CSV,
            MIME_TYPE_APPLICATION_VND_MS_EXCEL
    );

    private static final List<String> CSV_EXTENSIONS = Arrays.asList(EXTENSION_CSV, EXTENSION_TXT);

    private static final List<String> EXPECTED_HEADERS = Arrays.asList(HEADER_CUSTOMER_ID, HEADER_SERVICE_TYPE, HEADER_ACTIVATION_DATE, HEADER_EXPIRATION_DATE, HEADER_AMOUNT, HEADER_STATUS);

    public void validateCsvFile(MultipartFile file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File cannot be empty");
        }

        if (file.getSize() == 0) {
            throw new IllegalArgumentException("File size cannot be zero");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("File must have a valid filename");
        }

        if (!hasValidCsvExtension(originalFilename)) {
            throw new IllegalArgumentException("File must have a valid CSV extension (.csv or .txt)");
        }

        try (InputStream inputStream = file.getInputStream()) {
            String detectedMimeType = tika.detect(inputStream, originalFilename);
            if (!isValidCsvMimeType(detectedMimeType)) {
                throw new IllegalArgumentException(String.format("File content type '%s' is not a valid CSV type. Expected: %s", detectedMimeType, CSV_MIME_TYPES));
            }
        }

        try (InputStream headerStream = file.getInputStream()) {
            areExpectedHeadersPresent(headerStream);
        }

    }

    private boolean hasValidCsvExtension(String filename) {
        String lowerCaseFilename = filename.toLowerCase();
        return CSV_EXTENSIONS.stream().anyMatch(lowerCaseFilename::endsWith);
    }

    private boolean isValidCsvMimeType(String mimeType) {
        return CSV_MIME_TYPES.contains(mimeType);
    }

    private void areExpectedHeadersPresent(InputStream inputStream) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("File does not contain any headers");
            }
            String[] headers = headerLine.split(",");
            if (headers.length != EXPECTED_HEADERS.size()) {
                throw new IllegalArgumentException("Invalid number of headers. Expected " + EXPECTED_HEADERS.size() + " but found " + headers.length);
            }
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i].trim().toLowerCase();
                String expected = EXPECTED_HEADERS.get(i);
                if (!header.equals(expected)) {
                    throw new IllegalArgumentException(String.format("Invalid header, expected '%s' but found '%s'", expected, header));
                }
            }
        }
    }
}
