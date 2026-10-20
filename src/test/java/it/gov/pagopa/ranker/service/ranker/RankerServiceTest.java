package it.gov.pagopa.ranker.service.ranker;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import it.gov.pagopa.ranker.connector.event.producer.RankerProducer;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.domain.dto.VerifyDTO;
import it.gov.pagopa.ranker.domain.mapper.ConsentMapper;
import it.gov.pagopa.ranker.domain.model.InitiativeCountersPreallocations;
import it.gov.pagopa.ranker.domain.model.Onboarding;
import it.gov.pagopa.ranker.enums.PreallocationStatus;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.ranker.repository.OnboardingRepository;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.ranker.constants.OnboardingConstant.ON_EVALUATION;
import static it.gov.pagopa.ranker.constants.OnboardingConstant.UNSUBSCRIBED;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankerServiceTest {

    @Mock
    private RankerProducer rankerProducer;

    @Mock
    private InitiativeCountersRepository initiativeCountersRepository;

    @Mock
    private InitiativeCountersService initiativeCountersService;

    @Mock
    private OnboardingRepository onboardingRepository;

    @Mock
    private ConsentMapper consentMapper;

    private ObjectMapper objectMapper;
    private RankerService rankerService;
    private final List<String> initiatives = List.of("INITIATIVE_ID");

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        rankerService = new RankerServiceImpl(
                rankerProducer,
                initiativeCountersService,
                onboardingRepository,
                consentMapper,
                objectMapper,
                initiatives
        );
    }

    private ServiceBusReceivedMessage buildMessage(OnboardingDTO dto) {
        String json = objectMapper.writeValueAsString(dto);
        ServiceBusMessage sbMessage = new ServiceBusMessage(json);
        ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(message.getBody()).thenReturn(sbMessage.getBody());
        when(message.getSequenceNumber()).thenReturn(99L);
        when(message.getEnqueuedTime()).thenReturn(OffsetDateTime.of(
                LocalDateTime.of(2025, 1, 1, 12, 0),
                ZoneOffset.ofHours(1)));
        return message;
    }

    private ServiceBusReceivedMessage buildMessage(OnboardingDTO dto, long sequenceNumber, OffsetDateTime enqueuedTime) {
        String json = objectMapper.writeValueAsString(dto);
        ServiceBusMessage sbMessage = new ServiceBusMessage(json);
        ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(message.getBody()).thenReturn(sbMessage.getBody());
        when(message.getSequenceNumber()).thenReturn(sequenceNumber);
        when(message.getEnqueuedTime()).thenReturn(enqueuedTime);
        return message;
    }

    @Test
    void testExecute_whenNewPreallocation_shouldAddAndSend() {
        List<VerifyDTO> mockVerifies = List.of();
        OnboardingDTO dto = new OnboardingDTO();
        dto.setInitiativeId(initiatives.getFirst());
        dto.setUserId("USR001");
        dto.setVerifies(mockVerifies);

        ServiceBusReceivedMessage message = buildMessage(dto);
        when(initiativeCountersService.findById(initiatives.getFirst(), "USR001")).thenReturn(Optional.empty());

        rankerService.execute(message);

        verify(initiativeCountersService).addPreallocatedUser(
                eq(initiatives.getFirst()),
                eq("USR001"),
                eq(mockVerifies),
                eq(99L),
                any(LocalDateTime.class)
        );
        verify(rankerProducer).sendSaveConsent(any(OnboardingDTO.class));
    }

    @Test
    void testExecute_whenUserAlreadyPreallocated_shouldDoNothing(){
        OnboardingDTO dto = new OnboardingDTO();
        dto.setInitiativeId(initiatives.getFirst());
        dto.setUserId("USR_EXIST");
        dto.setVerifies(List.of());

        InitiativeCountersPreallocations preallocation = new InitiativeCountersPreallocations();
        preallocation.setStatus(PreallocationStatus.PREALLOCATED);

        ServiceBusReceivedMessage message = buildMessage(dto);
        when(initiativeCountersService.findById(initiatives.getFirst(), "USR_EXIST")).thenReturn(Optional.of(preallocation));

        rankerService.execute(message);

        verify(initiativeCountersService, never()).addPreallocatedUser(any(), any(), any(), anyLong(), any());
        verify(rankerProducer, never()).sendSaveConsent(any());
    }

    @Test
    void testExecute_whenDeserializationFails_shouldThrowIllegalStateException() {
        ServiceBusMessage badMessage = new ServiceBusMessage("{invalid-json}");
        ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(message.getBody()).thenReturn(badMessage.getBody());

        assertThrows(IllegalStateException.class, () -> rankerService.execute(message));

        verifyNoInteractions(rankerProducer, initiativeCountersRepository, initiativeCountersService);
    }

    @Test
    void testExecute_whenInternalServiceThrows_shouldWrapInMessageProcessingException() {
        OnboardingDTO dto = new OnboardingDTO();
        dto.setInitiativeId(initiatives.getFirst());
        dto.setUserId("USR_FAIL");

        ServiceBusReceivedMessage message = buildMessage(dto);

        when(initiativeCountersService.findById(any(), any()))
                .thenThrow(new DuplicateKeyException("MESSAGE"));

        DuplicateKeyException ex = assertThrows(DuplicateKeyException.class, () -> rankerService.execute(message));
        assertTrue(ex.getMessage().contains("MESSAGE"));
    }

    @Test
    void testExecute_whenVerifiesNull_shouldPassNullOrEmpty() {
        OnboardingDTO dto = new OnboardingDTO();
        dto.setInitiativeId(initiatives.getFirst());
        dto.setUserId("USR002");
        dto.setVerifies(null);

        ServiceBusReceivedMessage message = buildMessage(dto);
        when(initiativeCountersService.findById(initiatives.getFirst(), "USR002")).thenReturn(Optional.empty());

        rankerService.execute(message);

        verify(initiativeCountersService).addPreallocatedUser(
                eq(initiatives.getFirst()),
                eq("USR002"),
                isNull(),
                eq(99L),
                any(LocalDateTime.class)
        );
    }

    @Test
    void testExecute_AnotherInitiative() {
        OnboardingDTO dto = new OnboardingDTO();
        dto.setInitiativeId("another-initiative");
        dto.setUserId("USR002");
        dto.setVerifies(null);

        ServiceBusReceivedMessage message = buildMessage(dto);

        rankerService.execute(message);

        verify(initiativeCountersService, never()).addPreallocatedUser(any(), any(), any(), any(), any());
    }

    @Test
    void testExecute_whenRefundedAndReopenSucceeds_shouldResendConsent() {
        OnboardingDTO dto = new OnboardingDTO();
        dto.setInitiativeId(initiatives.getFirst());
        dto.setUserId("USR_REFUNDED");
        dto.setVerifies(List.of());

        InitiativeCountersPreallocations preallocation = new InitiativeCountersPreallocations();
        preallocation.setStatus(PreallocationStatus.REFUNDED);

        ServiceBusReceivedMessage message = buildMessage(dto);
        when(initiativeCountersService.findById(initiatives.getFirst(), "USR_REFUNDED"))
                .thenReturn(Optional.of(preallocation));
        when(initiativeCountersService.reopenPreallocatedUser(eq(initiatives.getFirst()), eq("USR_REFUNDED"), any(), eq(99L), any(LocalDateTime.class)))
                .thenReturn(true);

        rankerService.execute(message);

        verify(initiativeCountersService).reopenPreallocatedUser(eq(initiatives.getFirst()), eq("USR_REFUNDED"), any(), eq(99L), any(LocalDateTime.class));
        verify(initiativeCountersService, never()).addPreallocatedUser(any(), any(), any(), anyLong(), any());
        verify(rankerProducer).sendSaveConsent(any(OnboardingDTO.class));
    }

    @Test
    void testExecute_whenExpiredAndReopenFails_shouldNotResendConsent() {
        OnboardingDTO dto = new OnboardingDTO();
        dto.setInitiativeId(initiatives.getFirst());
        dto.setUserId("USR_EXPIRED");
        dto.setVerifies(List.of());

        InitiativeCountersPreallocations preallocation = new InitiativeCountersPreallocations();
        preallocation.setStatus(PreallocationStatus.EXPIRED);

        ServiceBusReceivedMessage message = buildMessage(dto);
        when(initiativeCountersService.findById(initiatives.getFirst(), "USR_EXPIRED"))
                .thenReturn(Optional.of(preallocation));
        when(initiativeCountersService.reopenPreallocatedUser(eq(initiatives.getFirst()), eq("USR_EXPIRED"), any(), eq(99L), any(LocalDateTime.class)))
                .thenReturn(false);

        rankerService.execute(message);

        verify(initiativeCountersService).reopenPreallocatedUser(eq(initiatives.getFirst()), eq("USR_EXPIRED"), any(), eq(99L), any(LocalDateTime.class));
        verify(rankerProducer, never()).sendSaveConsent(any(OnboardingDTO.class));
        verify(initiativeCountersService, never()).addPreallocatedUser(any(), any(), any(), anyLong(), any());
    }

    @Test
    void testExecute_whenRejected_shouldNotReopenAndShouldNotResendConsent() {
        OnboardingDTO dto = new OnboardingDTO();
        dto.setInitiativeId(initiatives.getFirst());
        dto.setUserId("USR_REJECTED");
        dto.setVerifies(List.of());

        InitiativeCountersPreallocations preallocation = new InitiativeCountersPreallocations();
        preallocation.setStatus(PreallocationStatus.REJECTED);

        ServiceBusReceivedMessage message = buildMessage(dto);
        when(initiativeCountersService.findById(initiatives.getFirst(), "USR_REJECTED"))
                .thenReturn(Optional.of(preallocation));

        rankerService.execute(message);

        verify(initiativeCountersService, never()).reopenPreallocatedUser(anyString(), anyString(), any(), anyLong(), any());
        verify(rankerProducer, never()).sendSaveConsent(any(OnboardingDTO.class));
        verify(initiativeCountersService, never()).addPreallocatedUser(any(), any(), any(), anyLong(), any());
    }


    @Test
    void testExecute_shouldPropagateMessageHeaderInAddPreallocatedUser() {
        OnboardingDTO dto = new OnboardingDTO();
        dto.setInitiativeId(initiatives.getFirst());
        dto.setUserId("USR_HEADER");
        dto.setVerifies(List.of());

        OffsetDateTime enqueuedTimeUtc = OffsetDateTime.of(2025, 6, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        ServiceBusReceivedMessage message = buildMessage(dto, 123L, enqueuedTimeUtc);
        when(initiativeCountersService.findById(initiatives.getFirst(), "USR_HEADER")).thenReturn(Optional.empty());

        rankerService.execute(message);

        verify(initiativeCountersService).addPreallocatedUser(
                eq(initiatives.getFirst()),
                eq("USR_HEADER"),
                any(),
                eq(123L),
                eq(LocalDateTime.of(2025, 6, 1, 12, 0))
        );
    }

    @Test
    void testRecovery_whenDataValid_shouldResendConsent() {
        OnboardingDTO input = new OnboardingDTO();
        input.setInitiativeId("INITIATIVE_ID");
        input.setUserId("USER123");
        input.setServiceId("SRV001");

        InitiativeCountersPreallocations pre = new InitiativeCountersPreallocations();
        pre.setPreallocatedAmountCents(15000L);

        Onboarding onboarding = new Onboarding("INITIATIVE_ID", "USER123");
        onboarding.setStatus(ON_EVALUATION);

        when(initiativeCountersService.findById("INITIATIVE_ID","USER123"))
                .thenReturn(Optional.of(pre));
        when(onboardingRepository.findById("USER123_INITIATIVE_ID"))
                .thenReturn(Optional.of(onboarding));

        OnboardingDTO mapped = new OnboardingDTO();
        when(consentMapper.map(onboarding)).thenReturn(mapped);

        rankerService.recovery(input);

        assertEquals("SRV001", mapped.getServiceId());
        assertNotNull(mapped);

        verify(rankerProducer).sendSaveConsent(mapped);
    }

    @Test
    void testRecovery_whenStatusUnsubscribed_shouldResendConsent() {
        OnboardingDTO input = new OnboardingDTO();
        input.setInitiativeId("INITIATIVE_ID");
        input.setUserId("USER777");
        input.setServiceId("SRV777");

        InitiativeCountersPreallocations pre = new InitiativeCountersPreallocations();
        pre.setPreallocatedAmountCents(15000L);

        Onboarding onboarding = new Onboarding("INITIATIVE_ID", "USER777");
        onboarding.setStatus(UNSUBSCRIBED);

        when(initiativeCountersService.findById("INITIATIVE_ID", "USER777"))
                .thenReturn(Optional.of(pre));
        when(onboardingRepository.findById("USER777_INITIATIVE_ID"))
                .thenReturn(Optional.of(onboarding));

        OnboardingDTO mapped = new OnboardingDTO();
        when(consentMapper.map(onboarding)).thenReturn(mapped);

        rankerService.recovery(input);

        assertEquals("SRV777", mapped.getServiceId());
        verify(rankerProducer).sendSaveConsent(mapped);
    }

    @Test
    void testRecovery_whenInvalidData_shouldLogError() {
        OnboardingDTO input = new OnboardingDTO();
        input.setInitiativeId("INITIATIVE_ID");
        input.setUserId("USR999");

        when(initiativeCountersService.findById("INITIATIVE_ID","USR999"))
                .thenReturn(Optional.empty());
        when(onboardingRepository.findById(any())).thenReturn(Optional.empty());

        rankerService.recovery(input);

        verify(rankerProducer, never()).sendSaveConsent(any());
    }

    @Test
    void testRecovery_whenStatusNotOnEvaluation_shouldGoToErrorBranch() {
        OnboardingDTO input = new OnboardingDTO();
        input.setInitiativeId("INITIATIVE_ID");
        input.setUserId("USER123");

        InitiativeCountersPreallocations pre = new InitiativeCountersPreallocations();
        pre.setPreallocatedAmountCents(5000L);

        Onboarding onboarding = new Onboarding("INITIATIVE_ID", "USER123");
        onboarding.setStatus("OTHER_STATUS");

        when(initiativeCountersService.findById("INITIATIVE_ID", "USER123"))
                .thenReturn(Optional.of(pre));
        when(onboardingRepository.findById("USER123_INITIATIVE_ID"))
                .thenReturn(Optional.of(onboarding));

        rankerService.recovery(input);

        verify(rankerProducer, never()).sendSaveConsent(any());
    }

    @Test
    void testRecovery_whenOnboardingMissing_shouldUseNAStatusInError() {
        OnboardingDTO input = new OnboardingDTO();
        input.setInitiativeId("INITIATIVE_ID");
        input.setUserId("USR999");

        InitiativeCountersPreallocations pre = new InitiativeCountersPreallocations();
        pre.setPreallocatedAmountCents(5000L);

        when(initiativeCountersService.findById("INITIATIVE_ID","USR999"))
                .thenReturn(Optional.of(pre));
        when(onboardingRepository.findById(any()))
                .thenReturn(Optional.empty());

        rankerService.recovery(input);

        verify(rankerProducer, never()).sendSaveConsent(any());
    }

    @Test
    void testRecovery_whenPreallocationMissingAndOnboardingPresent_shouldGoToErrorBranch() {
        OnboardingDTO input = new OnboardingDTO();
        input.setInitiativeId("INITIATIVE_ID");
        input.setUserId("USR_PRE_MISSING");

        Onboarding onboarding = new Onboarding("INITIATIVE_ID", "USR_PRE_MISSING");
        onboarding.setStatus(ON_EVALUATION);

        when(initiativeCountersService.findById("INITIATIVE_ID", "USR_PRE_MISSING"))
                .thenReturn(Optional.empty());
        when(onboardingRepository.findById("USR_PRE_MISSING_INITIATIVE_ID"))
                .thenReturn(Optional.of(onboarding));

        rankerService.recovery(input);

        verify(rankerProducer, never()).sendSaveConsent(any());
    }
}