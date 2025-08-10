package it.aruba.service;

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

@Service
public class FileValidationService {

    private static final List<String> CSV_MIME_TYPES = Arrays.asList(
            "text/csv",
            "text/plain",
            "application/csv",
            "application/vnd.ms-excel"
    );

    private static final List<String> CSV_EXTENSIONS = Arrays.asList(".csv", ".txt");

    private static final List<String> EXPECTED_HEADERS = Arrays.asList(
            "customer_id", "service_type", "activation_date", "expiration_date", "amount", "status"
    );

    public void validateCsvFile(MultipartFile file) throws IOException {
        Tika tika = new Tika();

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
