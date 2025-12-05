package it.gov.pagopa.ranker.service.initative;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.domain.model.InitiativeCountersPreallocations;
import it.gov.pagopa.ranker.enums.PreallocationStatus;
import it.gov.pagopa.ranker.exception.BudgetExhaustedException;
import it.gov.pagopa.ranker.repository.InitiativeCountersPreallocationsRepository;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import it.gov.pagopa.utils.InitiativeCountersUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitiativeCountersServiceImplTest {

    private static final List<String> INITIATIVE_ID = List.of("INITIATIVE_ID");

    @Mock
    private InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;

    @Mock
    private InitiativeCountersRepository initiativeCountersRepositoryMock;

    private InitiativeCountersServiceImpl initiativeCountersService;

    @BeforeEach
    void setUp() {
        initiativeCountersService = new InitiativeCountersServiceImpl(
                initiativeCountersRepositoryMock,
                INITIATIVE_ID,
                initiativeCountersPreallocationsRepository
        );
    }

    @Test
    void testExistsByInitiativeIdAndUserId() {
        when(initiativeCountersPreallocationsRepository.existsById("USER_1_INIT")).thenReturn(true);

        boolean result = initiativeCountersService.existsByInitiativeIdAndUserId("INIT", "USER_1");

        assertTrue(result);
        verify(initiativeCountersPreallocationsRepository).existsById("USER_1_INIT");
    }

    @Test
    void testFindById() {
        InitiativeCountersPreallocations pre = InitiativeCountersPreallocations.builder()
                .id("USER_1_INIT")
                .build();

        when(initiativeCountersPreallocationsRepository.findById("USER_1_INIT"))
                .thenReturn(Optional.of(pre));

        Optional<InitiativeCountersPreallocations> result =
                initiativeCountersService.findById("INIT", "USER_1");

        assertTrue(result.isPresent());
        assertEquals("USER_1_INIT", result.get().getId());
    }

    @Test
    void testAddPreallocatedUser_success() {
        String userId = "USER123";
        LocalDateTime time = LocalDateTime.now();

        initiativeCountersService.addPreallocatedUser(
                INITIATIVE_ID.getFirst(), userId, true, 1L, time);

        verify(initiativeCountersRepositoryMock)
                .incrementOnboardedAndBudget(INITIATIVE_ID.getFirst(), 20000L);

        ArgumentCaptor<InitiativeCountersPreallocations> captor =
                ArgumentCaptor.forClass(InitiativeCountersPreallocations.class);

        verify(initiativeCountersPreallocationsRepository).insert(captor.capture());

        assertEquals("USER123_INITIATIVE_ID", captor.getValue().getId());
        assertEquals(PreallocationStatus.PREALLOCATED, captor.getValue().getStatus());
        assertEquals(20000L, captor.getValue().getPreallocatedAmountCents());
    }

    @Test
    void testAddPreallocatedUser_budgetExhausted() {
        doThrow(new DuplicateKeyException("Duplicate"))
                .when(initiativeCountersRepositoryMock)
                .incrementOnboardedAndBudget(anyString(), anyLong());

        assertThrows(BudgetExhaustedException.class, () ->
                initiativeCountersService.addPreallocatedUser(
                        INITIATIVE_ID.getFirst(), "USER", false, 10L, LocalDateTime.now())
        );

        verify(initiativeCountersRepositoryMock)
                .incrementOnboardedAndBudget(INITIATIVE_ID.getFirst(), 10000L);
    }

    @Test
    void testHasAvailableBudget_true() {
        when(initiativeCountersRepositoryMock
                .existsByIdInAndResidualInitiativeBudgetCentsGreaterThanEqual(INITIATIVE_ID, 20000))
                .thenReturn(true);

        assertTrue(initiativeCountersService.hasAvailableBudget());
    }

    @Test
    void testHasAvailableBudget_false() {
        when(initiativeCountersRepositoryMock
                .existsByIdInAndResidualInitiativeBudgetCentsGreaterThanEqual(INITIATIVE_ID, 20000))
                .thenReturn(false);

        assertFalse(initiativeCountersService.hasAvailableBudget());
    }

    @Test
    void testUpdateInitiativeCounters_preallocationMissing() {
        TransactionInProgressDTO dto = new TransactionInProgressDTO();
        dto.setId("ID_1");
        dto.setInitiativeId("INIT_1");
        dto.setVoucherAmountCents(1000L);
        dto.setUserId("USER_1");

        String preallocationId = InitiativeCountersUtils.computePreallocationId(dto);

        when(initiativeCountersPreallocationsRepository.existsById(preallocationId)).thenReturn(false);

        initiativeCountersService.updateInitiativeCounters(dto, preallocationId, "ID_1");

        verify(initiativeCountersPreallocationsRepository, never()).deleteById(any());
        verifyNoInteractions(initiativeCountersRepositoryMock);
    }

    @Test
    void testUpdateInitiativeCounters_deleteError() {
        TransactionInProgressDTO dto = new TransactionInProgressDTO();
        dto.setId("ID_1");
        dto.setInitiativeId("INIT_1");
        dto.setVoucherAmountCents(1000L);
        dto.setUserId("USER_1");

        String preallocationId = InitiativeCountersUtils.computePreallocationId(dto);

        when(initiativeCountersPreallocationsRepository.existsById(preallocationId)).thenReturn(true);
        doThrow(new RuntimeException("err"))
                .when(initiativeCountersPreallocationsRepository).deleteById(preallocationId);

        assertThrows(Exception.class, () ->
                initiativeCountersService.updateInitiativeCounters(dto, preallocationId, "ID_1")
        );

        verify(initiativeCountersPreallocationsRepository).deleteById(preallocationId);
        verifyNoMoreInteractions(initiativeCountersRepositoryMock);
    }

    @Test
    void testUpdateInitiativeCounters_decrementError() {
        TransactionInProgressDTO dto = new TransactionInProgressDTO();
        dto.setId("ID_1");
        dto.setInitiativeId("INIT_1");
        dto.setVoucherAmountCents(1000L);
        dto.setUserId("USER_1");

        String preallocationId = InitiativeCountersUtils.computePreallocationId(dto);

        when(initiativeCountersPreallocationsRepository.existsById(preallocationId)).thenReturn(true);

        doNothing().when(initiativeCountersPreallocationsRepository).deleteById(preallocationId);
        doThrow(new RuntimeException("err"))
                .when(initiativeCountersRepositoryMock)
                .decrementOnboardedAndBudget("INIT_1", 1000L);

        assertThrows(Exception.class, () ->
                initiativeCountersService.updateInitiativeCounters(dto, preallocationId, "ID_1")
        );

        verify(initiativeCountersPreallocationsRepository).deleteById(preallocationId);
        verify(initiativeCountersRepositoryMock)
                .decrementOnboardedAndBudget("INIT_1", 1000L);
    }

    @Test
    void testCalculateReservationCents() {
        assertEquals(20000L, initiativeCountersService.calculateReservationCents(true));
        assertEquals(10000L, initiativeCountersService.calculateReservationCents(false));
    }

    @Test
    void testSanitizeString() {
        assertNull(InitiativeCountersServiceImpl.sanitizeString(null));
        assertEquals("hello", InitiativeCountersServiceImpl.sanitizeString("\nhe!!llo\r"));
    }
}
