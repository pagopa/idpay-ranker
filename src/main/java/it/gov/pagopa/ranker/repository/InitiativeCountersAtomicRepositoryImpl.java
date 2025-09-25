package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.domain.model.Preallocation;
import it.gov.pagopa.ranker.enums.PreallocationStatus;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class InitiativeCountersAtomicRepositoryImpl implements InitiativeCountersAtomicRepository {

    private static final String FIELD_ID = "_id";
    private static final String FIELD_ONBOARDED = "onboarded";
    private static final String FIELD_RESERVED_BUDGET_CENTS = "reservedInitiativeBudgetCents";
    private static final String FIELD_RESIDUAL_BUDGET_CENTS = "residualInitiativeBudgetCents";
    private static final String FIELD_PREALLOCATION_LIST = "preallocationList";

    private final MongoTemplate mongoTemplate;

    public InitiativeCountersAtomicRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public InitiativeCounters incrementOnboardedAndBudget(String initiativeId, String userId, long reservationCents) {
        return mongoTemplate.findAndModify(
                Query.query(Criteria
                        .where(FIELD_ID).is(initiativeId)
                        .and(FIELD_RESIDUAL_BUDGET_CENTS).gte(reservationCents)
                ),
                new Update()
                        .inc(FIELD_ONBOARDED, 1L)
                        .inc(FIELD_RESERVED_BUDGET_CENTS, reservationCents)
                        .inc(FIELD_RESIDUAL_BUDGET_CENTS, -reservationCents)
                        .addToSet(FIELD_PREALLOCATION_LIST,
                                Preallocation.builder()
                                        .userId(userId)
                                        .status(PreallocationStatus.PREALLOCATED)
                                        .createdAt(LocalDateTime.now())
                                        .build()
                        ),
                FindAndModifyOptions.options().returnNew(true),
                InitiativeCounters.class
        );
    }
}
