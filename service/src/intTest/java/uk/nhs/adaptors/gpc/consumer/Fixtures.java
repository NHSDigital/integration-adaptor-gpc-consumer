package uk.nhs.adaptors.gpc.consumer;

import java.util.Arrays;

import lombok.Getter;

public class Fixtures {

    public static String[] orgCodes() {
        return Arrays.stream(Organization.values())
            .map(Organization::getOdsCode).toArray(String[]::new);
    }

    @Getter
    public enum Organization {
        MOCK_ORG("GP0001"), GPC_DEMONSTRATOR_ORG("B82617");

        private final String odsCode;

        Organization(String odsCode) {
            this.odsCode = odsCode;
        }
    }

    @Getter
    public enum Patient {
        HAS_DOCUMENTS("9690937286", "2"), NO_DOCUMENTS("9690937294", "1");

        private final String nhsNumber;
        private final String accessDocumentLogicalId;

        Patient(String nhsNumber, String accessDocumentLogicalId) {
            this.nhsNumber = nhsNumber;
            this.accessDocumentLogicalId = accessDocumentLogicalId;
        }
    }

    @Getter
    public enum Binary {
        MSWORD("07a6483f-732b-461e-86b6-edb665c45510", Patient.HAS_DOCUMENTS);

        private final String id;
        private final Patient forPatient;

        Binary(String id, Patient forPatient) {
            this.id = id;
            this.forPatient = forPatient;
        }
    }
}
