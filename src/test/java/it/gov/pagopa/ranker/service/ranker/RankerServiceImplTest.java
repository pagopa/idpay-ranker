package it.gov.pagopa.ranker.service.ranker;

import it.gov.pagopa.ranker.connector.event.producer.RankerProducer;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.domain.model.Preallocation;
import it.gov.pagopa.ranker.enums.PreallocationStatus;
import it.gov.pagopa.ranker.exception.ResourceNotReadyException;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.ranker.constants.ErrorMessages.RESOURCE_NOT_READY;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RankerServiceImplTest {

    private RankerProducer rankerProducer;
    private InitiativeCountersRepository initiativeCountersRepository;
    private InitiativeCountersService initiativeCountersService;
    private RankerServiceImpl rankerServiceImpl;

    @BeforeEach
    void setUp() {
        rankerProducer = mock(RankerProducer.class);
        initiativeCountersRepository = mock(InitiativeCountersRepository.class);
        initiativeCountersService = mock(InitiativeCountersService.class);
        rankerServiceImpl = new RankerServiceImpl(rankerProducer, initiativeCountersRepository, initiativeCountersService);
    }

    @Test
    void testUserAlreadyPreallocated() {
        OnboardingDTO dto = OnboardingDTO.builder()
                .userId("user1")
                .initiativeId("initiative1")
                .build();

        Preallocation preallocation = Preallocation.builder()
                .userId("user1")
                .status(PreallocationStatus.PREALLOCATED)
                .createdAt(LocalDateTime.now())
                .build();

        InitiativeCounters counters = InitiativeCounters.builder()
                .id("initiative1")
                .preallocationList(new ArrayList<>(List.of(preallocation)))
                .build();

        when(initiativeCountersRepository.findById("initiative1"))
                .thenReturn(Optional.of(counters));

        rankerServiceImpl.execute(dto);

        verifyNoInteractions(rankerProducer);
        verifyNoInteractions(initiativeCountersService);
        verify(initiativeCountersRepository, never()).save(any());
    }

    @Test
    void testUserNotPreallocatedYet() {
        OnboardingDTO dto = OnboardingDTO.builder()
                .userId("user2")
                .initiativeId("initiative1")
                .verifyIsee(true)
                .build();

        InitiativeCounters counters = InitiativeCounters.builder()
                .id("initiative1")
                .preallocationList(new ArrayList<>())
                .build();

        when(initiativeCountersRepository.findById("initiative1"))
                .thenReturn(Optional.of(counters));
        when(initiativeCountersService.addedPreallocatedUser("initiative1", "user2", true)).thenReturn(1L);

        rankerServiceImpl.execute(dto);

        verify(initiativeCountersService, times(1)).addedPreallocatedUser("initiative1", "user2", true);
        verify(rankerProducer, times(1)).sendSaveConsent(dto);
        verify(initiativeCountersRepository, never()).save(any());
    }

    @Test
    void testInitiativeNotExists() {
        OnboardingDTO dto = OnboardingDTO.builder()
                .userId("user3")
                .initiativeId("initiativeNew")
                .build();

        when(initiativeCountersRepository.findById("initiativeNew")).thenReturn(Optional.empty());

        ResourceNotReadyException exception = assertThrows(ResourceNotReadyException.class, () -> rankerServiceImpl.execute(dto));

        assertEquals(RESOURCE_NOT_READY, exception.getMessage());
        verifyNoInteractions(initiativeCountersService);
        verifyNoInteractions(rankerProducer);
    }
}
