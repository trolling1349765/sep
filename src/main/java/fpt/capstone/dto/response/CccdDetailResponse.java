package fpt.capstone.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CccdDetailResponse {
    private String idNumber;       // Số CCCD (12 số)
    private String fullName;       // Họ và tên
    private String dateOfBirth;    // Ngày sinh (dd/mm/yyyy)
    private String gender;         // Giới tính
    private String nationality;    // Quốc tịch
    private String placeOfOrigin;   // Quê quán
    private String placeOfResidence;// Nơi thường trú
    private String issueDate;      // Ngày cấp
    private String issuePlace;     // Nơi cấp
    private String issueSigner;    // Người cấp
}