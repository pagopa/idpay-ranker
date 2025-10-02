package it.gov.pagopa.ranker.service.ranker;

import it.gov.pagopa.ranker.connector.event.producer.RankerProducer;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.domain.model.Preallocation;
import it.gov.pagopa.ranker.exception.MessageProcessingException;
import it.gov.pagopa.ranker.exception.ResourceNotReadyException;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static it.gov.pagopa.ranker.constants.ErrorMessages.RESOURCE_NOT_READY;
import static it.gov.pagopa.ranker.enums.PreallocationStatus.PREALLOCATED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
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
    void testUserAlreadyPresentUser() {
        OnboardingDTO dto = OnboardingDTO.builder()
                .userId("user1")
                .initiativeId("initiative1")
                .build();

        Map<String, Preallocation> preallocationMap = new HashMap<>();
        preallocationMap.put("user1", Preallocation.builder()
                .userId("user1")
                .status(PREALLOCATED)
                .createdAt(LocalDateTime.now())
                .build());

        InitiativeCounters counters = InitiativeCounters.builder()
                .id("initiative1")
                .preallocationMap(preallocationMap)
                .residualInitiativeBudgetCents(200L)
                .build();

        when(initiativeCountersRepository.findById("initiative1")).thenReturn(Optional.of(counters));
        when(initiativeCountersRepository.existsByInitiativeIdAndUserId("initiative1", "user1")).thenReturn(true);

        rankerServiceImpl.execute(dto, 123L, 456L);

        verify(initiativeCountersService, never())
                .addedPreallocatedUser(anyString(), anyString(), anyBoolean(), anyLong(), anyLong());
        verifyNoInteractions(rankerProducer);
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
                .preallocationMap(new HashMap<>())
                .build();

        when(initiativeCountersRepository.findById("initiative1")).thenReturn(Optional.of(counters));
        when(initiativeCountersService.addedPreallocatedUser(
                eq("initiative1"),
                eq("user2"),
                eq(true),
                anyLong(),
                anyLong()
        )).thenReturn(1L);

        rankerServiceImpl.execute(dto, 123L, 456L);

        verify(initiativeCountersService, times(1)).addedPreallocatedUser(
                eq("initiative1"),
                eq("user2"),
                eq(true),
                eq(123L),
                eq(456L)
        );
        verify(rankerProducer, times(1)).sendSaveConsent(dto);
    }

    @Test
    void testPreallocationMapNull() {
        OnboardingDTO dto = OnboardingDTO.builder()
                .userId("user3")
                .initiativeId("initiative1")
                .verifyIsee(false)
                .build();

        InitiativeCounters counters = InitiativeCounters.builder()
                .id("initiative1")
                .preallocationMap(null)
                .build();

        when(initiativeCountersRepository.findById("initiative1")).thenReturn(Optional.of(counters));
        when(initiativeCountersService.addedPreallocatedUser(
                eq("initiative1"),
                eq("user3"),
                eq(false),
                anyLong(),
                anyLong()
        )).thenReturn(1L);

        rankerServiceImpl.execute(dto, 10L, 20L);

        verify(initiativeCountersService, times(1)).addedPreallocatedUser(
                eq("initiative1"),
                eq("user3"),
                eq(false),
                eq(10L),
                eq(20L)
        );
        verify(rankerProducer, times(1)).sendSaveConsent(dto);
    }

    @Test
    void testInitiativeNotExists() {
        OnboardingDTO dto = OnboardingDTO.builder()
                .userId("user4")
                .initiativeId("initiativeX")
                .build();

        when(initiativeCountersRepository.findById("initiativeX")).thenReturn(Optional.empty());

        ResourceNotReadyException exception = assertThrows(ResourceNotReadyException.class,
                () -> rankerServiceImpl.execute(dto, 1L, 2L));

        assertEquals(RESOURCE_NOT_READY, exception.getMessage());
        verifyNoInteractions(initiativeCountersService);
        verifyNoInteractions(rankerProducer);
    }

    @Test
    void testExecuteWhenConsumerInactiveThrowsException() {
        OnboardingDTO dto = OnboardingDTO.builder()
                .userId("userInactive")
                .initiativeId("initiative1")
                .build();

        rankerServiceImpl.stopConsumer();
        assertFalse(getConsumerActive(rankerServiceImpl));

        MessageProcessingException exception = assertThrows(MessageProcessingException.class,
                () -> rankerServiceImpl.execute(dto, 1L, 1L));

        assertEquals("Consumer inactive, message not processed", exception.getMessage());

        verifyNoInteractions(initiativeCountersService);
        verifyNoInteractions(rankerProducer);
    }

    @Test
    void testStartConsumerActivatesConsumerOnlyIfInactive() {
        rankerServiceImpl.stopConsumer();
        assertFalse(getConsumerActive(rankerServiceImpl));

        rankerServiceImpl.startConsumer();
        assertTrue(getConsumerActive(rankerServiceImpl));

        rankerServiceImpl.startConsumer();
        assertTrue(getConsumerActive(rankerServiceImpl));
    }

    @Test
    void testStopConsumerDeactivatesConsumerOnlyIfActive() {
        rankerServiceImpl.startConsumer();
        assertTrue(getConsumerActive(rankerServiceImpl));

        rankerServiceImpl.stopConsumer();
        assertFalse(getConsumerActive(rankerServiceImpl));

        rankerServiceImpl.stopConsumer();
        assertFalse(getConsumerActive(rankerServiceImpl));
    }

    @Test
    void testExecuteStopsConsumerWhenBudgetBelowThreshold() {
        rankerServiceImpl.startConsumer();

        OnboardingDTO dto = OnboardingDTO.builder()
                .userId("user5")
                .initiativeId("initiativeLowBudget")
                .verifyIsee(true)
                .build();

        InitiativeCounters counters = InitiativeCounters.builder()
                .id("initiativeLowBudget")
                .preallocationMap(new HashMap<>())
                .residualInitiativeBudgetCents(50L)
                .build();

        when(initiativeCountersRepository.findById("initiativeLowBudget"))
                .thenReturn(Optional.of(counters));

        when(initiativeCountersRepository.existsByInitiativeIdAndUserId("initiativeLowBudget", "user5"))
                .thenReturn(false);

        when(initiativeCountersService.addedPreallocatedUser(
                eq("initiativeLowBudget"),
                eq("user5"),
                eq(true),
                anyLong(),
                anyLong()))
                .thenReturn(1L);

        rankerServiceImpl.execute(dto, 1L, 1L);

        assertFalse(getConsumerActive(rankerServiceImpl));

        verify(initiativeCountersService, times(1)).addedPreallocatedUser(
                eq("initiativeLowBudget"),
                eq("user5"),
                eq(true),
                eq(1L),
                eq(1L)
        );
        verify(rankerProducer, times(1)).sendSaveConsent(dto);
    }

    private boolean getConsumerActive(RankerServiceImpl service) {
        try {
            var field = RankerServiceImpl.class.getDeclaredField("consumerActive");
            field.setAccessible(true);
            return (boolean) field.get(service);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
