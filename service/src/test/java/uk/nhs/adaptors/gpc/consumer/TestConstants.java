package uk.nhs.adaptors.gpc.consumer;

public class TestConstants {
    public static final String GET_STRUCTURED_INTERACTION =
            "urn:nhs:names:services:gpconnect:fhir:operation:gpc.getstructuredrecord-1";
    public static final String PATIENT_SEARCH_ACCESS_DOCUMENT_INTERACTION =
            "urn:nhs:names:services:gpconnect:documents:fhir:rest:search:patient-1";
    public static final String SEARCH_FOR_DOCUMENT_INTERACTION =
            "urn:nhs:names:services:gpconnect:documents:fhir:rest:search:documentreference-1";
    public static final String RETRIEVE_DOCUMENT_INTERACTION =
            "urn:nhs:names:services:gpconnect:documents:fhir:rest:read:binary-1";
    public static final String MIGRATE_DOCUMENT_INTERACTION =
            "urn:nhs:names:services:gpconnect:documents:fhir:rest:migrate:binary-1";
    public static final String MIGRATE_STRUCTURED_INTERACTION =
            "urn:nhs:names:services:gpconnect:fhir:operation:gpc.migratestructuredrecord-1";
}
