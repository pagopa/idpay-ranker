package it.gov.pagopa.ranker.service.initative;

import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
import it.gov.pagopa.ranker.exception.BudgetExhaustedException;
import it.gov.pagopa.ranker.repository.InitiativeCountersRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitiativeCountersServiceImplTest {
    private static final String INITIATIVE_ID = "INITIATIVE_ID";
    @Mock
    private InitiativeCountersRepository initiativeCountersRepositoryMock;
    private InitiativeCountersService initiativeCountersService;

    @BeforeEach
    void setUp() {
        initiativeCountersService = new InitiativeCountersServiceImpl(initiativeCountersRepositoryMock, INITIATIVE_ID);
    }

    @Test
    void testAddPreallocatedUser_success() {
        // Given
        String userId = "USER123";
        boolean verifyIsee = true;
        long sequenceNumber = 1L;
        LocalDateTime time = LocalDateTime.now();

        // When
        initiativeCountersService.addPreallocatedUser(INITIATIVE_ID, userId, verifyIsee, sequenceNumber, time);

        // Then
        verify(initiativeCountersRepositoryMock, times(1))
                .incrementOnboardedAndBudget(INITIATIVE_ID, userId, 20000L, sequenceNumber, time);
    }

    @Test
    void testAddPreallocatedUser_budgetExhausted_throwsException() {
        // Given
        String userId = "USER999";
        boolean verifyIsee = false;
        long sequenceNumber = 10L;
        LocalDateTime time = LocalDateTime.now();

        doThrow(new DuplicateKeyException("Duplicate key"))
                .when(initiativeCountersRepositoryMock)
                .incrementOnboardedAndBudget(anyString(), anyString(), anyLong(), anyLong(), any(LocalDateTime.class));

        // Then
        BudgetExhaustedException ex = assertThrows(BudgetExhaustedException.class, () ->
                initiativeCountersService.addPreallocatedUser(INITIATIVE_ID, userId, verifyIsee, sequenceNumber, time)
        );

        assertTrue(ex.getMessage().contains("Budget exhausted"));
        verify(initiativeCountersRepositoryMock, times(1))
                .incrementOnboardedAndBudget(INITIATIVE_ID, userId, 10000L, sequenceNumber, time);
    }

    @Test
    void testHasAvailableBudget_true() {
        // Given
        InitiativeCounters counters = new InitiativeCounters();
        counters.setResidualInitiativeBudgetCents(15000L);
        when(initiativeCountersRepositoryMock.findById(INITIATIVE_ID)).thenReturn(Optional.of(counters));

        // When
        boolean result = initiativeCountersService.hasAvailableBudget();

        // Then
        assertTrue(result);
    }

    @Test
    void testHasAvailableBudget_falseWhenNull() {
        // Given
        when(initiativeCountersRepositoryMock.findById(INITIATIVE_ID)).thenReturn(Optional.empty());

        // When
        boolean result = initiativeCountersService.hasAvailableBudget();

        // Then
        assertFalse(result);
    }

    @Test
    void testHasAvailableBudget_falseWhenBelowThreshold() {
        // Given
        InitiativeCounters counters = new InitiativeCounters();
        counters.setResidualInitiativeBudgetCents(9000L);
        when(initiativeCountersRepositoryMock.findById(INITIATIVE_ID)).thenReturn(Optional.of(counters));

        // When
        boolean result = initiativeCountersService.hasAvailableBudget();

        // Then
        assertFalse(result);
    }

}
