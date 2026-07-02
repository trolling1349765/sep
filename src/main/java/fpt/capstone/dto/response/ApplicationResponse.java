package fpt.capstone.dto.response;

import fpt.capstone.entity.Application;
import fpt.capstone.entity.Policy;
import fpt.capstone.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.util.Date;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ApplicationResponse {

    String supportedUser;
    String approvedBy;
    LocalDate approvedDate;
    int policyId;
    LocalDate submitDate;
    String status;
    String formType;
    String address;
    String supportReason;
    Double requestedAmount;
    LocalDate createdAt;
    String createdBy;
    LocalDate updatedAt;
    String updatedBy;
    boolean isDeleted;

    public ApplicationResponse(Application application) {
        this.supportedUser = application.getSupportedUser().getId();
        this.approvedBy = application.getApprovedBy().getId();
        this.approvedDate = application.getApprovedDate();
        this.policyId = application.getPolicy().getId();
        this.submitDate = application.getSubmitDate();
        this.status = application.getStatus().get();
        this.formType = application.getFormType();
        this.address = application.getAddress();
        this.supportReason = application.getSupportReason();
        this.requestedAmount = application.getRequestedAmount();
        this.createdAt = application.getCreateAt();
        this.createdBy = application.getCreateBy();
        this.updatedAt = application.getUpdateAt();
        this.updatedBy = application.getUpdateBy();
        this.isDeleted = application.isDelete();
    }
}
