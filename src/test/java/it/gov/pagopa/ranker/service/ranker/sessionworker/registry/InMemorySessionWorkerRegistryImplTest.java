package it.gov.pagopa.ranker.service.ranker.sessionworker.registry;

import it.gov.pagopa.ranker.constants.RankerConstants;
import it.gov.pagopa.ranker.enums.ManualClaimOutcome;
import it.gov.pagopa.ranker.service.ranker.sessionworker.NormalSessionWorker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class InMemorySessionWorkerRegistryImplTest {

    private InMemorySessionWorkerRegistryImpl registry;

    @BeforeEach
    void setUp() {
        registry = new InMemorySessionWorkerRegistryImpl();
    }

    @AfterEach
    void tearDown() {
        MDC.clear();
    }

    @Test
    void tryClaimNormal_whenSessionIsNotOwned_shouldClaimSession() {
        String sessionId = "SESSION_ID";
        UUID claimId = UUID.randomUUID();
        NormalSessionWorker normalWorker = mock(NormalSessionWorker.class);

        boolean claimed = registry.tryClaimNormal(
                sessionId,
                claimId,
                normalWorker
        );

        assertTrue(claimed);
    }

    @Test
    void tryClaimNormal_whenSessionIsAlreadyOwnedByNormalWorker_shouldRejectClaim() {
        String sessionId = "SESSION_ID";
        UUID firstClaimId = UUID.randomUUID();
        UUID secondClaimId = UUID.randomUUID();

        NormalSessionWorker firstWorker = mock(NormalSessionWorker.class);
        NormalSessionWorker secondWorker = mock(NormalSessionWorker.class);

        assertTrue(registry.tryClaimNormal(
                sessionId,
                firstClaimId,
                firstWorker
        ));

        boolean claimed = registry.tryClaimNormal(
                sessionId,
                secondClaimId,
                secondWorker
        );

        assertFalse(claimed);
    }

    @Test
    void tryClaimNormal_whenSessionIsAlreadyOwnedByManualWorker_shouldRejectClaim() {
        String sessionId = "SESSION_ID";
        UUID manualClaimId = UUID.randomUUID();
        UUID normalClaimId = UUID.randomUUID();

        assertEquals(
                ManualClaimOutcome.CLAIMED,
                registry.tryClaimManual(sessionId, manualClaimId).outcome()
        );

        boolean claimed = registry.tryClaimNormal(
                sessionId,
                normalClaimId,
                mock(NormalSessionWorker.class)
        );

        assertFalse(claimed);
    }

    @Test
    void tryClaimManual_whenSessionIsNotOwned_shouldReturnClaimed() {
        String sessionId = "SESSION_ID";
        UUID claimId = UUID.randomUUID();

        MDC.put(RankerConstants.MDC_TRACE_ID, "manual-trace-id");

        ManualClaimResult result = registry.tryClaimManual(
                sessionId,
                claimId
        );

        assertEquals(
                ManualClaimOutcome.CLAIMED,
                result.outcome()
        );
    }

    @Test
    void tryClaimManual_whenSessionIsOwnedByNormalWorker_shouldReturnNormalWorker() {
        String sessionId = "SESSION_ID";
        UUID normalClaimId = UUID.randomUUID();
        UUID manualClaimId = UUID.randomUUID();

        NormalSessionWorker normalWorker = mock(NormalSessionWorker.class);

        assertTrue(registry.tryClaimNormal(
                sessionId,
                normalClaimId,
                normalWorker
        ));

        ManualClaimResult result = registry.tryClaimManual(
                sessionId,
                manualClaimId
        );

        assertEquals(
                ManualClaimOutcome.OWNED_BY_NORMAL,
                result.outcome()
        );

        assertSame(
                normalWorker,
                result.normalWorker()
        );
    }

    @Test
    void tryClaimManual_whenSessionIsOwnedByManualWorker_shouldReturnOwnedByManual() {
        String sessionId = "SESSION_ID";
        UUID firstClaimId = UUID.randomUUID();
        UUID secondClaimId = UUID.randomUUID();

        assertEquals(
                ManualClaimOutcome.CLAIMED,
                registry.tryClaimManual(sessionId, firstClaimId).outcome()
        );

        ManualClaimResult result = registry.tryClaimManual(
                sessionId,
                secondClaimId
        );

        assertEquals(
                ManualClaimOutcome.OWNED_BY_MANUAL,
                result.outcome()
        );
    }

    @Test
    void release_whenClaimIdMatchesCurrentOwner_shouldReleaseSession() {
        String sessionId = "SESSION_ID";
        UUID claimId = UUID.randomUUID();

        assertEquals(
                ManualClaimOutcome.CLAIMED,
                registry.tryClaimManual(sessionId, claimId).outcome()
        );

        boolean released = registry.release(
                sessionId,
                claimId
        );

        assertTrue(released);

        ManualClaimResult newClaimResult = registry.tryClaimManual(
                sessionId,
                UUID.randomUUID()
        );

        assertEquals(
                ManualClaimOutcome.CLAIMED,
                newClaimResult.outcome()
        );
    }

    @Test
    void release_whenSessionHasNoOwner_shouldReturnFalse() {
        boolean released = registry.release(
                "SESSION_ID",
                UUID.randomUUID()
        );

        assertFalse(released);
    }

    @Test
    void release_whenClaimIdDoesNotMatchCurrentOwner_shouldNotReleaseSession() {
        String sessionId = "SESSION_ID";
        UUID ownerClaimId = UUID.randomUUID();
        UUID wrongClaimId = UUID.randomUUID();

        NormalSessionWorker normalWorker = mock(NormalSessionWorker.class);

        assertTrue(registry.tryClaimNormal(
                sessionId,
                ownerClaimId,
                normalWorker
        ));

        boolean released = registry.release(
                sessionId,
                wrongClaimId
        );

        assertFalse(released);

        ManualClaimResult manualClaimResult = registry.tryClaimManual(
                sessionId,
                UUID.randomUUID()
        );

        assertEquals(
                ManualClaimOutcome.OWNED_BY_NORMAL,
                manualClaimResult.outcome()
        );

        assertSame(
                normalWorker,
                manualClaimResult.normalWorker()
        );
    }

    @Test
    void release_whenCalledTwiceWithSameClaimId_shouldReturnFalseOnSecondCall() {
        String sessionId = "SESSION_ID";
        UUID claimId = UUID.randomUUID();

        assertEquals(
                ManualClaimOutcome.CLAIMED,
                registry.tryClaimManual(sessionId, claimId).outcome()
        );

        assertTrue(registry.release(sessionId, claimId));

        boolean releasedAgain = registry.release(
                sessionId,
                claimId
        );

        assertFalse(releasedAgain);
    }

    @Test
    void tryClaimNormal_whenSessionIdIsNull_shouldThrowException() {
        UUID claimId = UUID.randomUUID();
        assertThrows(
                NullPointerException.class,
                () -> registry.tryClaimNormal(
                        null,
                        claimId,
                        mock(NormalSessionWorker.class)
                )
        );
    }

    @Test
    void tryClaimManual_whenClaimIdIsNull_shouldThrowException() {
        assertThrows(
                NullPointerException.class,
                () -> registry.tryClaimManual(
                        "SESSION_ID",
                        null
                )
        );
    }

    @Test
    void release_whenSessionIdIsNull_shouldThrowException() {
        UUID claimId = UUID.randomUUID();
        assertThrows(
                NullPointerException.class,
                () -> registry.release(
                        null,
                        claimId
                )
        );
    }

    @Test
    void release_whenClaimIdIsNull_shouldThrowException() {
        assertThrows(
                NullPointerException.class,
                () -> registry.release(
                        "SESSION_ID",
                        null
                )
        );
    }
}
