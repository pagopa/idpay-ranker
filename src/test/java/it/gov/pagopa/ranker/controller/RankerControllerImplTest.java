package it.gov.pagopa.ranker.controller;

import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.service.ranker.RankerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankerControllerImplTest {

    @Mock
    private RankerService rankerService;

    @InjectMocks
    private RankerControllerImpl controller;

    @Test
    void preallocate_ok() {
        OnboardingDTO dto = new OnboardingDTO();
        dto.setInitiativeId("INIT123");
        dto.setUserId("USER_ABC");

        controller.preallocate(dto);

        verify(rankerService, times(1)).preallocate(dto);
        verifyNoMoreInteractions(rankerService);
    }
}
