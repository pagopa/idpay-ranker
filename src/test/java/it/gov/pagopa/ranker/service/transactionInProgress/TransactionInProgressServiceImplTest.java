package it.gov.pagopa.ranker.service.transactionInProgress;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import it.gov.pagopa.common.config.json.PageModule;
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
        updateMapper(objectMapper);

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

    public ObjectMapper updateMapper(ObjectMapper mapper) {
        mapper.registerModule(new JavaTimeModule());
        mapper.registerModule(new Jdk8Module());
        mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.DEFAULT));
        mapper.registerModule(new PageModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY);
        mapper.setVisibility(PropertyAccessor.IS_GETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY);
        mapper.setVisibility(PropertyAccessor.SETTER, JsonAutoDetect.Visibility.PUBLIC_ONLY);
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        mapper.setVisibility(PropertyAccessor.CREATOR, JsonAutoDetect.Visibility.ANY);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.setTimeZone(TimeZone.getDefault());
        return mapper;
    }

    @Test
    void getFlowName(){
        String result = transactionInProgressService.getFlowName();

        Assertions.assertEquals("PROCESS_TRANSACTION_EH", result);
    }

}
