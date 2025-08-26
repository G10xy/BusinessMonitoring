package it.bm;

import it.bm.service.FileValidationService;
import org.apache.tika.Tika;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;



@ExtendWith(MockitoExtension.class)
class FileValidationServiceTest {


    @Mock
    private Tika tikaMock;

    private FileValidationService fileValidationService;

    @BeforeEach
    void setup() {
        fileValidationService = new FileValidationService(tikaMock);
    }

    private static final String VALID_CSV =
            "customer_id,service_type,activation_date,expiration_date,amount,status\n" +
                    "123,Hosting,2023-01-01,2023-12-31,100,ACTIVE";

    @Test
    void testNullFile_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> fileValidationService.validateCsvFile(null));
    }

    @Test
    void testEmptyFile_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.csv", "text/csv", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> fileValidationService.validateCsvFile(file));
    }

    @Test
    void testFileWithoutName_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile("file", "", "text/csv", "data".getBytes());

        assertThrows(IllegalArgumentException.class, () -> fileValidationService.validateCsvFile(file));
    }

    @Test
    void testInvalidExtension_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "data".getBytes());

        assertThrows(IllegalArgumentException.class, () -> fileValidationService.validateCsvFile(file));
    }

    @Test
    void testInvalidMimeType_ThrowsException() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "application/pdf", VALID_CSV.getBytes());

        when(tikaMock.detect(any(InputStream.class), eq("test.csv"))).thenReturn("application/pdf");

        assertThrows(IllegalArgumentException.class, () -> fileValidationService.validateCsvFile(file));
    }

    @Test
    void testValidMimeTypeTextCsv() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv",
                VALID_CSV.getBytes(StandardCharsets.UTF_8));

        when(tikaMock.detect(any(InputStream.class), eq("test.csv"))).thenReturn("text/csv");

        assertDoesNotThrow(() -> fileValidationService.validateCsvFile(file));
        verify(tikaMock).detect(any(InputStream.class), eq("test.csv"));
    }


    @Test
    void testValidMimeTypeTextPlain() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "data.txt", "text/plain",
                VALID_CSV.getBytes(StandardCharsets.UTF_8));

        when(tikaMock.detect(any(InputStream.class), eq("data.txt"))).thenReturn("text/plain");

        assertDoesNotThrow(() -> fileValidationService.validateCsvFile(file));
        verify(tikaMock).detect(any(InputStream.class), eq("data.txt"));
    }

    @Test
    void testTika_ThrowsRuntimeException() throws IOException {
        MockMultipartFile file = new MockMultipartFile("file", "fail.csv", "text/csv",
                VALID_CSV.getBytes(StandardCharsets.UTF_8));

        when(tikaMock.detect(any(InputStream.class), eq("fail.csv")))
                .thenThrow(new RuntimeException("Error"));

        assertThrows(RuntimeException.class,
                () -> fileValidationService.validateCsvFile(file));
    }

    @Test
    void testMissingHeaders_ThrowsException() {
        String invalidContent = "wrong_header,another_one\n123,456";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", invalidContent.getBytes());

        assertThrows(IllegalArgumentException.class, () -> fileValidationService.validateCsvFile(file));
    }

    @Test
    void testIncorrectNumberOfHeaders_ThrowsException() {
        String invalidContent = "customer_id,service_type,activation_date\n123,hosting,2023-01-01";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", invalidContent.getBytes());

        assertThrows(IllegalArgumentException.class, () -> fileValidationService.validateCsvFile(file));
    }

    @Test
    void testHeaderOrderMismatch_ThrowsException() {
        String invalidContent = "service_type,customer_id,activation_date,expiration_date,amount,status\n"
                + "hosting,123,2023-01-01,2023-12-31,50,ACTIVE";
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", invalidContent.getBytes());

        assertThrows(IllegalArgumentException.class, () -> fileValidationService.validateCsvFile(file));
    }
}