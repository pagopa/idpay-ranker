package it.gov.pagopa.ranker.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.repo.MongoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class RankerService {

    private static final Logger log = LoggerFactory.getLogger(RankerService.class);
    private final MongoRepository repo;
    private final ObjectMapper mapper = new ObjectMapper();

    public RankerService(MongoRepository repo) {
        this.repo = repo;
    }

    public void checkBudget(OnboardingDTO onboardingDTO) {
        //create checkBudget
    }
}

