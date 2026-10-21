package it.gov.pagopa.ranker.service.ranker.sessionworker.registry;

import it.gov.pagopa.ranker.enums.SessionOwnerType;
import it.gov.pagopa.ranker.service.ranker.sessionworker.NormalSessionWorker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static it.gov.pagopa.utils.CommonUtils.getSpanContextTraceId;
import static it.gov.pagopa.utils.CommonUtils.sanitizeString;

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

        logDebugSanitizedMapState("[SESSION_WORKER_REGISTRY][CURRENT_STATE] tryClaimNormal, before state: {}");

        // Prepare the attempted owner object to put inside the map.
        SessionOwner attemptedOwner = SessionOwner.normal(
                claimId,
                getSpanContextTraceId(),
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
            logDebugSanitizedMapState(LOG_TRY_CLAIM_NORMAL_AFTER_STATE);
            return true;
        }

        logClaimRejected(
                sessionId,
                existingOwner,
                attemptedOwner
        );

        logDebugSanitizedMapState(LOG_TRY_CLAIM_NORMAL_AFTER_STATE);
        return false;
    }

    @Override
    public ManualClaimResult tryClaimManual(
            String sessionId,
            UUID claimId
    ) {
        Objects.requireNonNull(sessionId, LOG_SESSION_ID_MUST_NOT_BE_NULL);
        Objects.requireNonNull(claimId, LOG_CLAIM_ID_MUST_NOT_BE_NULL);

        logDebugSanitizedMapState("[SESSION_WORKER_REGISTRY][CURRENT_STATE] tryClaimManual, before state: {}");

        // Prepare the attempted owner object to put inside the map.
        SessionOwner attemptedOwner = SessionOwner.manual(
                claimId,
                getSpanContextTraceId()
        );

        // Tries to put the attempted owner in the map, returning the existing owner.
        SessionOwner existingOwner = sessionOwners.putIfAbsent(
                sessionId,
                attemptedOwner
        );

        // If there's no existing owner then the attempted owner acquires the claim.
        if (existingOwner == null) {
            logClaimAcquired(sessionId, attemptedOwner);
            logDebugSanitizedMapState(LOG_TRY_CLAIM_MANUAL_AFTER_STATE);
            return ManualClaimResult.claimed();
        }

        logClaimRejected(
                sessionId,
                existingOwner,
                attemptedOwner
        );

        if (existingOwner.ownerType() == SessionOwnerType.NORMAL) {
            logDebugSanitizedMapState(LOG_TRY_CLAIM_MANUAL_AFTER_STATE);
            return ManualClaimResult.ownedByNormal(
                    existingOwner.normalWorker()
            );
        }

        logDebugSanitizedMapState(LOG_TRY_CLAIM_MANUAL_AFTER_STATE);
        return ManualClaimResult.ownedByManual();
    }

    @Override
    public boolean release(
            String sessionId,
            UUID claimId
    ) {
        Objects.requireNonNull(sessionId, LOG_SESSION_ID_MUST_NOT_BE_NULL);
        Objects.requireNonNull(claimId, LOG_CLAIM_ID_MUST_NOT_BE_NULL);

        logDebugSanitizedMapState("[SESSION_WORKER_REGISTRY][CURRENT_STATE] release, before state: {}");

        SessionOwner observedOwner = sessionOwners.get(sessionId);

        if (observedOwner == null) {
            logReleaseRejected(
                    sessionId,
                    claimId,
                    null,
                    "NO_OWNER"
            );
            logDebugSanitizedMapState(LOG_RELEASE_AFTER_STATE);
            return false;
        }

        if (!observedOwner.claimId().equals(claimId)) {
            logReleaseRejected(
                    sessionId,
                    claimId,
                    observedOwner,
                    "CLAIM_ID_MISMATCH"
            );
            logDebugSanitizedMapState(LOG_RELEASE_AFTER_STATE);
            return false;
        }

        boolean released = sessionOwners.remove(
                sessionId,
                observedOwner
        );

        if (released) {
            logClaimReleased(sessionId, observedOwner);
            logDebugSanitizedMapState(LOG_RELEASE_AFTER_STATE);
            return true;
        }

        logReleaseRejected(
                sessionId,
                claimId,
                observedOwner,
                "OWNER_CHANGED_CONCURRENTLY"
        );

        logDebugSanitizedMapState(LOG_RELEASE_AFTER_STATE);
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
                sanitizeString(sessionId),
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
                sanitizeString(sessionId),
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
                sanitizeString(sessionId),
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
                sanitizeString(sessionId),
                reason,
                attemptedClaimId,
                observedOwner != null ? observedOwner.ownerType() : null,
                observedOwner != null ? observedOwner.claimId() : null,
                observedOwner != null ? observedOwner.traceId() : null
        );
    }

    private void logDebugSanitizedMapState(String message) {
        if (log.isDebugEnabled()) {
            StringBuilder sanitizedMapLogBuilder = new StringBuilder();
            sanitizedMapLogBuilder.append("[");
            for (Map.Entry<String, SessionOwner> entry : sessionOwners.entrySet()) {
                sanitizedMapLogBuilder.append("{ sessionId=")
                        .append(sanitizeString(entry.getKey()))
                        .append(" : owner=")
                        .append(entry.getValue().toString());
            }
            sanitizedMapLogBuilder.append("]");
            log.debug(message, sanitizedMapLogBuilder);
        }
    }
}
