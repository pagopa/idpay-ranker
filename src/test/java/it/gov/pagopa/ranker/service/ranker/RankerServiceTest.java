package it.gov.pagopa.ranker.service.ranker;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.ranker.connector.event.producer.RankerProducer;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.domain.mapper.ConsentMapper;
import it.gov.pagopa.ranker.domain.model.InitiativeCountersPreallocations;
import it.gov.pagopa.ranker.domain.model.Onboarding;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.ranker.repository.OnboardingRepository;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.*;
import java.util.List;
import java.util.Optional;

import static it.gov.pagopa.ranker.constants.OnboardingConstant.ON_EVALUATION;
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
    private List<String> initiatives = List.of("INITIATIVE_ID");

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

    private ServiceBusReceivedMessage buildMessage(OnboardingDTO dto) throws Exception {
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

    @Test
    void testExecute_whenNewPreallocation_shouldAddAndSend() throws Exception {
        // Given
        OnboardingDTO dto = new OnboardingDTO();
        dto.setInitiativeId(initiatives.getFirst());
        dto.setUserId("USR001");
        dto.setVerifyIsee(true);

        ServiceBusReceivedMessage message = buildMessage(dto);
        when(initiativeCountersService.existsByInitiativeIdAndUserId(initiatives.getFirst(), "USR001")).thenReturn(false);

        // When
        rankerService.execute(message);

        // Then
        verify(initiativeCountersService).addPreallocatedUser(
                eq(initiatives.getFirst()),
                eq("USR001"),
                eq(true),
                eq(99L),
                any(LocalDateTime.class)
        );
        verify(rankerProducer).sendSaveConsent(any(OnboardingDTO.class));
    }

    @Test
    void testExecute_whenUserAlreadyPreallocated_shouldDoNothing() throws Exception {
        // Given
        OnboardingDTO dto = new OnboardingDTO();
        dto.setInitiativeId(initiatives.getFirst());
        dto.setUserId("USR_EXIST");
        dto.setVerifyIsee(false);

        ServiceBusReceivedMessage message = buildMessage(dto);
        when(initiativeCountersService.existsByInitiativeIdAndUserId(initiatives.getFirst(),  "USR_EXIST")).thenReturn(true);

        // When
        rankerService.execute(message);

        // Then
        verify(initiativeCountersService, never()).addPreallocatedUser(any(), any(), anyBoolean(), anyLong(), any());
        verify(rankerProducer, never()).sendSaveConsent(any());
    }

    @Test
    void testExecute_whenDeserializationFails_shouldThrowIllegalStateException() {
        // Given
        ServiceBusMessage badMessage = new ServiceBusMessage("{invalid-json}");
        ServiceBusReceivedMessage message = mock(ServiceBusReceivedMessage.class);
        when(message.getBody()).thenReturn(badMessage.getBody());

        // Then
        assertThrows(IllegalStateException.class, () -> rankerService.execute(message));

        verifyNoInteractions(rankerProducer, initiativeCountersRepository, initiativeCountersService);
    }

    @Test
    void testExecute_whenInternalServiceThrows_shouldWrapInMessageProcessingException() throws Exception {
        // Given
        OnboardingDTO dto = new OnboardingDTO();
        dto.setInitiativeId("INIT_FAIL");
        dto.setUserId("USR_FAIL");

        ServiceBusReceivedMessage message = buildMessage(dto);

        when(initiativeCountersService.existsByInitiativeIdAndUserId(any(), any()))
                .thenThrow(new DuplicateKeyException("MESSAGE"));

        // Then
        DuplicateKeyException ex = assertThrows(DuplicateKeyException.class, () -> rankerService.execute(message));
        assertTrue(ex.getMessage().contains("MESSAGE"));
    }

    @Test
    void testExecute_whenVerifyIseeNull_shouldTreatAsFalse() throws Exception {
        // Given
        OnboardingDTO dto = new OnboardingDTO();
        dto.setInitiativeId(initiatives.getFirst());
        dto.setUserId("USR002");
        dto.setVerifyIsee(null);

        ServiceBusReceivedMessage message = buildMessage(dto);
        when(initiativeCountersService.existsByInitiativeIdAndUserId(initiatives.getFirst(), "USR002")).thenReturn(false);

        // When
        rankerService.execute(message);

        // Then
        verify(initiativeCountersService).addPreallocatedUser(
                eq(initiatives.getFirst()),
                eq("USR002"),
                eq(false),
                eq(99L),
                any(LocalDateTime.class)
        );
    }

    @Test
    void testExecute_AnotherInitiative() throws Exception {
        // Given
        OnboardingDTO dto = new OnboardingDTO();
        dto.setInitiativeId("another-initiative");
        dto.setUserId("USR002");
        dto.setVerifyIsee(null);

        ServiceBusReceivedMessage message = buildMessage(dto);

        // When
        rankerService.execute(message);

        // Then
        verify(initiativeCountersService, never()).addPreallocatedUser(any(), any(), anyBoolean(), any(), any());
    }

    @Test
    void testRecovery_whenDataValid_shouldResendConsent() {
        // Given
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

        // When
        rankerService.recovery(input);

        // Then
        assertTrue(mapped.getVerifyIsee());
        assertEquals("SRV001", mapped.getServiceId());

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
        // Given
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

        // When
        rankerService.recovery(input);

        // Then
        verify(rankerProducer, never()).sendSaveConsent(any());
    }

    @Test
    void testRecovery_verifyIseeFalse_whenAmountLow() {
        // Given
        OnboardingDTO input = new OnboardingDTO();
        input.setInitiativeId("INITIATIVE_ID");
        input.setUserId("USER123");

        InitiativeCountersPreallocations pre = new InitiativeCountersPreallocations();
        pre.setPreallocatedAmountCents(8000L);

        Onboarding onboarding = new Onboarding("INITIATIVE_ID", "USER123");
        onboarding.setStatus(ON_EVALUATION);

        when(initiativeCountersService.findById("INITIATIVE_ID","USER123"))
                .thenReturn(Optional.of(pre));
        when(onboardingRepository.findById("USER123_INITIATIVE_ID"))
                .thenReturn(Optional.of(onboarding));

        OnboardingDTO mapped = new OnboardingDTO();
        when(consentMapper.map(onboarding)).thenReturn(mapped);

        // When
        rankerService.recovery(input);

        // Then
        assertFalse(mapped.getVerifyIsee());
        verify(rankerProducer).sendSaveConsent(mapped);
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
    void testSanitizeString() {
        assertNull(RankerServiceImpl.sanitizeString(null));
        assertEquals("abc", RankerServiceImpl.sanitizeString("a\nb\rc"));
        assertEquals("helloWorld", RankerServiceImpl.sanitizeString("hello@World!!!"));
    }
}