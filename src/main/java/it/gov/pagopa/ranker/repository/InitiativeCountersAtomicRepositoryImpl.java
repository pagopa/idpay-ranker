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

    private static final String FIELD_ID = InitiativeCounters.Fields.id;
    private static final String FIELD_ONBOARDED = InitiativeCounters.Fields.onboarded;
    private static final String FIELD_RESERVED_BUDGET_CENTS = InitiativeCounters.Fields.reservedInitiativeBudgetCents;
    private static final String FIELD_RESIDUAL_BUDGET_CENTS = InitiativeCounters.Fields.residualInitiativeBudgetCents;
    private static final String FIELD_PREALLOCATION_MAP = InitiativeCounters.Fields.preallocationMap;

    private final MongoTemplate mongoTemplate;

    public InitiativeCountersAtomicRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public InitiativeCounters incrementOnboardedAndBudget(String initiativeId, String userId, long reservationCents) {
        Query query = Query.query(Criteria
                .where(FIELD_ID).is(initiativeId)
                .and(FIELD_RESIDUAL_BUDGET_CENTS).gte(reservationCents)
        );

        Update update = new Update()
                .inc(FIELD_ONBOARDED, 1L)
                .inc(FIELD_RESERVED_BUDGET_CENTS, reservationCents)
                .inc(FIELD_RESIDUAL_BUDGET_CENTS, -reservationCents)
                .set(FIELD_PREALLOCATION_MAP + "." + userId,
                        Preallocation.builder()
                                .userId(userId)
                                .status(PreallocationStatus.PREALLOCATED)
                                .createdAt(LocalDateTime.now())
                                .build()
                );

        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                InitiativeCounters.class
        );
    }
}
