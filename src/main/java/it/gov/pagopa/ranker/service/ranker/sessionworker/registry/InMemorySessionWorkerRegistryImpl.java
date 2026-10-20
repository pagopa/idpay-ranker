package it.gov.pagopa.ranker.service.ranker.sessionworker.registry;

import it.gov.pagopa.ranker.enums.SessionOwnerType;
import it.gov.pagopa.ranker.service.ranker.sessionworker.NormalSessionWorker;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static it.gov.pagopa.ranker.constants.RankerConstants.MDC_TRACE_ID;

@Slf4j
@Service
public class InMemorySessionWorkerRegistryImpl implements SessionWorkerRegistry {

    private static final String LOG_SESSION_ID_MUST_NOT_BE_NULL = "sessionId must not be null";
    private static final String LOG_CLAIM_ID_MUST_NOT_BE_NULL = "claimId must not be null";
    private static final String LOG_RELEASE_AFTER_STATE =
            "[SESSION_WORKER_REGISTRY][CURRENT_STATE] release, after state: {}";
    private static final String LOG_TRY_CLAIM_MANUAL_AFTER_STATE =
            "[SESSION_WORKER_REGISTRY][CURRENT_STATE] tryClaimManual, after state: {}";
    private static final String LOG_TRY_CLAIM_NORMAL_AFTER_STATE =
            "[SESSION_WORKER_REGISTRY][CURRENT_STATE] tryClaimNormal, after state: {}";

    private final ConcurrentHashMap<String, SessionOwner> sessionOwners = new ConcurrentHashMap<>();

    @Override
    public boolean tryClaimNormal(
            String sessionId,
            UUID claimId,
            NormalSessionWorker normalWorker
    ) {
        Objects.requireNonNull(sessionId, LOG_SESSION_ID_MUST_NOT_BE_NULL);
        Objects.requireNonNull(claimId, LOG_CLAIM_ID_MUST_NOT_BE_NULL);
        Objects.requireNonNull(normalWorker, "normalWorker must not be null");

        log.debug("[SESSION_WORKER_REGISTRY][CURRENT_STATE] tryClaimNormal, before state: {}", sessionOwners);

        // Prepare the attempted owner object to put inside the map.
        SessionOwner attemptedOwner = SessionOwner.normal(
                claimId,
                MDC.get(MDC_TRACE_ID),
                normalWorker
        );

        // Tries to put the attempted owner in the map, returning the existing owner.
        SessionOwner existingOwner = sessionOwners.putIfAbsent(
                sessionId,
                attemptedOwner
        );

        // If there's no existing owner then the attempted owner acquires the claim.
        if (existingOwner == null) {
            logClaimAcquired(sessionId, attemptedOwner);
            log.debug(LOG_TRY_CLAIM_NORMAL_AFTER_STATE, sessionOwners);
            return true;
        }

        logClaimRejected(
                sessionId,
                existingOwner,
                attemptedOwner
        );

        log.debug(LOG_TRY_CLAIM_NORMAL_AFTER_STATE, sessionOwners);
        return false;
    }

    @Override
    public ManualClaimResult tryClaimManual(
            String sessionId,
            UUID claimId
    ) {
        Objects.requireNonNull(sessionId, LOG_SESSION_ID_MUST_NOT_BE_NULL);
        Objects.requireNonNull(claimId, LOG_CLAIM_ID_MUST_NOT_BE_NULL);

        log.debug("[SESSION_WORKER_REGISTRY][CURRENT_STATE] tryClaimManual, before state: {}", sessionOwners);

        // Prepare the attempted owner object to put inside the map.
        SessionOwner attemptedOwner = SessionOwner.manual(
                claimId,
                MDC.get(MDC_TRACE_ID)
        );

        // Tries to put the attempted owner in the map, returning the existing owner.
        SessionOwner existingOwner = sessionOwners.putIfAbsent(
                sessionId,
                attemptedOwner
        );

        // If there's no existing owner then the attempted owner acquires the claim.
        if (existingOwner == null) {
            logClaimAcquired(sessionId, attemptedOwner);
            log.debug(LOG_TRY_CLAIM_MANUAL_AFTER_STATE, sessionOwners);
            return ManualClaimResult.claimed();
        }

        logClaimRejected(
                sessionId,
                existingOwner,
                attemptedOwner
        );

        if (existingOwner.ownerType() == SessionOwnerType.NORMAL) {
            log.debug(LOG_TRY_CLAIM_MANUAL_AFTER_STATE, sessionOwners);
            return ManualClaimResult.ownedByNormal(
                    existingOwner.normalWorker()
            );
        }

        log.debug(LOG_TRY_CLAIM_MANUAL_AFTER_STATE, sessionOwners);
        return ManualClaimResult.ownedByManual();
    }

    @Override
    public boolean release(
            String sessionId,
            UUID claimId
    ) {
        Objects.requireNonNull(sessionId, LOG_SESSION_ID_MUST_NOT_BE_NULL);
        Objects.requireNonNull(claimId, LOG_CLAIM_ID_MUST_NOT_BE_NULL);

        log.debug("[SESSION_WORKER_REGISTRY][CURRENT_STATE] release, before state: {}", sessionOwners);

        SessionOwner observedOwner = sessionOwners.get(sessionId);

        if (observedOwner == null) {
            logReleaseRejected(
                    sessionId,
                    claimId,
                    null,
                    "NO_OWNER"
            );
            log.debug(LOG_RELEASE_AFTER_STATE, sessionOwners);
            return false;
        }

        if (!observedOwner.claimId().equals(claimId)) {
            logReleaseRejected(
                    sessionId,
                    claimId,
                    observedOwner,
                    "CLAIM_ID_MISMATCH"
            );
            log.debug(LOG_RELEASE_AFTER_STATE, sessionOwners);
            return false;
        }

        boolean released = sessionOwners.remove(
                sessionId,
                observedOwner
        );

        if (released) {
            logClaimReleased(sessionId, observedOwner);
            log.debug(LOG_RELEASE_AFTER_STATE, sessionOwners);
            return true;
        }

        logReleaseRejected(
                sessionId,
                claimId,
                observedOwner,
                "OWNER_CHANGED_CONCURRENTLY"
        );

        log.debug(LOG_RELEASE_AFTER_STATE, sessionOwners);
        return false;
    }

    private void logClaimAcquired(
            String sessionId,
            SessionOwner owner
    ) {
        log.info(
                "[SESSION_WORKER_REGISTRY][CLAIM_ACQUIRED] "
                        + "sessionId={} "
                        + "ownerType={} claimId={} traceId={}",
                sessionId,
                owner.ownerType(), owner.claimId(), owner.traceId()
        );
    }

    private void logClaimRejected(
            String sessionId,
            SessionOwner existingOwner,
            SessionOwner attemptedOwner
    ) {
        log.info(
                "[SESSION_WORKER_REGISTRY][CLAIM_REJECTED] "
                        + "sessionId={} "
                        + "existingOwnerType={} existingClaimId={} existingTraceId={} "
                        + "attemptedOwnerType={} attemptedClaimId={} attemptedTraceId={}",
                sessionId,
                existingOwner.ownerType(), existingOwner.claimId(), existingOwner.traceId(),
                attemptedOwner.ownerType(), attemptedOwner.claimId(), attemptedOwner.traceId()
        );
    }

    private void logClaimReleased(
            String sessionId,
            SessionOwner releasedOwner
    ) {
        log.info(
                "[SESSION_WORKER_REGISTRY][CLAIM_RELEASED] "
                        + "sessionId={} "
                        + "ownerType={} claimId={} ownerTraceId={}",
                sessionId,
                releasedOwner.ownerType(), releasedOwner.claimId(), releasedOwner.traceId()
        );
    }

    private void logReleaseRejected(
            String sessionId,
            UUID attemptedClaimId,
            SessionOwner observedOwner,
            String reason
    ) {
        log.warn(
                "[SESSION_WORKER_REGISTRY][RELEASE_REJECTED] "
                        + "sessionId={} "
                        + "reason={} "
                        + "attemptedClaimId={} "
                        + "observedOwnerType={} "
                        + "observedClaimId={} "
                        + "observedOwnerTraceId={}",
                sessionId,
                reason,
                attemptedClaimId,
                observedOwner != null ? observedOwner.ownerType() : null,
                observedOwner != null ? observedOwner.claimId() : null,
                observedOwner != null ? observedOwner.traceId() : null
        );
    }
}
