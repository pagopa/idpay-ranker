package it.gov.pagopa.ranker.service.ranker.sessionworker.registry;

import it.gov.pagopa.ranker.service.ranker.sessionworker.NormalSessionWorker;

import java.util.UUID;

public interface SessionWorkerRegistry {
    boolean tryClaimNormal(String sessionId, UUID claimId, NormalSessionWorker normalWorker);
    ManualClaimResult tryClaimManual(String sessionId, UUID claimId);
    boolean release(String sessionId, UUID claimId);
}
