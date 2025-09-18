package it.gov.pagopa.ranker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.ranker.connector.event.producer.RankerProducer;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.repo.MongoRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RankerService {

    private final MongoRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();

    private final RankerProducer rankerProducer;

    public RankerService(MongoRepository repo, RankerProducer rankerProducer) {
        this.repo = repo;
        this.rankerProducer = rankerProducer;
    }

    public void checkBudget(OnboardingDTO onboardingDTO) {
        log.info("Reading message: " + onboardingDTO);
        rankerProducer.sendSaveConsent(onboardingDTO);

    }
}

