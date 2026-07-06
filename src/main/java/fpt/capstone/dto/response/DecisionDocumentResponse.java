package fpt.capstone.dto.response;

import fpt.capstone.entity.Application;
import fpt.capstone.entity.DecisionDocument;
import fpt.capstone.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class DecisionDocumentResponse {

    int id;
    int applicationId;
    String issuer;
    LocalDate issueDate;
    String details;
    String filePath;

    public DecisionDocumentResponse(DecisionDocument decisionDocument) {
        this.id = decisionDocument.getId();
        this.applicationId = decisionDocument.getApplication().getId();
        this.issuer = decisionDocument.getIssuer().getId();
        this.issueDate = decisionDocument.getIssueDate();
        this.details = decisionDocument.getDetails();
        this.filePath = decisionDocument.getFilePath();
    }
}
