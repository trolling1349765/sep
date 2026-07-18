package fpt.capstone.dto.request;

import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.multipart.MultipartFile;

@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdditionalDocumentRequest {
    Integer id;
    String type;
    String filePath;
    String description;
    Integer applicationId;
    MultipartFile multipartFile;


}