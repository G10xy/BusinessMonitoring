package it.aruba.rest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.aruba.model.ReportSummaryResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Tag(name = "Report", description = "Endpoints to upload CSV data and retrieve report summaries")
public interface ReportController {

    @Operation(
            summary = "Upload CSV file",
            description = "Accepts a CSV file, processes it, and creates/updates the report dataset.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "File upload successful",
                            content = @Content(mediaType = MediaType.TEXT_PLAIN_VALUE,
                                    schema = @Schema(example = "File upload successful"))),
                    @ApiResponse(responseCode = "400", description = "Invalid argument within file or format"),
                    @ApiResponse(responseCode = "415", description = "Unsupported media type"),
                    @ApiResponse(responseCode = "500", description = "Server error while processing file")
            }
    )
    ResponseEntity<String> uploadFile(@Parameter(
            description = "CSV file to upload.",
            required = true,
            content = @Content(
                    mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
                    schema = @Schema(type = "string", format = "binary"),
                    encoding = @Encoding(name = "file", contentType = "text/csv")
            )
    ) @RequestParam("file") MultipartFile file) throws IOException;

    @Operation(
            summary = "Get summary report",
            description = "Returns the computed summary of the uploaded dataset.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Summary returned",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ReportSummaryResponse.class))),
                    @ApiResponse(responseCode = "404", description = "No data available"),
                    @ApiResponse(responseCode = "500", description = "Server error")
            }
    )
    ResponseEntity<ReportSummaryResponse> getSummaryReport();
}
