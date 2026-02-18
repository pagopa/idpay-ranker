package it.gov.pagopa.common.mongo;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DummySpringRepository extends MongoRepository<DummySpringRepository.DummyMongoCollection, String> {
    DummyMongoCollection findByIdOrderById(String id);

    @Document("beneficiary_rule")
    class DummyMongoCollection {
        @Id
        private String id;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }
    }
}
