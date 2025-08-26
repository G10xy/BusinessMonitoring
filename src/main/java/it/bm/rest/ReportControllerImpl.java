package it.bm.rest;

import it.bm.model.response.ReportSummaryResponse;
import it.bm.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/report")
@CrossOrigin(origins = "*")
public class ReportControllerImpl implements ReportController {

    private final ReportService reportService;


    @PostMapping(value = "/upload-csv", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('REPORT_USER')")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        reportService.createReport(file);
        return ResponseEntity.status(HttpStatus.OK).body("File upload successful: " + file.getOriginalFilename());
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('REPORT_USER')")
    public ResponseEntity<ReportSummaryResponse> getSummaryReport() {
        return ResponseEntity.ok(reportService.getReportSummary());
    }
}

