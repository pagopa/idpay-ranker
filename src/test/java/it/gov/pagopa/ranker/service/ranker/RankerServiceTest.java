package it.gov.pagopa.ranker.service.ranker;

import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.ranker.connector.event.producer.RankerProducer;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.ranker.service.initative.InitiativeCountersService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.*;

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


    private ObjectMapper objectMapper;

    private RankerService rankerService;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        rankerService = new RankerServiceImpl(
                rankerProducer,
                initiativeCountersRepository,
                initiativeCountersService,
                objectMapper
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
        dto.setInitiativeId("INIT123");
        dto.setUserId("USR001");
        dto.setVerifyIsee(true);

        ServiceBusReceivedMessage message = buildMessage(dto);
        when(initiativeCountersRepository.existsByInitiativeIdAndUserId("INIT123", "USR001")).thenReturn(false);

        // When
        rankerService.execute(message);

        // Then
        verify(initiativeCountersRepository).existsByInitiativeIdAndUserId("INIT123", "USR001");
        verify(initiativeCountersService).addPreallocatedUser(
                eq("INIT123"),
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
        dto.setInitiativeId("INIT123");
        dto.setUserId("USR_EXIST");
        dto.setVerifyIsee(false);

        ServiceBusReceivedMessage message = buildMessage(dto);
        when(initiativeCountersRepository.existsByInitiativeIdAndUserId("INIT123", "USR_EXIST")).thenReturn(true);

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

        when(initiativeCountersRepository.existsByInitiativeIdAndUserId(any(), any()))
                .thenThrow(new DuplicateKeyException("MESSAGE"));

        // Then
        DuplicateKeyException ex = assertThrows(DuplicateKeyException.class, () -> rankerService.execute(message));
        assertTrue(ex.getMessage().contains("MESSAGE"));
    }

    @Test
    void testExecute_whenVerifyIseeNull_shouldTreatAsFalse() throws Exception {
        // Given
        OnboardingDTO dto = new OnboardingDTO();
        dto.setInitiativeId("INIT456");
        dto.setUserId("USR002");
        dto.setVerifyIsee(null);

        ServiceBusReceivedMessage message = buildMessage(dto);
        when(initiativeCountersRepository.existsByInitiativeIdAndUserId("INIT456", "USR002")).thenReturn(false);

        // When
        rankerService.execute(message);

        // Then
        verify(initiativeCountersService).addPreallocatedUser(
                eq("INIT456"),
                eq("USR002"),
                eq(false),
                eq(99L),
                any(LocalDateTime.class)
        );
    }
}