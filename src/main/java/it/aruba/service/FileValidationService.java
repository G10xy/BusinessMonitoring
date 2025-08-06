package it.aruba.service;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
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

        String detectedMimeType;
        try (InputStream inputStream = file.getInputStream()) {
            detectedMimeType = tika.detect(inputStream, originalFilename);
        }

        if (!isValidCsvMimeType(detectedMimeType)) {
            throw new IllegalArgumentException(String.format("File content type '%s' is not a valid CSV type. Expected: %s", detectedMimeType, CSV_MIME_TYPES));
        }

    }

    private boolean hasValidCsvExtension(String filename) {
        String lowerCaseFilename = filename.toLowerCase();
        return CSV_EXTENSIONS.stream().anyMatch(lowerCaseFilename::endsWith);
    }

    private boolean isValidCsvMimeType(String mimeType) {
        return CSV_MIME_TYPES.contains(mimeType);
    }
}
