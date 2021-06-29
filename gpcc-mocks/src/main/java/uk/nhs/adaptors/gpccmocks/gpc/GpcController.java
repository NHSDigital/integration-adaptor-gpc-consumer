package uk.nhs.adaptors.gpccmocks.gpc;

import static org.springframework.http.HttpStatus.ACCEPTED;

import static uk.nhs.adaptors.gpccmocks.common.ControllerHelpers.getHostAndPortFromRequest;
import static uk.nhs.adaptors.gpccmocks.common.ControllerHelpers.getResponseHeaders;
import static uk.nhs.adaptors.gpccmocks.common.ControllerHelpers.isUuid;
import static uk.nhs.adaptors.gpccmocks.common.OperationOutcomes.badRequest;
import static uk.nhs.adaptors.gpccmocks.common.OperationOutcomes.invalidNhsNumber;
import static uk.nhs.adaptors.gpccmocks.common.OperationOutcomes.patientNotFound;
import static uk.nhs.adaptors.gpccmocks.common.OperationOutcomes.referenceNotFound;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
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
@RequestMapping("/{odsCode}/STU3/1/gpconnect")
public class GpcController {
    @PostMapping(value = "/structured/fhir/Patient/$gpc.getstructuredrecord")
    @ResponseStatus(value = ACCEPTED)
    public ResponseEntity<String> accessStructuredRecord(
        HttpServletRequest request,
        @PathVariable String odsCode,
        @RequestHeader(name = "Ssp-TraceID") String sspTraceId,
        @RequestHeader(name = "Ssp-From") String sspFrom,
        @RequestHeader(name = "Ssp-To") String sspTo,
        @RequestHeader(name = "Ssp-InteractionID") String sspInteractionId,
        @RequestHeader(name = HttpHeaders.AUTHORIZATION) String authorization,
        @RequestBody String requestBody) {

        var host = getHostAndPortFromRequest(request);

        log.debug("Request for 'Access Structured Record'. " +
                "odsCode={} Ssp-TraceID={} Ssp-From={} Ssp-To={} Ssp-InteractionID={} Host/X-Forwarded-Host: {}",
            odsCode, sspTraceId, sspFrom, sspTo, sspInteractionId, host);

        log.debug("Request body for 'Access Structured Record'\n{}", requestBody);

        if (!isUuid(sspTraceId)) {
            return badRequest("Ssp-TraceID header must be a UUID");
        }

        var result = (List<?>) JsonPath.read(requestBody, "$.parameter[:1].valueIdentifier.value");
        if (result.isEmpty()) {
            return badRequest("Request body does not include patient identifier");
        }
        var nhsNumber = (String) result.get(0);

        GpcModel.GpcModelBuilder gpcModelBuilder = GpcModel.builder()
            .baseUrl("http://" + host)
            .odsCode(odsCode);

        switch (nhsNumber) {
            case "9690937294":
            case "9690937286":
                gpcModelBuilder.nhsNumber(nhsNumber);
                break;
            default:
                return patientNotFound("Patient " + nhsNumber + " not found");
        }

        var body = TemplateUtils.fillTemplate("gpc/accessRecordStructured", gpcModelBuilder.build());
        return new ResponseEntity<>(body, getResponseHeaders(), HttpStatus.OK);
    }

    @GetMapping(value = "/documents/fhir/Patient/{patientId}/DocumentReference")
    @ResponseStatus(value = ACCEPTED)
    public ResponseEntity<String> findPatientsDocuments(
        HttpServletRequest request,
        @PathVariable String odsCode,
        @PathVariable String patientId,
        @RequestHeader(name = "Ssp-TraceID") String sspTraceId,
        @RequestHeader(name = "Ssp-From") String sspFrom,
        @RequestHeader(name = "Ssp-To") String sspTo,
        @RequestHeader(name = "Ssp-InteractionID") String sspInteractionId,
        @RequestHeader(name = HttpHeaders.AUTHORIZATION) String authorization) {

        var host = getHostAndPortFromRequest(request);

        log.debug("Request for 'Find a patient's documents'. " +
                "odsCode={} Ssp-TraceID={} Ssp-From={} Ssp-To={} Ssp-InteractionID={} Host/X-Forwarded-Host: {}",
            odsCode, sspTraceId, sspFrom, sspTo, sspInteractionId, host);

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

    @GetMapping(value = "/documents/fhir/Patient")
    @ResponseStatus(value = ACCEPTED)
    public ResponseEntity<String> findAPatient(
        HttpServletRequest request,
        @PathVariable String odsCode,
        @RequestParam String identifier,
        @RequestHeader(name = "Ssp-TraceID") String sspTraceId,
        @RequestHeader(name = "Ssp-From") String sspFrom,
        @RequestHeader(name = "Ssp-To") String sspTo,
        @RequestHeader(name = "Ssp-InteractionID") String sspInteractionId,
        @RequestHeader(name = HttpHeaders.AUTHORIZATION) String authorization) {

        var host = getHostAndPortFromRequest(request);

        log.debug("Request for 'Find a patient'. " +
                "odsCode={} Ssp-TraceID={} Ssp-From={} Ssp-To={} Ssp-InteractionID={} Host/X-Forwarded-Host: {}",
            odsCode, sspTraceId, sspFrom, sspTo, sspInteractionId, host);

        if (!isUuid(sspTraceId)) {
            return badRequest("Ssp-TraceID header must be a UUID");
        }

        var nhsNumber = identifier.split("\\|")[1];
        if (!nhsNumber.matches("[0-9]+")) {
            return invalidNhsNumber("Invalid NHS number");
        }

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

    @GetMapping(value = "/documents/fhir/Binary/{documentId}")
    @ResponseStatus(value = ACCEPTED)
    public ResponseEntity<String> retrieveADocument(
        @PathVariable String odsCode,
        @PathVariable String documentId,
        @RequestHeader(name = "Ssp-TraceID") String sspTraceId,
        @RequestHeader(name = "Ssp-From") String sspFrom,
        @RequestHeader(name = "Ssp-To") String sspTo,
        @RequestHeader(name = "Ssp-InteractionID") String sspInteractionId,
        @RequestHeader(name = HttpHeaders.AUTHORIZATION) String authorization) {

        log.debug("Request for 'Retrieve a document'. " +
                "odsCode={} Ssp-TraceID={} Ssp-From={} Ssp-To={} Ssp-InteractionID={}",
            odsCode, sspTraceId, sspFrom, sspTo, sspInteractionId);

        if (!isUuid(sspTraceId)) {
            return badRequest("Ssp-TraceID header must be a UUID");
        }

        if (!"07a6483f-732b-461e-86b6-edb665c45510".equals(documentId)) {
            return referenceNotFound("Binary/" + documentId + " not found");
        }

        var gpcModel = GpcModel.builder()
            .documentId(documentId)
            .build();

        var body = TemplateUtils.fillTemplate("gpc/retrieveADocument", gpcModel);
        return new ResponseEntity<>(body, getResponseHeaders(), HttpStatus.OK);
    }
}
