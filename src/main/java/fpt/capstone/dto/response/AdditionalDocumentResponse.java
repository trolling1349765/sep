package fpt.capstone.dto.response;

import fpt.capstone.entity.AdditionalDocument;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdditionalDocumentResponse {
    Integer id;
    String type;
    String filePath;
    String description;
    Integer applicationId;

    public AdditionalDocumentResponse(AdditionalDocument additionalDocument) {
        this.id = additionalDocument.getId();
        this.type = additionalDocument.getType();
        this.filePath = additionalDocument.getFilePath();
        this.description = additionalDocument.getDescription();
        this.applicationId = additionalDocument.getApplication().getId();
    }
}
