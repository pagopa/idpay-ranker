package it.gov.pagopa.ranker.constants;

import java.time.ZoneId;

public class CommonConstants {
    private CommonConstants() {}

    public static final ZoneId ZONEID = ZoneId.of("Europe/Rome");

    public static final String SEQUENCE_NUMBER_PROPERTY = "sequenceNumber";

    public static final String ADD_SEQUENCE_NUMBER = "ADD_SEQUENCE_NUMBER";

    public static final String DELETE_SEQUENCE_NUMBER = "DELETE_SEQUENCE_NUMBER";

}
