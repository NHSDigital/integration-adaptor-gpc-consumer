package uk.nhs.adaptors.gpccmocks.gpc;

import static org.springframework.http.HttpStatus.ACCEPTED;

import static uk.nhs.adaptors.gpccmocks.common.ControllerHelpers.getResponseHeaders;
import static uk.nhs.adaptors.gpccmocks.common.ControllerHelpers.isUuid;
import static uk.nhs.adaptors.gpccmocks.common.OperationOutcomes.badRequest;
import static uk.nhs.adaptors.gpccmocks.common.OperationOutcomes.patientNotFound;
import static uk.nhs.adaptors.gpccmocks.common.OperationOutcomes.referenceNotFound;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.jayway.jsonpath.JsonPath;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gpccmocks.common.TemplateUtils;

@RestController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class GpcController {
    @PostMapping(value = "/{odsCode}/STU3/1/gpconnect/structured/fhir/Patient/$gpc.getstructuredrecord")
    @ResponseStatus(value = ACCEPTED)
    public ResponseEntity<String> accessStructuredRecord(
        @PathVariable String odsCode,
        @RequestHeader(name = "Ssp-TraceID") String sspTraceId,
        @RequestHeader(name = "Ssp-From") String sspFrom,
        @RequestHeader(name = "Ssp-To") String sspTo,
        @RequestHeader(name = "Ssp-InteractionID") String sspInteractionId,
        @RequestHeader(name = HttpHeaders.AUTHORIZATION) String authorization,
        @RequestHeader(name = HttpHeaders.HOST) String host,
        @RequestHeader(name = "X-Forwarded-Host", required = false) String xForwardedHost,
        @RequestBody String requestBody) {

        // Use X-Forwarded-Host if provided to support LB / Proxy
        host = StringUtils.hasText(xForwardedHost) ? xForwardedHost : host;

        if (!isUuid(sspTraceId)) {
            return badRequest("Ssp-TraceID header must be a UUID");
        }

        var result = (List<?>) JsonPath.read(requestBody, "$.parameter[:1].valueIdentifier.value");
        if (result.isEmpty()) {
            return badRequest("Request body does not include patient identifier");
        }
        var nhsNumber = (String) result.get(0);

        GpcModel gpcModel = GpcModel.builder()
            .baseUrl("http://" + host)
            .odsCode(odsCode)
            .nhsNumber(nhsNumber)
            .build();

        var body = TemplateUtils.fillTemplate("gpc/accessRecordStructured", gpcModel);
        return new ResponseEntity<>(body, getResponseHeaders(), HttpStatus.OK);
    }

    @GetMapping(value = "/{odsCode}/STU3/1/gpconnect/documents/fhir/Patient/{patientId}/DocumentReference")
    @ResponseStatus(value = ACCEPTED)
    public ResponseEntity<String> findPatientsDocuments(
        @PathVariable String odsCode,
        @PathVariable String patientId,
        @RequestHeader(name = "Ssp-TraceID") String sspTraceId,
        @RequestHeader(name = "Ssp-From") String sspFrom,
        @RequestHeader(name = "Ssp-To") String sspTo,
        @RequestHeader(name = "Ssp-InteractionID") String sspInteractionId,
        @RequestHeader(name = HttpHeaders.AUTHORIZATION) String authorization,
        @RequestHeader(name = HttpHeaders.HOST) String host,
        @RequestHeader(name = "X-Forwarded-Host", required = false) String xForwardedHost) {

        // Use X-Forwarded-Host if provided to support LB / Proxy
        host = StringUtils.hasText(xForwardedHost) ? xForwardedHost : host;

        if (!isUuid(sspTraceId)) {
            return badRequest("Ssp-TraceID header must be a UUID");
        }

        GpcModel.GpcModelBuilder gpcModelBuilder = GpcModel.builder()
            .baseUrl("http://" + host)
            .odsCode(odsCode)
            .patientId(patientId);

        switch (patientId) {
            case "1":
                gpcModelBuilder.hasDocuments(false)
                    .nhsNumber("9690937294");
                break;
            case "2":
                gpcModelBuilder.hasDocuments(true)
                    .nhsNumber("9690937286");
                break;
            default:
                return patientNotFound("Patient/" + patientId + " not found");
        }

        var body = TemplateUtils.fillTemplate("gpc/searchForAPatientsDocuments", gpcModelBuilder.build());
        return new ResponseEntity<>(body, getResponseHeaders(), HttpStatus.OK);
    }

    @GetMapping(value = "/{odsCode}/STU3/1/gpconnect/documents/fhir/Patient")
    @ResponseStatus(value = ACCEPTED)
    public ResponseEntity<String> findAPatient(
        @PathVariable String odsCode,
        @RequestParam String identifier,
        @RequestHeader(name = "Ssp-TraceID") String sspTraceId,
        @RequestHeader(name = "Ssp-From") String sspFrom,
        @RequestHeader(name = "Ssp-To") String sspTo,
        @RequestHeader(name = "Ssp-InteractionID") String sspInteractionId,
        @RequestHeader(name = HttpHeaders.AUTHORIZATION) String authorization,
        @RequestHeader(name = HttpHeaders.HOST) String host,
        @RequestHeader(name = "X-Forwarded-Host", required = false) String xForwardedHost) {

        // Use X-Forwarded-Host if provided to support LB / Proxy
        host = StringUtils.hasText(xForwardedHost) ? xForwardedHost : host;

        if (!isUuid(sspTraceId)) {
            return badRequest("Ssp-TraceID header must be a UUID");
        }

        var nhsNumber = identifier.split("\\|")[1];

        GpcModel.GpcModelBuilder gpcModelBuilder = GpcModel.builder()
            .baseUrl("http://" + host)
            .odsCode(odsCode)
            .nhsNumber(nhsNumber);

        switch (nhsNumber) {
            case "9690937294":
                gpcModelBuilder.patientId("1");
                break;
            case "9690937286":
                gpcModelBuilder.patientId("2");
                break;
            default:
                gpcModelBuilder.patientId(null);
        }

        var body = TemplateUtils.fillTemplate("gpc/findAPatient", gpcModelBuilder.build());
        return new ResponseEntity<>(body, getResponseHeaders(), HttpStatus.OK);
    }

    @GetMapping(value = "/{odsCode}/STU3/1/gpconnect/documents/fhir/Binary/{documentId}")
    @ResponseStatus(value = ACCEPTED)
    public ResponseEntity<String> retrieveADocument(
        @PathVariable String odsCode,
        @PathVariable String documentId,
        @RequestHeader(name = "Ssp-TraceID") String sspTraceId,
        @RequestHeader(name = "Ssp-From") String sspFrom,
        @RequestHeader(name = "Ssp-To") String sspTo,
        @RequestHeader(name = "Ssp-InteractionID") String sspInteractionId,
        @RequestHeader(name = HttpHeaders.AUTHORIZATION) String authorization) {

        if (!isUuid(sspTraceId)) {
            return badRequest("Ssp-TraceID header must be a UUID");
        }

        if (!"07a6483f-732b-461e-86b6-edb665c45510".equals(documentId)) {
            return referenceNotFound("Binary/" + documentId + " not found");
        }

        var gpcModel = GpcModel.builder().build();

        var body = TemplateUtils.fillTemplate("gpc/retrieveADocument", gpcModel);
        return new ResponseEntity<>(body, getResponseHeaders(), HttpStatus.OK);
    }
}
