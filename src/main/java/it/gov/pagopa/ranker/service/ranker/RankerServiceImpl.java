package it.gov.pagopa.ranker.service.ranker;

import it.gov.pagopa.ranker.connector.event.producer.RankerProducer;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.exception.ResourceNotReadyException;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class RankerServiceImpl implements RankerService {

    private final RankerProducer rankerProducer;
    private final InitiativeCountersRepository initiativeCountersRepository;
    private final InitiativeCountersService initiativeCountersService;

    public RankerServiceImpl(RankerProducer rankerProducer,
                             InitiativeCountersRepository initiativeCountersRepository,
                             InitiativeCountersService initiativeCountersService) {
        this.rankerProducer = rankerProducer;
        this.initiativeCountersRepository = initiativeCountersRepository;
        this.initiativeCountersService = initiativeCountersService;
    }

    @Override
    public void execute(OnboardingDTO dto) {
        log.info("Processing message: {}", dto);

        Optional<InitiativeCounters> optional = initiativeCountersRepository.findById(dto.getInitiativeId());

        if (optional.isEmpty()) {
            log.error("Counter or initiative {} aren't ready yet", dto.getInitiativeId());
            throw new ResourceNotReadyException();
        }

        InitiativeCounters existing = optional.get();

        if(existing.getPreallocationMap() != null && existing.getPreallocationMap().containsKey(dto.getUserId())){
            log.info("User {} already preallocated for initiative {}", dto.getUserId(), dto.getInitiativeId());
            return;
        }

        initiativeCountersService.addedPreallocatedUser(
                dto.getInitiativeId(),
                dto.getUserId(),
                Boolean.TRUE.equals(dto.getVerifyIsee())
        );

        log.info("Preallocation added for user {} in initiative {}", dto.getUserId(), dto.getInitiativeId());
        rankerProducer.sendSaveConsent(dto);
    }
}
