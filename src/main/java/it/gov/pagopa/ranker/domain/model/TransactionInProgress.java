package it.gov.pagopa.ranker.domain.model;

import it.gov.pagopa.ranker.domain.dto.Reward;
import it.gov.pagopa.ranker.enums.OperationType;
import it.gov.pagopa.ranker.enums.SyncTrxStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@FieldNameConstants()
@Document(collection = "transaction_in_progress")
public class TransactionInProgress {

  @Id
  private String id;
  private String trxCode;
  private String idTrxAcquirer;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime trxDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime trxChargeDate;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime elaborationDateTime;

  private String operationType;
  private OperationType operationTypeTranscoded;
  private String idTrxIssuer;
  private String correlationId;
  private Long amountCents;
  private Long effectiveAmountCents;
  private String amountCurrency;
  private String mcc;
  private String acquirerId;
  private String merchantId;
  private String pointOfSaleId;
  private String merchantFiscalCode;
  private String vat;
  private String initiativeId;
  private String initiativeName;
  private String businessName;
  private Long rewardCents;
  private long counterVersion;
  @Builder.Default
  private List<String> rejectionReasons = new ArrayList<>();
  private String userId;
  private SyncTrxStatus status;
  private String channel;
  @Builder.Default
  private Map<String, Reward> rewards = new HashMap<>();
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime updateDate;

  private List<String> initiatives;
  @Builder.Default
  private Map<String, List<String>> initiativeRejectionReasons = new HashMap<>();
  private Map<String, String> additionalProperties = new HashMap<>();

  private Boolean extendedAuthorization;
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private OffsetDateTime trxEndDate;
  private OffsetDateTime initiativeEndDate;
  private Long voucherAmountCents;
}
