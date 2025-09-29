package it.gov.pagopa.ranker.connector.event.consumer;

import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.service.ranker.RankerServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class RankerConsumerTest {

    private RankerServiceImpl rankerServiceImpl;
    private RankerConsumer rankerConsumer;

    @BeforeEach
    void setUp() {
        rankerServiceImpl = mock(RankerServiceImpl.class);
        rankerConsumer = new RankerConsumer();
    }

    @Test
    void testRankerProcessorCallsService() {
        OnboardingDTO dto = OnboardingDTO.builder().userId("u1").initiativeId("i1").build();
        var consumer = rankerConsumer.rankerProcessor(rankerServiceImpl);

        consumer.accept(dto);

        verify(rankerServiceImpl, times(1)).execute(dto);
    }
}
