package it.gov.pagopa.ranker.service.ranker.sessionworker.registry;

import it.gov.pagopa.ranker.enums.SessionOwnerType;
import it.gov.pagopa.ranker.service.ranker.sessionworker.NormalSessionWorker;

import java.util.UUID;

record SessionOwner(
        UUID claimId,
        String traceId,
        SessionOwnerType ownerType,
        NormalSessionWorker normalWorker
) {

    static SessionOwner normal(
            UUID claimId,
            String traceId,
            NormalSessionWorker normalWorker
    ) {
        return new SessionOwner(
                claimId,
                traceId,
                SessionOwnerType.NORMAL,
                normalWorker
        );
    }

    static SessionOwner manual(
            UUID claimId,
            String traceId
    ) {
        return new SessionOwner(
                claimId,
                traceId,
                SessionOwnerType.MANUAL,
                null
        );
    }
}