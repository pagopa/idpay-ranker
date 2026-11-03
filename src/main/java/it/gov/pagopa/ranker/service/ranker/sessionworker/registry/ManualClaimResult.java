package it.gov.pagopa.ranker.service.ranker.sessionworker.registry;

import it.gov.pagopa.ranker.enums.ManualClaimOutcome;
import it.gov.pagopa.ranker.service.ranker.sessionworker.NormalSessionWorker;

public record ManualClaimResult(
        ManualClaimOutcome outcome,
        NormalSessionWorker normalWorker
) {

    public static ManualClaimResult claimed() {
        return new ManualClaimResult(
                ManualClaimOutcome.CLAIMED,
                null
        );
    }

    public static ManualClaimResult ownedByNormal(
            NormalSessionWorker normalWorker
    ) {
        return new ManualClaimResult(
                ManualClaimOutcome.OWNED_BY_NORMAL,
                normalWorker
        );
    }

    public static ManualClaimResult ownedByManual() {
        return new ManualClaimResult(
                ManualClaimOutcome.OWNED_BY_MANUAL,
                null
        );
    }
}