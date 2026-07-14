package fpt.capstone.dto.response;

import fpt.capstone.entity.BenefitHistory;
import fpt.capstone.entity.Benificiary;
import fpt.capstone.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@NoArgsConstructor()
@Getter
@Setter
public class BenifitHistoryResponse {
    Integer id;
    Integer benificiaryId;
    String deliverId;
    String transferMethod;
    String receiver;

    public BenifitHistoryResponse(BenefitHistory benefitHistory) {
        this.id = benefitHistory.getId();
        this.benificiaryId = benefitHistory.getBenificiary().getId();
        this.deliverId = benefitHistory.getDeliver().getId();
        this.transferMethod = benefitHistory.getTransferMethod();
        this.receiver = benefitHistory.getReceiver();
    }
}
