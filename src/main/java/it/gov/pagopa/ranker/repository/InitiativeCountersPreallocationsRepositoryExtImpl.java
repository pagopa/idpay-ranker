package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.InitiativeCountersPreallocations;
import it.gov.pagopa.ranker.enums.PreallocationStatus;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.time.LocalDateTime;
import java.time.ZoneId;


public class InitiativeCountersPreallocationsRepositoryExtImpl implements InitiativeCountersPreallocationsRepositoryExt {

    private final MongoTemplate mongoTemplate;

    public InitiativeCountersPreallocationsRepositoryExtImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public boolean findByIdAndStatusThenUpdateStatus(String id, PreallocationStatus currentStatus, PreallocationStatus newStatus) {
        InitiativeCountersPreallocations initiativeCountersPreallocations = mongoTemplate.findAndModify(
                Query.query(
                        Criteria.where(InitiativeCountersPreallocations.Fields.id).is(id)
                                .and(InitiativeCountersPreallocations.Fields.status).is(currentStatus)),
                new Update()
                        .set(InitiativeCountersPreallocations.Fields.status, newStatus)
                        .set(InitiativeCountersPreallocations.Fields.updateDate, LocalDateTime.now(ZoneId.of("Europe/Rome"))),
                FindAndModifyOptions.options().returnNew(true),
                InitiativeCountersPreallocations.class);
        return initiativeCountersPreallocations != null;
    }

    @Override
    public boolean findByIdAndStatusThenUpdateStatusCreatedBefore(
            String id,
            PreallocationStatus currentStatus,
            PreallocationStatus newStatus,
            LocalDateTime createdAtUpperBound) {
        Criteria currentAttemptCriteria = Criteria.where(InitiativeCountersPreallocations.Fields.attemptStartAt)
                .lte(createdAtUpperBound);
        Criteria legacyAttemptCriteria = new Criteria().andOperator(
                Criteria.where(InitiativeCountersPreallocations.Fields.attemptStartAt).exists(false),
                Criteria.where(InitiativeCountersPreallocations.Fields.createdAt).lte(createdAtUpperBound)
        );
        Criteria idAndStatusCriteria = Criteria.where(InitiativeCountersPreallocations.Fields.id).is(id)
                .and(InitiativeCountersPreallocations.Fields.status).is(currentStatus);
        Criteria attemptWindowCriteria = new Criteria().orOperator(currentAttemptCriteria, legacyAttemptCriteria);

        InitiativeCountersPreallocations initiativeCountersPreallocations = mongoTemplate.findAndModify(
                Query.query(new Criteria().andOperator(idAndStatusCriteria, attemptWindowCriteria)),
                new Update()
                        .set(InitiativeCountersPreallocations.Fields.status, newStatus)
                        .set(InitiativeCountersPreallocations.Fields.updateDate, LocalDateTime.now(ZoneId.of("Europe/Rome"))),
                FindAndModifyOptions.options().returnNew(true),
                InitiativeCountersPreallocations.class);
        return initiativeCountersPreallocations != null;
    }
}
