package fpt.capstone.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnore;
import fpt.capstone.entity.*;
import jakarta.persistence.Column;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PolicyResponse {
    Integer id;
    String documentNo;
    String title;
    String documentType;
    LocalDate issuedDate;
    LocalDate effectiveDate;
    LocalDate expiredDate;
    String issuer;
    String summary;
    String fileURL;

    public PolicyResponse(Policy policy) {
        this.id = policy.getId();
        this.documentNo = policy.getDocumentNo();
        this.title = policy.getTitle();
        this.documentType = policy.getDocumentType();
        this.issuedDate = policy.getIssuedDate();
        this.effectiveDate = policy.getEffectiveDate();
        this.expiredDate = policy.getExpiredDate();
        this.issuer = policy.getIssuer();
        this.summary = policy.getSummary();
        this.fileURL = policy.getFileURL();
    }
}
