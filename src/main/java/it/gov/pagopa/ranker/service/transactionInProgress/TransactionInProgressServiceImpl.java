package it.gov.pagopa.ranker.service.transactionInProgress;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.ranker.domain.dto.TransactionInProgressDTO;
import it.gov.pagopa.ranker.exception.UnmanagedStrategyException;
import it.gov.pagopa.ranker.strategy.TransactionInProgressProcessorStrategyFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
public class TransactionInProgressServiceImpl implements TransactionInProgressService {

    private final ObjectMapper objectMapper;
    private final TransactionInProgressErrorNotifierService transactionInProgressErrorNotifierService;
    private final TransactionInProgressProcessorStrategyFactory transactionInProgressProcessorStrategyFactory;
    private final Validator validator;

    public TransactionInProgressServiceImpl(
            ObjectMapper objectMapper,
            TransactionInProgressErrorNotifierService transactionInProgressErrorNotifierService,
            TransactionInProgressProcessorStrategyFactory transactionInProgressProcessorStrategyFactory,
            Validator validator) {
        this.objectMapper = objectMapper;
        this.transactionInProgressErrorNotifierService = transactionInProgressErrorNotifierService;
        this.transactionInProgressProcessorStrategyFactory = transactionInProgressProcessorStrategyFactory;
        this.validator = validator;
    }

    @Override
    public void processTransactionInProgressEH(Message<String> message) {


        TransactionInProgressDTO transactionInProgressDTO = null;

        try {

            transactionInProgressDTO = objectMapper.readValue(message.getPayload(), TransactionInProgressDTO.class);
            log.debug("Received Transaction in Progress: trx {} with status {}",
                    transactionInProgressDTO.getId(),transactionInProgressDTO.getStatus());

            Set<ConstraintViolation<TransactionInProgressDTO>> constraintValidators =
                    validator.validate(transactionInProgressDTO);
            if (!constraintValidators.isEmpty()) {
                throw new ConstraintViolationException(constraintValidators);
            }

        } catch (JsonProcessingException e) {
            log.error("[PROCESS_TRX_EH] Unable to map message to TransactionInProgress");
            return;
        } catch (ConstraintViolationException constraintViolationException) {
            assert transactionInProgressDTO != null;
            log.error("[PROCESS_TRX_EH] Encountered violations for received transaction with id " +
                    transactionInProgressDTO.getId());
            notifyError(message, transactionInProgressDTO.getId(), false, constraintViolationException);
            return;
        }

        try {
            transactionInProgressProcessorStrategyFactory.getStrategy(transactionInProgressDTO.getStatus())
                    .processTransaction(transactionInProgressDTO);
        } catch (UnmanagedStrategyException unmanagedStrategyException) {
            log.debug("[PROCESS_TRX_EH] Unmanaged status {}", transactionInProgressDTO.getStatus());
        } catch (Exception e) {
            notifyError(message, transactionInProgressDTO.getId(),true, e);
        }

    }

    private void notifyError(Message<String> transactionInProgressDTO, String transactionId, Boolean retry, Exception e) {
        try {
            transactionInProgressErrorNotifierService.notifyExpiredTransaction(
                    transactionInProgressErrorNotifierService.buildMessage(
                            transactionInProgressDTO, transactionId),
                    e.getMessage(), retry, e
            );
            log.info("Failed Transaction Event Processing: {}", e.getMessage(), e);
        } catch (Exception cryptException) {
            log.error("Exception on Processing Transaction: Unable to save unparsable data to error", cryptException);
        }
    }

}
