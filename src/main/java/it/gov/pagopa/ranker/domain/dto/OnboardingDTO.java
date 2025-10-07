package it.gov.pagopa.ranker.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnboardingDTO {

    String userId;

    String initiativeId;

    String name;

    String surname;

    Boolean tc;

    Boolean verifyIsee;

    String status;

    Boolean pdndAccept;

    String userMail;

    String channel;

    LocalDateTime tcAcceptTimestamp;

    LocalDateTime criteriaConsensusTimestamp;

    //New attribute
    String serviceId;

    String rejectionReason;

    Long sequenceNumber;

    LocalDateTime enqueuedTime;
}
