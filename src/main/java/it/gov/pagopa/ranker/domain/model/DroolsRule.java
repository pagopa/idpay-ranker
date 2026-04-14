package it.gov.pagopa.ranker.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "beneficiary_rule")
@FieldNameConstants()
public class DroolsRule {
    @Id
    private String id;
    private String name;
    private InitiativeConfig initiativeConfig;
    private LocalDateTime updateDate;
}
