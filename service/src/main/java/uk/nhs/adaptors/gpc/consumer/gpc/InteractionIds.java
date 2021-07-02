package uk.nhs.adaptors.gpc.consumer.gpc;

import lombok.experimental.UtilityClass;

@UtilityClass
public class InteractionIds {
    private static final String INTERACTION_ID_PREFIX = "urn:nhs:names:services:gpconnect:";
    public static final String STRUCTURED_ID = INTERACTION_ID_PREFIX + "fhir:operation:gpc.getstructuredrecord-1";
    public static final String MIGRATE_STRUCTURED_ID = "urn:nhs:names:services:gpconnect:fhir:operation:gpc.migratestructuredrecord-1";
    public static final String PATIENT_SEARCH_ID = INTERACTION_ID_PREFIX + "documents:fhir:rest:search:patient-1";
    public static final String DOCUMENT_SEARCH_ID = INTERACTION_ID_PREFIX + "documents:fhir:rest:search:documentreference-1";
    public static final String DOCUMENT_READ_ID = INTERACTION_ID_PREFIX + "documents:fhir:rest:read:binary-1";
    public static final String DOCUMENT_MIGRATE_ID = INTERACTION_ID_PREFIX + "documents:fhir:rest:migrate:binary-1";
}
