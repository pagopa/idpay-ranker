package it.gov.pagopa.ranker.connector.event.producer;

import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.cloud.stream.function.StreamBridge;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RankerProducerTest {

    private StreamBridge streamBridge;
    private RankerProducer rankerProducer;

    @BeforeEach
    void setUp() {
        streamBridge = mock(StreamBridge.class);
        rankerProducer = new RankerProducer("mockBinder", streamBridge);
    }

    @Test
    void testSendSaveConsent() {
        OnboardingDTO dto = OnboardingDTO.builder()
                .userId("user1")
                .initiativeId("initiative1")
                .build();

        rankerProducer.sendSaveConsent(dto);

        ArgumentCaptor<OnboardingDTO> captor = ArgumentCaptor.forClass(OnboardingDTO.class);
        verify(streamBridge, times(1)).send(eq("rankerProducer-out-0"), captor.capture());

        OnboardingDTO captured = captor.getValue();
        assertEquals("user1", captured.getUserId());
        assertEquals("initiative1", captured.getInitiativeId());
    }
}
