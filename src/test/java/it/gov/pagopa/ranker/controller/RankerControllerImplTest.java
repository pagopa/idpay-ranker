package it.gov.pagopa.ranker.controller;

import it.gov.pagopa.ranker.domain.dto.ManualDequeueResponseDTO;
import it.gov.pagopa.ranker.domain.dto.OnboardingDTO;
import it.gov.pagopa.ranker.exception.ManualDequeueSizeNotValidException;
import it.gov.pagopa.ranker.service.ranker.RankerService;
import it.gov.pagopa.ranker.service.ranker.manualdequeue.ManualDequeueService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankerControllerImplTest {

  @Mock
  private RankerService rankerService;

  @Mock
  private ManualDequeueService manualDequeueService;

  @InjectMocks
  private RankerControllerImpl controller;

  @Test
  void preallocate_ok() {
    OnboardingDTO dto = new OnboardingDTO();
    dto.setInitiativeId("INIT123");
    dto.setUserId("USER_ABC");

    controller.preallocate(dto);

    verify(rankerService).preallocate(dto);
    verifyNoMoreInteractions(rankerService);
  }

  @Test
  void recovery_ok() {
    OnboardingDTO dto = new OnboardingDTO();
    dto.setInitiativeId("INIT123");
    dto.setUserId("USER_ABC");

    controller.recovery(dto);

    verify(rankerService).recovery(dto);
    verifyNoMoreInteractions(rankerService);
  }

  @Test
  void manualDequeue_ok() {
    String initiativeId = "INIT123";
    int size = 10;

    ManualDequeueResponseDTO expectedResponse =
            mock(ManualDequeueResponseDTO.class);

    when(manualDequeueService.requestDequeue(initiativeId, size))
            .thenReturn(expectedResponse);

    ResponseEntity<ManualDequeueResponseDTO> response =
            controller.manualDequeue(initiativeId, size);

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    assertSame(expectedResponse, response.getBody());

    verify(manualDequeueService).requestDequeue(initiativeId, size);
    verifyNoMoreInteractions(manualDequeueService);
    verifyNoInteractions(rankerService);
  }

  @Test
  void manualDequeue_withNonPositiveSize_throwsException() {
    assertThrows(
            ManualDequeueSizeNotValidException.class,
            () -> controller.manualDequeue("INIT123", 0)
    );

    verifyNoInteractions(manualDequeueService, rankerService);
  }
}
