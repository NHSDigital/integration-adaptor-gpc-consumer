package uk.nhs.adaptors.gpccmocks.sds;

import static org.springframework.http.HttpStatus.ACCEPTED;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import uk.nhs.adaptors.gpccmocks.TemplateUtils;

@RestController
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class SdsController {
    @GetMapping(value = "/spine-directory/Endpoint"
//        consumes = "application/json+fhir"
    )
    @ResponseStatus(value = ACCEPTED)
    public ResponseEntity<String> postMockMhs(
            @RequestParam String organization,
            @RequestParam String identifier,
            @RequestHeader(name="X-Correlation-Id") String correlationId,
            @RequestHeader(name="apikey") String apikey,
            @RequestHeader(name="Host") String host,
            @RequestHeader(name="X-Forwarded-Host", required=false) String xForwardedHost) {

        // Use X-Forwarded-Host if provided to support LB / Proxy
        host = StringUtils.hasText(xForwardedHost) ? xForwardedHost : host;

        if(!isUuid(correlationId)) {
            return badRequest("X-Correlation-Id header must be a UUID");
        }

        var odsCode = organization.split("\\|")[1];
        var interaction = identifier.split("\\|")[1];

        SdsModel.SdsModelBuilder sdsModelBuilder = SdsModel.builder()
            .fhirBase("STU3/1/gpconnect/structured/fhir")
            .baseUrl("http://" + host)
            .interactionId(interaction)
            .odsCode(odsCode);

        switch (interaction) {
            case "urn:nhs:names:services:gpconnect:fhir:operation:gpc.getstructuredrecord-1":
                sdsModelBuilder.fhirBase("STU3/1/gpconnect/structured/fhir");
                break;
            case "urn:nhs:names:services:gpconnect:documents:fhir:rest:search:patient-1":
            case "urn:nhs:names:services:gpconnect:documents:fhir:rest:read:binary-1":
            case "urn:nhs:names:services:gpconnect:documents:fhir:rest:search:documentreference-1":
                sdsModelBuilder.fhirBase("STU3/1/gpconnect/documents/fhir");
                break;
            default:
                return badRequest("Unsupported interaction id " + interaction);
        }

        var body = TemplateUtils.fillTemplate("sds/endpoint", sdsModelBuilder.build());
        return new ResponseEntity<>(body, getResponseHeaders(), HttpStatus.OK);
    }

    ResponseEntity<String> badRequest(String message) {
        return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
    }

    private boolean isUuid(@NonNull String str) {
        try {
            UUID.fromString(str);
        } catch (IllegalArgumentException e) {
            return false;
        }
        return true;
    }

    private HttpHeaders getResponseHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_TYPE, "application/json+fhir");
        return headers;
    }
}
