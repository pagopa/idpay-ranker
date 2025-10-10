package it.gov.pagopa.ranker.service.transactionInProgress;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import it.gov.pagopa.common.config.json.PageModule;
import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import it.gov.pagopa.ranker.strategy.TransactionInProgressProcessorStrategy;
import it.gov.pagopa.ranker.strategy.TransactionInProgressProcessorStrategyFactory;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.TimeZone;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class TransactionInProgressServiceImplTest {

    @Mock
    private TransactionInProgressProcessorStrategyFactory transactionInProgressProcessorStrategyFactory;

    @Mock
    private TransactionInProgressProcessorStrategy transactionInProgressProcessorStrategy;

    @Mock
    private TransactionInProgressErrorNotifierService transactionInProgressErrorNotifierService;

    @Spy
    ObjectMapper objectMapper;

    @Spy
    Validator validator;

    TransactionInProgressServiceImpl transactionInProgressService;

    @BeforeEach
    public void init() {
        Mockito.reset(transactionInProgressProcessorStrategyFactory, transactionInProgressProcessorStrategy);
        updateMapper(objectMapper);
        transactionInProgressService = new TransactionInProgressServiceImpl(
                objectMapper, transactionInProgressErrorNotifierService,
                transactionInProgressProcessorStrategyFactory, validator);
    }

    @Test
    public void shouldProcessValidExpiredTrx() {

        TransactionInProgressDTO transactionInProgressDTO =
                TransactionInProgressDTO.builder()
                        .id("ID_1")
                        .trxDate(OffsetDateTime.now())
                        .status(SyncTrxStatus.EXPIRED)
                        .extendedAuthorization(true)
                        .build();
        when(transactionInProgressProcessorStrategyFactory.getStrategy(eq(SyncTrxStatus.EXPIRED)))
                .thenReturn(transactionInProgressProcessorStrategy);
        Assertions.assertDoesNotThrow(() ->
                transactionInProgressService.processTransactionInProgressEH(
                        objectMapper.writeValueAsString(transactionInProgressDTO)));
        verify(validator).validate(any());
        verify(transactionInProgressProcessorStrategyFactory).getStrategy(eq(SyncTrxStatus.EXPIRED));
        verify(transactionInProgressProcessorStrategy).processTransaction(any());
        verifyNoInteractions(transactionInProgressErrorNotifierService);
    }

    @Test
    public void shouldNotProcessInvalidTrx_MissingId() {
        TransactionInProgressDTO transactionInProgressDTO =
                TransactionInProgressDTO.builder()
                        .trxDate(OffsetDateTime.now())
                        .status(SyncTrxStatus.EXPIRED)
                        .extendedAuthorization(true)
                        .build();
        when(transactionInProgressProcessorStrategyFactory.getStrategy(eq(SyncTrxStatus.EXPIRED)))
                .thenReturn(transactionInProgressProcessorStrategy);
        Assertions.assertDoesNotThrow(() ->
                transactionInProgressService.processTransactionInProgressEH(
                        objectMapper.writeValueAsString(transactionInProgressDTO)));
        verify(validator).validate(any());
        verifyNoInteractions(transactionInProgressProcessorStrategyFactory);
        verifyNoInteractions(transactionInProgressProcessorStrategy);
        verify(transactionInProgressErrorNotifierService).notifyExpiredTransaction(any(),any(),eq(false),any());
    }

    @Test
    public void shouldNotProcessInvalidTrx_MissingStatus() {

        TransactionInProgressDTO transactionInProgressDTO =
                TransactionInProgressDTO.builder()
                        .id("ID_1")
                        .trxDate(OffsetDateTime.now())
                        .extendedAuthorization(true)
                        .build();
        when(transactionInProgressProcessorStrategyFactory.getStrategy(eq(SyncTrxStatus.EXPIRED)))
                .thenReturn(transactionInProgressProcessorStrategy);
        Assertions.assertDoesNotThrow(() ->
                transactionInProgressService.processTransactionInProgressEH(
                        objectMapper.writeValueAsString(transactionInProgressDTO)));
        verify(validator).validate(any());
        verifyNoInteractions(transactionInProgressProcessorStrategyFactory);
        verifyNoInteractions(transactionInProgressProcessorStrategy);
        verify(transactionInProgressErrorNotifierService).notifyExpiredTransaction(any(),any(),eq(false),any());
    }

    @Test
    public void shouldSendErrorOnProcessingKO() {
        TransactionInProgressDTO transactionInProgressDTO =
                TransactionInProgressDTO.builder()
                        .id("ID_1")
                        .trxDate(OffsetDateTime.now())
                        .status(SyncTrxStatus.EXPIRED)
                        .extendedAuthorization(true)
                        .build();
        when(transactionInProgressProcessorStrategyFactory.getStrategy(eq(SyncTrxStatus.EXPIRED)))
                .thenReturn(transactionInProgressProcessorStrategy);
        doThrow(new RuntimeException("error")).doNothing().when(transactionInProgressProcessorStrategy)
                .processTransaction(any());
        Assertions.assertDoesNotThrow(() ->
                transactionInProgressService.processTransactionInProgressEH(
                        objectMapper.writeValueAsString(transactionInProgressDTO)));
        verify(validator).validate(any());
        verify(transactionInProgressProcessorStrategyFactory).getStrategy(eq(SyncTrxStatus.EXPIRED));
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

}
