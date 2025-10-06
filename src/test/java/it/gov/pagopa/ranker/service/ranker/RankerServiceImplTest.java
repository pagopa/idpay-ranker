//package it.gov.pagopa.ranker.service.ranker;
//
//import it.gov.pagopa.ranker.connector.event.producer.RankerProducer;
//import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
//import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
//import it.gov.pagopa.ranker.domain.model.Preallocation;
//import it.gov.pagopa.ranker.exception.MessageProcessingException;
//import it.gov.pagopa.ranker.exception.ResourceNotReadyException;
//import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
//import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.springframework.cloud.stream.binding.BindingsLifecycleController;
//import org.springframework.cloud.stream.binder.Binding;
//
//import java.time.LocalDateTime;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.Optional;
//
//import static it.gov.pagopa.ranker.constants.ErrorMessages.RESOURCE_NOT_READY;
//import static it.gov.pagopa.ranker.enums.PreallocationStatus.PREALLOCATED;
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.*;
//import static org.mockito.Mockito.*;
//
//class RankerServiceImplTest {
//
//    private RankerProducer rankerProducer;
//    private InitiativeCountersRepository initiativeCountersRepository;
//    private InitiativeCountersService initiativeCountersService;
//    private BindingsLifecycleController bindingsLifecycleController;
//    private RankerServiceImpl rankerServiceImpl;
//    private Binding<?> mockBinding;
//
//    @BeforeEach
//    void setUp() {
//        rankerProducer = mock(RankerProducer.class);
//        initiativeCountersRepository = mock(InitiativeCountersRepository.class);
//        initiativeCountersService = mock(InitiativeCountersService.class);
//        bindingsLifecycleController = mock(BindingsLifecycleController.class);
//        mockBinding = mock(Binding.class);
//
//        rankerServiceImpl = new RankerServiceImpl(rankerProducer,
//                initiativeCountersRepository,
//                initiativeCountersService,
//                bindingsLifecycleController);
//
//        rankerServiceImpl.onBindingCreated(new org.springframework.cloud.stream.binder.BindingCreatedEvent(mockBinding) {
//            @Override
//            public Object getSource() {
//                when(mockBinding.getBindingName()).thenReturn("rankerProcessor-in-0");
//                return mockBinding;
//            }
//        });
//    }
//
//    @Test
//    void testUserAlreadyPresentUser() {
//        OnboardingDTO dto = OnboardingDTO.builder()
//                .userId("user1")
//                .initiativeId("initiative1")
//                .build();
//
//        Map<String, Preallocation> preallocationMap = new HashMap<>();
//        preallocationMap.put("user1", Preallocation.builder()
//                .userId("user1")
//                .status(PREALLOCATED)
//                .createdAt(LocalDateTime.now())
//                .build());
//
//        InitiativeCounters counters = InitiativeCounters.builder()
//                .id("initiative1")
//                .preallocationMap(preallocationMap)
//                .residualInitiativeBudgetCents(200L)
//                .build();
//
//        when(initiativeCountersRepository.findById("initiative1")).thenReturn(Optional.of(counters));
//        when(initiativeCountersRepository.existsByInitiativeIdAndUserId("initiative1", "user1")).thenReturn(true);
//
//        rankerServiceImpl.execute(dto, 123L, 456L);
//
//        verify(initiativeCountersService, never())
//                .addedPreallocatedUser(anyString(), anyString(), anyBoolean(), anyLong(), anyLong());
//        verifyNoInteractions(rankerProducer);
//    }
//
//    @Test
//    void testUserNotPreallocatedYet() {
//        OnboardingDTO dto = OnboardingDTO.builder()
//                .userId("user2")
//                .initiativeId("initiative1")
//                .verifyIsee(true)
//                .build();
//
//        InitiativeCounters counters = InitiativeCounters.builder()
//                .id("initiative1")
//                .preallocationMap(new HashMap<>())
//                .residualInitiativeBudgetCents(200L)
//                .build();
//
//        when(initiativeCountersRepository.findById("initiative1")).thenReturn(Optional.of(counters));
//        when(initiativeCountersService.addedPreallocatedUser(
//                eq("initiative1"),
//                eq("user2"),
//                eq(true),
//                anyLong(),
//                anyLong()
//        )).thenReturn(1L);
//
//        rankerServiceImpl.execute(dto, 123L, 456L);
//
//        verify(initiativeCountersService, times(1)).addedPreallocatedUser(
//                eq("initiative1"),
//                eq("user2"),
//                eq(true),
//                eq(123L),
//                eq(456L)
//        );
//        verify(rankerProducer, times(1)).sendSaveConsent(dto);
//    }
//
//    @Test
//    void testPauseConsumerWhenBudgetBelowThreshold() {
//        OnboardingDTO dto = OnboardingDTO.builder()
//                .userId("user3")
//                .initiativeId("initiativeLowBudget")
//                .verifyIsee(true)
//                .build();
//
//        InitiativeCounters counters = InitiativeCounters.builder()
//                .id("initiativeLowBudget")
//                .preallocationMap(new HashMap<>())
//                .residualInitiativeBudgetCents(50L)
//                .build();
//
//        when(initiativeCountersRepository.findById("initiativeLowBudget"))
//                .thenReturn(Optional.of(counters));
//        when(initiativeCountersRepository.existsByInitiativeIdAndUserId("initiativeLowBudget", "user3"))
//                .thenReturn(false);
//        when(initiativeCountersService.addedPreallocatedUser(
//                eq("initiativeLowBudget"),
//                eq("user3"),
//                eq(true),
//                anyLong(),
//                anyLong()))
//                .thenReturn(1L);
//
//        rankerServiceImpl.execute(dto, 1L, 1L);
//
//        verify(bindingsLifecycleController, times(1))
//                .changeState("rankerProcessor-in-0", BindingsLifecycleController.State.PAUSED);
//    }
//
//    @Test
//    void testExecuteWhenConsumerPausedThrowsException() {
//        rankerServiceImpl.pauseConsumer();
//
//        OnboardingDTO dto = OnboardingDTO.builder()
//                .userId("user4")
//                .initiativeId("initiative1")
//                .build();
//
//        MessageProcessingException exception = assertThrows(MessageProcessingException.class,
//                () -> rankerServiceImpl.execute(dto, 1L, 1L));
//
//        assertEquals("Consumer paused, message not processed", exception.getMessage());
//    }
//
//    @Test
//    void testResumeConsumer() {
//        rankerServiceImpl.pauseConsumer();
//        rankerServiceImpl.resumeConsumer();
//
//        verify(bindingsLifecycleController, times(1))
//                .changeState("rankerProcessor-in-0", BindingsLifecycleController.State.STARTED);
//    }
//
//    @Test
//    void testInitiativeNotExists() {
//        OnboardingDTO dto = OnboardingDTO.builder()
//                .userId("user5")
//                .initiativeId("initiativeX")
//                .build();
//
//        when(initiativeCountersRepository.findById("initiativeX")).thenReturn(Optional.empty());
//
//        ResourceNotReadyException exception = assertThrows(ResourceNotReadyException.class,
//                () -> rankerServiceImpl.execute(dto, 1L, 2L));
//
//        assertEquals(RESOURCE_NOT_READY, exception.getMessage());
//    }
//
//    @Test
//    void testOnBindingCreatedWithDifferentName() {
//        Binding<?> anotherBinding = mock(Binding.class);
//        when(anotherBinding.getBindingName()).thenReturn("anotherBinding-in-0");
//
//        rankerServiceImpl.onBindingCreated(new org.springframework.cloud.stream.binder.BindingCreatedEvent(anotherBinding) {
//            @Override
//            public Object getSource() {
//                return anotherBinding;
//            }
//        });
//
//        assertNotSame(anotherBinding, getRankerProcessorBinding(rankerServiceImpl));
//    }
//
//    @Test
//    void testPauseConsumerWithNoBinding() {
//        setRankerProcessorBinding(rankerServiceImpl, null);
//        rankerServiceImpl.pauseConsumer();
//        assertFalse(getConsumerPaused(rankerServiceImpl));
//    }
//
//    @Test
//    void testResumeConsumerWithNoBindingOrNotPaused() {
//        setConsumerPaused(rankerServiceImpl, false);
//        setRankerProcessorBinding(rankerServiceImpl, mockBinding);
//        rankerServiceImpl.resumeConsumer();
//        verify(bindingsLifecycleController, never()).changeState(anyString(), any());
//
//        setConsumerPaused(rankerServiceImpl, true);
//        setRankerProcessorBinding(rankerServiceImpl, null);
//        rankerServiceImpl.resumeConsumer();
//        verify(bindingsLifecycleController, never()).changeState(anyString(), any());
//    }
//
//    private Binding<?> getRankerProcessorBinding(RankerServiceImpl service) {
//        try {
//            var field = RankerServiceImpl.class.getDeclaredField("rankerProcessorBinding");
//            field.setAccessible(true);
//            return (Binding<?>) field.get(service);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private void setRankerProcessorBinding(RankerServiceImpl service, Binding<?> binding) {
//        try {
//            var field = RankerServiceImpl.class.getDeclaredField("rankerProcessorBinding");
//            field.setAccessible(true);
//            field.set(service, binding);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private boolean getConsumerPaused(RankerServiceImpl service) {
//        try {
//            var field = RankerServiceImpl.class.getDeclaredField("consumerPaused");
//            field.setAccessible(true);
//            return (boolean) field.get(service);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//
//    private void setConsumerPaused(RankerServiceImpl service, boolean value) {
//        try {
//            var field = RankerServiceImpl.class.getDeclaredField("consumerPaused");
//            field.setAccessible(true);
//            field.set(service, value);
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//    }
//}
