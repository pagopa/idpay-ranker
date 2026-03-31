package it.gov.pagopa.ranker.service.transactionInProgress;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.exception.UnmanagedStrategyException;
import it.gov.pagopa.ranker.strategy.TransactionInProgressProcessorStrategy;
import it.gov.pagopa.ranker.strategy.TransactionInProgressProcessorStrategyFactory;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.TimeZone;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionInProgressServiceImplTest {

    private static final String APP_NAME = "TEST_APP";

    @Mock
    private TransactionInProgressProcessorStrategyFactory transactionInProgressProcessorStrategyFactory;

    @Mock
    private TransactionInProgressProcessorStrategy transactionInProgressProcessorStrategy;

    @Mock
    private TransactionInProgressErrorNotifierService transactionInProgressErrorNotifierService;


    @Spy
    ObjectMapper objectMapper;

    TransactionInProgressServiceImpl transactionInProgressService;

    @BeforeEach
    public void init() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Mockito.reset(transactionInProgressProcessorStrategyFactory, transactionInProgressProcessorStrategy);
        objectMapper = updateMapper();

        transactionInProgressService = new TransactionInProgressServiceImpl(
                objectMapper, transactionInProgressErrorNotifierService,
                transactionInProgressProcessorStrategyFactory, validator, APP_NAME);
    }

    @Test
    void shouldProcessValidExpiredTrx() throws JsonProcessingException {

        TransactionInProgressDTO transactionInProgressDTO =
                TransactionInProgressDTO.builder()
                        .id("ID_1")
                        .trxDate(OffsetDateTime.now())
                        .status(SyncTrxStatus.EXPIRED)
                        .extendedAuthorization(true)
                        .build();

        Message<String> message = MessageBuilder.withPayload(objectMapper.writeValueAsString(transactionInProgressDTO)).build();
        when(transactionInProgressProcessorStrategyFactory.getStrategy(SyncTrxStatus.EXPIRED))
                .thenReturn(transactionInProgressProcessorStrategy);
        Assertions.assertDoesNotThrow(() ->
                transactionInProgressService.process(message));
        verify(transactionInProgressProcessorStrategyFactory).getStrategy(SyncTrxStatus.EXPIRED);
        verify(transactionInProgressProcessorStrategy).processTransaction(any());
        verifyNoInteractions(transactionInProgressErrorNotifierService);
    }

    @Test
    void shouldNotProcessInvalidTrx_MissingId() throws JsonProcessingException {
        TransactionInProgressDTO transactionInProgressDTO =
                TransactionInProgressDTO.builder()
                        .trxDate(OffsetDateTime.now())
                        .status(SyncTrxStatus.EXPIRED)
                        .extendedAuthorization(true)
                        .build();
        Message<String> message = MessageBuilder.withPayload(objectMapper.writeValueAsString(transactionInProgressDTO)).build();
        Assertions.assertDoesNotThrow(() ->
                transactionInProgressService.process(message));
        verifyNoInteractions(transactionInProgressProcessorStrategyFactory);
        verifyNoInteractions(transactionInProgressProcessorStrategy);
        verify(transactionInProgressErrorNotifierService).notifyExpiredTransaction(any(),any(),eq(false),any());
    }

    @Test
    void shouldNotProcessInvalidTrx_MissingStatus() throws JsonProcessingException {

        TransactionInProgressDTO transactionInProgressDTO =
                TransactionInProgressDTO.builder()
                        .id("ID_1")
                        .trxDate(OffsetDateTime.now())
                        .extendedAuthorization(true)
                        .build();
        Message<String> message = MessageBuilder.withPayload(objectMapper.writeValueAsString(transactionInProgressDTO)).build();
        Assertions.assertDoesNotThrow(() ->
                transactionInProgressService.process(message));
        verifyNoInteractions(transactionInProgressProcessorStrategyFactory);
        verifyNoInteractions(transactionInProgressProcessorStrategy);
        verify(transactionInProgressErrorNotifierService).notifyExpiredTransaction(any(),any(),eq(false),any());
    }

    @Test
    void shouldSendErrorOnProcessingKO() throws JsonProcessingException {
        TransactionInProgressDTO transactionInProgressDTO =
                TransactionInProgressDTO.builder()
                        .id("ID_1")
                        .trxDate(OffsetDateTime.now())
                        .status(SyncTrxStatus.EXPIRED)
                        .extendedAuthorization(true)
                        .build();
        Message<String> message = MessageBuilder.withPayload(objectMapper.writeValueAsString(transactionInProgressDTO)).build();
        when(transactionInProgressProcessorStrategyFactory.getStrategy(SyncTrxStatus.EXPIRED))
                .thenReturn(transactionInProgressProcessorStrategy);
        doThrow(new RuntimeException("error")).doNothing().when(transactionInProgressProcessorStrategy)
                .processTransaction(any());
        Assertions.assertDoesNotThrow(() ->
                transactionInProgressService.process(message));
        verify(transactionInProgressProcessorStrategyFactory).getStrategy(SyncTrxStatus.EXPIRED);
        verify(transactionInProgressProcessorStrategy).processTransaction(any());
        verify(transactionInProgressErrorNotifierService).notifyExpiredTransaction(any(),any(),eq(true),any());
    }

    @Test
    void shouldNotProcessInvalidTrx_ThrowJsonProcessing() {
        Message<String> message = MessageBuilder.withPayload("UNEXPECTED_JSON").build();
        Assertions.assertDoesNotThrow(() ->
                transactionInProgressService.process(message));
        verifyNoInteractions(transactionInProgressProcessorStrategyFactory);
        verifyNoInteractions(transactionInProgressProcessorStrategy);
        verifyNoInteractions(transactionInProgressErrorNotifierService);
    }

    @Test
    void proccesTransaction_UnmanagedStrategyException() throws JsonProcessingException {
        TransactionInProgressDTO transactionInProgressDTO =
                TransactionInProgressDTO.builder()
                        .id("ID_1")
                        .trxDate(OffsetDateTime.now())
                        .status(SyncTrxStatus.EXPIRED)
                        .extendedAuthorization(true)
                        .build();
        Message<String> message = MessageBuilder.withPayload(objectMapper.writeValueAsString(transactionInProgressDTO)).build();
        when(transactionInProgressProcessorStrategyFactory.getStrategy(SyncTrxStatus.EXPIRED))
                .thenThrow(new UnmanagedStrategyException("DUMMY_EXCEPTION"));
        Assertions.assertDoesNotThrow(() ->
                transactionInProgressService.process(message));
        verify(transactionInProgressProcessorStrategyFactory).getStrategy(SyncTrxStatus.EXPIRED);
        verify(transactionInProgressProcessorStrategy, never()).processTransaction(any());
        verify(transactionInProgressErrorNotifierService, never()).notifyExpiredTransaction(any(),any(),eq(true),any());
    }

    @Test
    void notifyErrorException_cryptException() throws JsonProcessingException {
        TransactionInProgressDTO transactionInProgressDTO =
                TransactionInProgressDTO.builder()
                        .id("ID_1")
                        .trxDate(OffsetDateTime.now())
                        .status(SyncTrxStatus.EXPIRED)
                        .extendedAuthorization(true)
                        .build();
        Message<String> message = MessageBuilder.withPayload(objectMapper.writeValueAsString(transactionInProgressDTO)).build();
        when(transactionInProgressProcessorStrategyFactory.getStrategy(SyncTrxStatus.EXPIRED))
                .thenReturn(transactionInProgressProcessorStrategy);
        doThrow(new RuntimeException("error")).doNothing().when(transactionInProgressProcessorStrategy)
                .processTransaction(any());
        doThrow(new RuntimeException("DUMY_EXCEPTION")).when(transactionInProgressErrorNotifierService).notifyExpiredTransaction(eq(message),eq("error"), eq(true), any());
        Assertions.assertDoesNotThrow(() ->
                transactionInProgressService.process(message));
        verify(transactionInProgressProcessorStrategyFactory).getStrategy(SyncTrxStatus.EXPIRED);
        verify(transactionInProgressProcessorStrategy).processTransaction(any());
        verify(transactionInProgressErrorNotifierService).notifyExpiredTransaction(any(),any(),eq(true),any());
    }

    public ObjectMapper updateMapper() {
        return JsonMapper.builder()
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .disable(tools.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .changeDefaultVisibility(vc -> vc
                        .withVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
                        .withVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY)
                        .withVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY)
                        .withVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY)
                        .withVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
                        .withVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY)
                )
                .changeDefaultPropertyInclusion(incl ->
                        incl.withValueInclusion(JsonInclude.Include.NON_NULL)
                                .withContentInclusion(JsonInclude.Include.NON_NULL)
                )
                .defaultTimeZone(TimeZone.getDefault())
                .build();
    }

    @Test
    void getFlowName(){
        String result = transactionInProgressService.getFlowName();

        Assertions.assertEquals("PROCESS_TRANSACTION_EH", result);
    }

}
