package it.gov.pagopa.ranker.service.initative;

import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.domain.model.InitiativeCountersPreallocations;
import it.gov.pagopa.ranker.enums.PreallocationStatus;
import it.gov.pagopa.ranker.exception.BudgetExhaustedException;
import it.gov.pagopa.ranker.exception.UnableToAddSeqException;
import it.gov.pagopa.ranker.repository.InitiativeCountersPreallocationsRepository;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class InitiativeCountersServiceImpl implements InitiativeCountersService {

    public static final String ID_SEPARATOR = "_";
    private final List<String> initiativeId;

    private final InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;
    private final InitiativeCountersRepository initiativeCountersRepository;

    public InitiativeCountersServiceImpl(InitiativeCountersRepository initiativeCounterRepository,
                                         @Value("${app.initiative.identified}") List<String> initiativeId,
                                         InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository) {
        this.initiativeCountersRepository = initiativeCounterRepository;
        this.initiativeId = initiativeId;
        this.initiativeCountersPreallocationsRepository = initiativeCountersPreallocationsRepository;
    }

    public boolean existsByInitiativeIdAndUserId(String initiativeId, String userId){
        return initiativeCountersPreallocationsRepository.existsById(userId+ID_SEPARATOR+initiativeId);
    }

    @Override
    public void removeMessageToProcessOnInitative(String initativeId, Long sequenceId) {
        if (Objects.isNull(initiativeCountersRepository.removeUnprocessedSequenceId(initativeId, sequenceId))) {
            throw new RuntimeException("[RANKER_PROCESSOR] Unable to remove sequenceId " + sequenceId +
                    " on initiative " + initiativeId);
        }
    }

    @Override
    public Set<Pair<String,Long>> getMessageToProcess() {
        Set<Pair<String,Long>> orderedPairs = new TreeSet<>(Comparator.comparing(Pair::getRight));
        Set<Pair<String,Long>> counters = initiativeCountersRepository.findExistingSequenceIdToProcess(initiativeId).stream().map(
                initiativeCounters -> {
                    return initiativeCounters.getSequenceIdsToProcess().stream().map(
                            seqId -> Pair.of(initiativeCounters.getId(), seqId)).collect(Collectors.toSet());
                }).reduce((setToAdd, accumulator) -> {
            accumulator.addAll(setToAdd);
            return accumulator;
        }).orElse(new HashSet<>());
        orderedPairs.addAll(counters);
        return orderedPairs;
    }

    @Override
    public void addMessageProcessOnInitiative(long sequenceNumber, String initiativeId) {
       if (Objects.isNull(initiativeCountersRepository.addSequenceToInitiative(initiativeId, sequenceNumber))) {
            throw new UnableToAddSeqException("[RANKER_PROCESSOR] Unable to add sequenceId on initiative " + initiativeId);
        }
    }

    @Transactional
    public void addPreallocatedUser(String initiativeId, String userId,
                                    boolean verifyIsee, Long sequenceNumber, LocalDateTime enqueuedTime) {
        long reservationCents = calculateReservationCents(verifyIsee);

        try {
            InitiativeCounters initiativeCounters =
                    initiativeCountersRepository.incrementOnboardedAndBudget(
                    initiativeId,
                    reservationCents
            );
            if (initiativeCounters == null) {
                throw new BudgetExhaustedException("Missing budget on initiative " + initiativeId);
            }

            initiativeCountersPreallocationsRepository.insert(
                    InitiativeCountersPreallocations.builder()
                            .id(userId+ID_SEPARATOR+initiativeId)
                            .initiativeId(initiativeId)
                            .userId(userId)
                            .sequenceNumber(sequenceNumber)
                            .enqueuedTime(enqueuedTime)
                            .createdAt(LocalDateTime.now())
                            .status(PreallocationStatus.PREALLOCATED)
                            .preallocatedAmountCents(reservationCents)
                            .build()
            );

        } catch (BudgetExhaustedException e){
            //CosmosDB throw DuplicateKey even if the residualInitiativeBudgetCents is less than the minimum required and is not really a duplicated id
            log.error("[RANKER] Budget exhausted for the initiative {}", initiativeId);
            throw new BudgetExhaustedException("[RANKER] Budget exhausted for the initiative: " + initiativeId, e);
        }
    }

    @Override
    public boolean hasAvailableBudget() {
        return initiativeCountersRepository.existsByIdInAndResidualInitiativeBudgetCentsGreaterThanEqual(
                initiativeId, 10000);
    }

    public long calculateReservationCents(boolean verifyIsee) {
        return verifyIsee ? 20000L : 10000L;
    }
}
