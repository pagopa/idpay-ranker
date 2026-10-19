package it.gov.pagopa.ranker.service.initative;

import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.domain.dto.VerifyDTO;
import it.gov.pagopa.ranker.domain.model.InitiativeConfig;
import it.gov.pagopa.ranker.domain.model.InitiativeCounters;
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

import static it.gov.pagopa.ranker.connector.event.producer.RankerProducer.sanitizeField;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InitiativeCountersServiceImplTest {

    private static final List<String> INITIATIVE_ID = List.of("INITIATIVE_ID");

    @Mock
    private InitiativeCountersPreallocationsRepository initiativeCountersPreallocationsRepository;

    @Mock
    private InitiativeCountersRepository initiativeCountersRepositoryMock;

    @Mock
    private InitiativeBeneficiaryRuleService initiativeBeneficiaryRuleServiceMock;

    private InitiativeCountersServiceImpl initiativeCountersService;

    @BeforeEach
    void setUp() {
        initiativeCountersService = new InitiativeCountersServiceImpl(
                initiativeCountersRepositoryMock,
                INITIATIVE_ID,
                initiativeCountersPreallocationsRepository,
                initiativeBeneficiaryRuleServiceMock
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
        List<VerifyDTO> verifies = List.of(); // Inserire eventuali mock di VerifyDTO se necessario
        Long sequenceNumber = 1L;
        Long beneficiaryBudgetFixedCents = 20000L;

        InitiativeConfig config = InitiativeConfig.builder()
                .initiativeId(INITIATIVE_ID.getFirst())
                .beneficiaryBudgetFixedCents(beneficiaryBudgetFixedCents).build();

        when(initiativeBeneficiaryRuleServiceMock.getInitiativeConfig(INITIATIVE_ID.getFirst())).thenReturn(config);

        // Nota: Rimosso il mock di initiativeBeneficiaryRuleService se non più usato internamente da addPreallocatedUser

        initiativeCountersService.addPreallocatedUser(
                INITIATIVE_ID.getFirst(), userId, verifies, sequenceNumber, time);

        // Il valore atteso dipende da come calculateReservationCents elabora i parametri.
        // Assumendo che rispecchi il beneficiaryBudgetFixedCents passato:
        verify(initiativeCountersRepositoryMock)
                .incrementOnboardedAndBudget(INITIATIVE_ID.getFirst(), 20000L);

        ArgumentCaptor<InitiativeCountersPreallocations> captor =
                ArgumentCaptor.forClass(InitiativeCountersPreallocations.class);

        verify(initiativeCountersPreallocationsRepository).insert(captor.capture());

        assertEquals("USER123_INITIATIVE_ID", captor.getValue().getId());
        assertEquals(PreallocationStatus.PREALLOCATED, captor.getValue().getStatus());
        assertEquals(20000L, captor.getValue().getPreallocatedAmountCents());
        assertFalse(Boolean.TRUE.equals(captor.getValue().getConsentSent()));
    }

    @Test
    void testAddPreallocatedUser_budgetExhausted() {
        doThrow(new DuplicateKeyException("Duplicate"))
                .when(initiativeCountersRepositoryMock)
                .incrementOnboardedAndBudget(anyString(), anyLong());

        List<VerifyDTO> verifies = List.of();
        Long sequenceNumber = 10L;
        Long beneficiaryBudgetFixedCents = 10000L;

        InitiativeConfig config = InitiativeConfig.builder()
                .initiativeId(INITIATIVE_ID.getFirst())
                .beneficiaryBudgetFixedCents(beneficiaryBudgetFixedCents).build();

        when(initiativeBeneficiaryRuleServiceMock.getInitiativeConfig(INITIATIVE_ID.getFirst())).thenReturn(config);

        assertThrows(BudgetExhaustedException.class, () ->
                initiativeCountersService.addPreallocatedUser(
                        INITIATIVE_ID.getFirst(), "USER", verifies, sequenceNumber, LocalDateTime.now())
        );

        verify(initiativeCountersRepositoryMock)
                .incrementOnboardedAndBudget(INITIATIVE_ID.getFirst(), 10000L);
    }

    @Test
    void testReopenPreallocatedUser_successFromRefunded() {
        String userId = "USER123";
        String initiativeId = INITIATIVE_ID.getFirst();
        Long sequenceNumber = 42L;
        LocalDateTime enqueuedTime = LocalDateTime.now();

        InitiativeConfig config = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .beneficiaryBudgetFixedCents(20000L)
                .build();
        when(initiativeBeneficiaryRuleServiceMock.getInitiativeConfig(initiativeId)).thenReturn(config);

        InitiativeCountersPreallocations existing = InitiativeCountersPreallocations.builder()
                .id(userId + "_" + initiativeId)
                .initiativeId(initiativeId)
                .userId(userId)
                .status(PreallocationStatus.REFUNDED)
                .build();
        when(initiativeCountersPreallocationsRepository.findById(userId + "_" + initiativeId))
                .thenReturn(Optional.of(existing));

        boolean reopened = initiativeCountersService.reopenPreallocatedUser(
                initiativeId,
                userId,
                List.of(),
                sequenceNumber,
                enqueuedTime);

        assertTrue(reopened);
        verify(initiativeCountersRepositoryMock).reserveBudget(initiativeId, 20000L);
        verify(initiativeCountersRepositoryMock, never()).incrementOnboardedAndBudget(anyString(), anyLong());

        ArgumentCaptor<InitiativeCountersPreallocations> captor =
                ArgumentCaptor.forClass(InitiativeCountersPreallocations.class);
        verify(initiativeCountersPreallocationsRepository).save(captor.capture());

        InitiativeCountersPreallocations saved = captor.getValue();
        assertEquals(PreallocationStatus.PREALLOCATED, saved.getStatus());
        assertEquals(sequenceNumber, saved.getSequenceNumber());
        assertEquals(enqueuedTime, saved.getEnqueuedTime());
        assertEquals(20000L, saved.getPreallocatedAmountCents());
        assertFalse(Boolean.TRUE.equals(saved.getConsentSent()));
    }

    @Test
    void testReopenPreallocatedUser_successFromExpired() {
        String userId = "USER123";
        String initiativeId = INITIATIVE_ID.getFirst();

        InitiativeConfig config = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .beneficiaryBudgetFixedCents(20000L)
                .build();
        when(initiativeBeneficiaryRuleServiceMock.getInitiativeConfig(initiativeId)).thenReturn(config);

        InitiativeCountersPreallocations existing = InitiativeCountersPreallocations.builder()
                .id(userId + "_" + initiativeId)
                .initiativeId(initiativeId)
                .userId(userId)
                .status(PreallocationStatus.EXPIRED)
                .build();
        when(initiativeCountersPreallocationsRepository.findById(userId + "_" + initiativeId))
                .thenReturn(Optional.of(existing));

        boolean reopened = initiativeCountersService.reopenPreallocatedUser(
                initiativeId,
                userId,
                List.of(),
                1L,
                LocalDateTime.now());

        assertTrue(reopened);
        verify(initiativeCountersRepositoryMock).incrementOnboardedAndBudget(initiativeId, 20000L);
        verify(initiativeCountersRepositoryMock, never()).reserveBudget(anyString(), anyLong());
    }

    @Test
    void testReopenPreallocatedUser_successFromRejected() {
        String userId = "USER123";
        String initiativeId = INITIATIVE_ID.getFirst();

        InitiativeConfig config = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .beneficiaryBudgetFixedCents(20000L)
                .build();
        when(initiativeBeneficiaryRuleServiceMock.getInitiativeConfig(initiativeId)).thenReturn(config);

        InitiativeCountersPreallocations existing = InitiativeCountersPreallocations.builder()
                .id(userId + "_" + initiativeId)
                .initiativeId(initiativeId)
                .userId(userId)
                .status(PreallocationStatus.REJECTED)
                .build();
        when(initiativeCountersPreallocationsRepository.findById(userId + "_" + initiativeId))
                .thenReturn(Optional.of(existing));

        boolean reopened = initiativeCountersService.reopenPreallocatedUser(
                initiativeId,
                userId,
                List.of(),
                1L,
                LocalDateTime.now());

        assertTrue(reopened);
        verify(initiativeCountersRepositoryMock).incrementOnboardedAndBudget(initiativeId, 20000L);
        verify(initiativeCountersRepositoryMock, never()).reserveBudget(anyString(), anyLong());
    }

    @Test
    void testReopenPreallocatedUser_shouldReturnFalseWhenStatusNotTerminal() {
        String userId = "USER123";
        String initiativeId = INITIATIVE_ID.getFirst();

        InitiativeCountersPreallocations existing = InitiativeCountersPreallocations.builder()
                .id(userId + "_" + initiativeId)
                .initiativeId(initiativeId)
                .userId(userId)
                .status(PreallocationStatus.CAPTURED)
                .build();
        when(initiativeCountersPreallocationsRepository.findById(userId + "_" + initiativeId))
                .thenReturn(Optional.of(existing));

        boolean reopened = initiativeCountersService.reopenPreallocatedUser(
                initiativeId,
                userId,
                List.of(),
                1L,
                LocalDateTime.now());

        assertFalse(reopened);
        verify(initiativeCountersRepositoryMock, never()).incrementOnboardedAndBudget(anyString(), anyLong());
        verify(initiativeCountersPreallocationsRepository, never()).save(any());
    }

    @Test
    void testReopenPreallocatedUser_shouldReturnFalseWhenSameSequenceIsRedelivered() {
        String userId = "USER123";
        String initiativeId = INITIATIVE_ID.getFirst();

        InitiativeCountersPreallocations existing = InitiativeCountersPreallocations.builder()
                .id(userId + "_" + initiativeId)
                .initiativeId(initiativeId)
                .userId(userId)
                .status(PreallocationStatus.REFUNDED)
                .sequenceNumber(10L)
                .enqueuedTime(LocalDateTime.now().minusMinutes(2))
                .build();
        when(initiativeCountersPreallocationsRepository.findById(userId + "_" + initiativeId))
                .thenReturn(Optional.of(existing));

        boolean reopened = initiativeCountersService.reopenPreallocatedUser(
                initiativeId,
                userId,
                List.of(),
                10L,
                LocalDateTime.now());

        assertFalse(reopened);
        verify(initiativeCountersRepositoryMock, never()).reserveBudget(anyString(), anyLong());
        verify(initiativeCountersRepositoryMock, never()).incrementOnboardedAndBudget(anyString(), anyLong());
        verify(initiativeCountersPreallocationsRepository, never()).save(any());
    }

    @Test
    void testReopenPreallocatedUser_shouldReturnFalseWhenOlderEnqueuedTime() {
        String userId = "USER123";
        String initiativeId = INITIATIVE_ID.getFirst();
        LocalDateTime existingTime = LocalDateTime.now();

        InitiativeCountersPreallocations existing = InitiativeCountersPreallocations.builder()
                .id(userId + "_" + initiativeId)
                .initiativeId(initiativeId)
                .userId(userId)
                .status(PreallocationStatus.EXPIRED)
                .enqueuedTime(existingTime)
                .build();
        when(initiativeCountersPreallocationsRepository.findById(userId + "_" + initiativeId))
                .thenReturn(Optional.of(existing));

        boolean reopened = initiativeCountersService.reopenPreallocatedUser(
                initiativeId,
                userId,
                List.of(),
                null,
                existingTime.minusSeconds(1));

        assertFalse(reopened);
        verify(initiativeCountersRepositoryMock, never()).reserveBudget(anyString(), anyLong());
        verify(initiativeCountersRepositoryMock, never()).incrementOnboardedAndBudget(anyString(), anyLong());
        verify(initiativeCountersPreallocationsRepository, never()).save(any());
    }

    @Test
    void testMarkConsentSent_shouldSaveWhenSameAttemptAndPreallocated() {
        String userId = "USER123";
        String initiativeId = INITIATIVE_ID.getFirst();
        LocalDateTime enqueuedTime = LocalDateTime.now();
        Long sequenceNumber = 10L;

        InitiativeCountersPreallocations existing = InitiativeCountersPreallocations.builder()
                .id(userId + "_" + initiativeId)
                .initiativeId(initiativeId)
                .userId(userId)
                .status(PreallocationStatus.PREALLOCATED)
                .sequenceNumber(sequenceNumber)
                .enqueuedTime(enqueuedTime)
                .consentSent(false)
                .build();

        when(initiativeCountersPreallocationsRepository.findById(userId + "_" + initiativeId))
                .thenReturn(Optional.of(existing));

        boolean result = initiativeCountersService.markConsentSent(initiativeId, userId, sequenceNumber, enqueuedTime);

        assertTrue(result);
        ArgumentCaptor<InitiativeCountersPreallocations> captor = ArgumentCaptor.forClass(InitiativeCountersPreallocations.class);
        verify(initiativeCountersPreallocationsRepository).save(captor.capture());
        assertTrue(Boolean.TRUE.equals(captor.getValue().getConsentSent()));
    }

    @Test
    void testMarkConsentSent_shouldReturnFalseWhenAttemptDoesNotMatch() {
        String userId = "USER123";
        String initiativeId = INITIATIVE_ID.getFirst();

        InitiativeCountersPreallocations existing = InitiativeCountersPreallocations.builder()
                .id(userId + "_" + initiativeId)
                .initiativeId(initiativeId)
                .userId(userId)
                .status(PreallocationStatus.PREALLOCATED)
                .sequenceNumber(8L)
                .enqueuedTime(LocalDateTime.now().minusMinutes(1))
                .consentSent(false)
                .build();

        when(initiativeCountersPreallocationsRepository.findById(userId + "_" + initiativeId))
                .thenReturn(Optional.of(existing));

        boolean result = initiativeCountersService.markConsentSent(initiativeId, userId, 9L, LocalDateTime.now());

        assertFalse(result);
        verify(initiativeCountersPreallocationsRepository, never()).save(any());
    }

    @Test
    void testCalculateReservationCents() {
        List<VerifyDTO> verifies = List.of();
        Long beneficiaryBudgetFixedCents = 10000L;
        String initiativeId = "INITIATIVE_ID";

        InitiativeConfig config = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .beneficiaryBudgetFixedCents(beneficiaryBudgetFixedCents).build();

        when(initiativeBeneficiaryRuleServiceMock.getInitiativeConfig(initiativeId)).thenReturn(config);

        // Modificare l'asserzione in base alla reale logica di calculateReservationCents(verifies, budget)
        long result = initiativeCountersService.calculateReservationCents(verifies, initiativeId);

        assertEquals(beneficiaryBudgetFixedCents, result);
    }

    @Test
    void testCalculateReservationCents_withBeneficiaryBudgetCentsMax() {
        VerifyDTO verifyMock = new VerifyDTO();
        verifyMock.setBeneficiaryBudgetCentsMax(50000L);
        List<VerifyDTO> verifies = List.of(verifyMock);

        String initiativeId = "INITIATIVE_ID";

        InitiativeConfig config = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .beneficiaryBudgetMaxCents(80000L).build();

        when(initiativeBeneficiaryRuleServiceMock.getInitiativeConfig(initiativeId)).thenReturn(config);

        long result = initiativeCountersService.calculateReservationCents(verifies, initiativeId);

        assertEquals(50000L, result);
    }

    @Test
    void testCalculateReservationCents_withMultipleVerifiesAndFirstNull() {
        VerifyDTO verifyFirst = new VerifyDTO();
        verifyFirst.setBeneficiaryBudgetCentsMax(null);

        VerifyDTO verifySecond = new VerifyDTO();
        verifySecond.setBeneficiaryBudgetCentsMax(30000L);

        List<VerifyDTO> verifies = List.of(verifyFirst, verifySecond);

        String initiativeId = "INITIATIVE_ID";

        InitiativeConfig config = InitiativeConfig.builder()
                .initiativeId(initiativeId)
                .beneficiaryBudgetMaxCents(50000L).build();

        when(initiativeBeneficiaryRuleServiceMock.getInitiativeConfig(initiativeId)).thenReturn(config);

        long result = initiativeCountersService.calculateReservationCents(verifies, initiativeId);

        assertEquals(30000L, result);
    }

    @Test
    void testCalculateReservationCents_withEmptyVerifiesShouldThrowException() {
        List<VerifyDTO> verifies = List.of();

        String initiativeId = "INITIATIVE_ID";

        when(initiativeBeneficiaryRuleServiceMock.getInitiativeConfig(initiativeId)).thenReturn(null);

        IllegalStateException errorResult = assertThrows(IllegalStateException.class, () -> initiativeCountersService.calculateReservationCents(verifies, initiativeId));

        assertEquals("Unable to calculate the reservation budget for initiative [%s] due to an invalid or incomplete configuration."
                .formatted(sanitizeField(initiativeId)), errorResult.getMessage());
    }

    @Test
    void testHasAvailableBudget_true() {
        InitiativeCounters counterMock = InitiativeCounters.builder()
                .id(INITIATIVE_ID.getFirst())
                .residualInitiativeBudgetCents(500L)
                .build();
        when(initiativeCountersRepositoryMock.findByIdIn(INITIATIVE_ID)).thenReturn(List.of(counterMock));

        InitiativeConfig config = InitiativeConfig.builder()
                .initiativeId(INITIATIVE_ID.getFirst())
                .beneficiaryBudgetFixedCents(100L)
                .beneficiaryBudgetMaxCents(500L)
                .build();
        when(initiativeBeneficiaryRuleServiceMock.getInitiativeConfig(INITIATIVE_ID.getFirst())).thenReturn(config);

        assertTrue(initiativeCountersService.hasAvailableBudget());
    }

    @Test
    void testHasAvailableBudget_trueWithoutBeneficiaryBudgetMax() {
        InitiativeCounters counterMock = InitiativeCounters.builder()
                .id(INITIATIVE_ID.getFirst())
                .residualInitiativeBudgetCents(100L)
                .build();
        when(initiativeCountersRepositoryMock.findByIdIn(INITIATIVE_ID)).thenReturn(List.of(counterMock));

        InitiativeConfig config = InitiativeConfig.builder()
                .initiativeId(INITIATIVE_ID.getFirst())
                .beneficiaryBudgetFixedCents(100L)
                .build();
        when(initiativeBeneficiaryRuleServiceMock.getInitiativeConfig(INITIATIVE_ID.getFirst())).thenReturn(config);

        assertTrue(initiativeCountersService.hasAvailableBudget());
    }

    @Test
    void testHasAvailableBudget_falseNotRetrieveInitiativeConfig() {
        InitiativeCounters counterMock = InitiativeCounters.builder()
                .id(INITIATIVE_ID.getFirst())
                .residualInitiativeBudgetCents(100L)
                .build();
        when(initiativeCountersRepositoryMock.findByIdIn(INITIATIVE_ID)).thenReturn(List.of(counterMock));

        when(initiativeBeneficiaryRuleServiceMock.getInitiativeConfig(INITIATIVE_ID.getFirst())).thenReturn(null);


        assertFalse(initiativeCountersService.hasAvailableBudget());
    }

    @Test
    void testHasAvailableBudget_false() {
        InitiativeCounters counterMock = InitiativeCounters.builder()
                .id(INITIATIVE_ID.getFirst())
                .residualInitiativeBudgetCents(400L)
                .build();
        when(initiativeCountersRepositoryMock.findByIdIn(INITIATIVE_ID)).thenReturn(List.of(counterMock));

        InitiativeConfig config = InitiativeConfig.builder()
                .initiativeId(INITIATIVE_ID.getFirst())
                .beneficiaryBudgetFixedCents(100L)
                .beneficiaryBudgetMaxCents(500L)
                .build();
        when(initiativeBeneficiaryRuleServiceMock.getInitiativeConfig(INITIATIVE_ID.getFirst())).thenReturn(config);


        assertFalse(initiativeCountersService.hasAvailableBudget());
    }

    @Test
    void testHasAvailableBudget_falseWithoutBeneficiaryBudgetMacNull() {
        InitiativeCounters counterMock = InitiativeCounters.builder()
                .id(INITIATIVE_ID.getFirst())
                .residualInitiativeBudgetCents(50L)
                .build();
        when(initiativeCountersRepositoryMock.findByIdIn(INITIATIVE_ID)).thenReturn(List.of(counterMock));

        InitiativeConfig config = InitiativeConfig.builder()
                .initiativeId(INITIATIVE_ID.getFirst())
                .beneficiaryBudgetFixedCents(100L)
                .build();
        when(initiativeBeneficiaryRuleServiceMock.getInitiativeConfig(INITIATIVE_ID.getFirst())).thenReturn(config);


        assertFalse(initiativeCountersService.hasAvailableBudget());
    }

    @Test
    void testRetrieveInitiativesAvailableBudget(){
        InitiativeCounters counterMock = InitiativeCounters.builder()
                .id(INITIATIVE_ID.getFirst())
                .residualInitiativeBudgetCents(500L)
                .build();
        when(initiativeCountersRepositoryMock.findByIdIn(INITIATIVE_ID)).thenReturn(List.of(counterMock));

        InitiativeConfig config = InitiativeConfig.builder()
                .initiativeId(INITIATIVE_ID.getFirst())
                .beneficiaryBudgetFixedCents(100L)
                .beneficiaryBudgetMaxCents(500L)
                .build();
        when(initiativeBeneficiaryRuleServiceMock.getInitiativeConfig(INITIATIVE_ID.getFirst())).thenReturn(config);

        List<String> result = initiativeCountersService.retrieveInitiativesAvailableBudget();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(INITIATIVE_ID, result);
    }

    @Test
    void testRetrieveInitiativesAvailableBudget_withoutBudget(){
        InitiativeCounters counterMock = InitiativeCounters.builder()
                .id(INITIATIVE_ID.getFirst())
                .residualInitiativeBudgetCents(90L)
                .build();
        when(initiativeCountersRepositoryMock.findByIdIn(INITIATIVE_ID)).thenReturn(List.of(counterMock));

        InitiativeConfig config = InitiativeConfig.builder()
                .initiativeId(INITIATIVE_ID.getFirst())
                .beneficiaryBudgetFixedCents(100L)
                .build();
        when(initiativeBeneficiaryRuleServiceMock.getInitiativeConfig(INITIATIVE_ID.getFirst())).thenReturn(config);

        List<String> result = initiativeCountersService.retrieveInitiativesAvailableBudget();

        assertNotNull(result);
        assertTrue(result.isEmpty());
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


}
