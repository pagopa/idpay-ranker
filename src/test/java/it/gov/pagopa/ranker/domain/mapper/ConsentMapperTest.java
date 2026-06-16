package it.gov.pagopa.ranker.domain.mapper;

import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.domain.model.Onboarding;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;

class ConsentMapperTest {

    private final ConsentMapper consentMapper = new ConsentMapper();
    @Test
    void testMap_success() {
        Instant now = Instant.now();

        Onboarding onboarding = new Onboarding("init123", "user123");
        onboarding.setStatus("STATUS_OK");
        onboarding.setPdndAccept(true);
        onboarding.setCriteriaConsensusTimestamp(now);
        onboarding.setTc(true);
        onboarding.setTcAcceptTimestamp(now.minus (1, ChronoUnit.HOURS));
        onboarding.setUserMail("email@test.com");
        onboarding.setChannel("APP");
        onboarding.setName("Mario");
        onboarding.setSurname("Rossi");

        OnboardingDTO result = consentMapper.map(onboarding);

        assertNotNull(result);
        assertEquals("user123", result.getUserId());
        assertEquals("init123", result.getInitiativeId());
        assertEquals("STATUS_OK", result.getStatus());
        assertTrue(result.getPdndAccept());
        assertEquals(now, result.getCriteriaConsensusTimestamp());
        assertTrue(result.getTc());
        assertEquals(now.minus(1, ChronoUnit.HOURS), result.getTcAcceptTimestamp());
        assertEquals("email@test.com", result.getUserMail());
        assertEquals("APP", result.getChannel());
        assertEquals("Mario", result.getName());
        assertEquals("Rossi", result.getSurname());
    }

    @Test
    void testMap_withNullValues() {
        Onboarding onboarding = new Onboarding(null, null);

        OnboardingDTO result = consentMapper.map(onboarding);

        assertNotNull(result);
        assertNull(result.getUserId());
        assertNull(result.getInitiativeId());
        assertNull(result.getStatus());
        assertNull(result.getPdndAccept());
        assertNull(result.getCriteriaConsensusTimestamp());
        assertNull(result.getTc());
        assertNull(result.getTcAcceptTimestamp());
        assertNull(result.getUserMail());
        assertNull(result.getChannel());
        assertNull(result.getName());
        assertNull(result.getSurname());
    }
}
