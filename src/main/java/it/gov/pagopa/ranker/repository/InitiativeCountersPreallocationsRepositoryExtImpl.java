package it.gov.pagopa.ranker.repository;

import it.gov.pagopa.ranker.domain.model.InitiativeCountersPreallocations;
import it.gov.pagopa.ranker.enums.PreallocationStatus;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;


public class InitiativeCountersPreallocationsRepositoryExtImpl implements InitiativeCountersPreallocationsRepositoryExt {

    private final MongoTemplate mongoTemplate;

    public InitiativeCountersPreallocationsRepositoryExtImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public boolean findByIdAndStatusThenUpdateStatusToCaptured(String id, PreallocationStatus status) {
        InitiativeCountersPreallocations initiativeCountersPreallocations = mongoTemplate.findAndModify(
                Query.query(
                        Criteria.where(InitiativeCountersPreallocations.Fields.id).is(id)
                                .and(InitiativeCountersPreallocations.Fields.status).is(status)),
                new Update()
                        .set(InitiativeCountersPreallocations.Fields.status, PreallocationStatus.CAPTURED),
                FindAndModifyOptions.options().returnNew(true),
                InitiativeCountersPreallocations.class);
        return initiativeCountersPreallocations != null;
    }
}
