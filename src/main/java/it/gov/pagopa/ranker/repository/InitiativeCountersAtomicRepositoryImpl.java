package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

@Repository
public class InitiativeCountersAtomicRepositoryImpl implements InitiativeCountersAtomicRepository {

    private static final String FIELD_ID = InitiativeCounters.Fields.id;
    private static final String FIELD_ONBOARDED = InitiativeCounters.Fields.onboarded;
    private static final String FIELD_SPENT_BUDGET_CENTS = InitiativeCounters.Fields.spentInitiativeBudgetCents;
    private static final String FIELD_RESERVED_BUDGET_CENTS = InitiativeCounters.Fields.reservedInitiativeBudgetCents;
    private static final String FIELD_RESIDUAL_BUDGET_CENTS = InitiativeCounters.Fields.residualInitiativeBudgetCents;

    private final MongoTemplate mongoTemplate;

    public InitiativeCountersAtomicRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public InitiativeCounters incrementOnboardedAndBudget(String initiativeId, long reservationCents) {

        Query query = Query.query(Criteria
                .where(FIELD_ID).is(initiativeId)
                .and(FIELD_RESIDUAL_BUDGET_CENTS).gte(reservationCents)
        );

        Update update = new Update()
                .inc(FIELD_ONBOARDED, 1L)
                .inc(FIELD_RESERVED_BUDGET_CENTS, reservationCents)
                .inc(FIELD_RESIDUAL_BUDGET_CENTS, -reservationCents);

        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                InitiativeCounters.class
        );
    }

    @Override
    public InitiativeCounters decrementOnboardedAndBudget(String initiativeId, long reservationCents) {
        Query query = Query.query(Criteria
                .where(FIELD_ID).is(initiativeId)
        );

        Update update = new Update()
                .inc(FIELD_ONBOARDED, -1L)
                .inc(FIELD_RESERVED_BUDGET_CENTS, -reservationCents)
                .inc(FIELD_RESIDUAL_BUDGET_CENTS, +reservationCents);

        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                InitiativeCounters.class
        );
    }

    @Override
    public InitiativeCounters updateCounterForCaptured(String initiativeId, Long spentVoucherAmountCents, Long voucherAmountCents) {
        Query query = Query.query(Criteria
                .where(FIELD_ID).is(initiativeId)
        );

        Update update = new Update()
                .inc(FIELD_SPENT_BUDGET_CENTS, spentVoucherAmountCents)
                .inc(FIELD_RESERVED_BUDGET_CENTS, -voucherAmountCents)
                .inc(FIELD_RESIDUAL_BUDGET_CENTS, voucherAmountCents - spentVoucherAmountCents);

        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                InitiativeCounters.class
        );
    }

    @Override
    public InitiativeCounters updateCounterForRefunded(String initiativeId, Long spentVoucherAmountCents) {
        Query query = Query.query(Criteria
                .where(FIELD_ID).is(initiativeId)
        );

        Update update = new Update()
                .inc(FIELD_SPENT_BUDGET_CENTS, -spentVoucherAmountCents)
                .inc(FIELD_RESIDUAL_BUDGET_CENTS, spentVoucherAmountCents);

        return mongoTemplate.findAndModify(
                query,
                update,
                FindAndModifyOptions.options().returnNew(true).upsert(true),
                InitiativeCounters.class
        );
    }

}
