package fpt.capstone.service.impl;

import fpt.capstone.enums.ErrorCode;
import fpt.capstone.enums.Table;
import fpt.capstone.exceprion.AppException;
import fpt.capstone.exceprion.InvalidArgsException;
import org.apache.poi.xwpf.usermodel.*;

import java.io.FileOutputStream;
import java.io.IOException;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.DecisionDocumentResponse;
import fpt.capstone.entity.Application;
import fpt.capstone.entity.DecisionDocument;
import fpt.capstone.entity.SystemLog;
import fpt.capstone.enums.Action;
import fpt.capstone.repository.DecisionDocumentRepository;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.DecisionDocumentService;
import fpt.capstone.service.SystemLogService;
import fpt.capstone.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DecisionDocumentServiceImpl implements DecisionDocumentService {

    private final DecisionDocumentRepository decisionDocumentRepository;
    private final SystemLogService systemLogService;
    private final SecurityUtil securityUtil;
    private final UserRepository userRepository;

    @Override
    public APIResponse<DecisionDocumentResponse> createDecisionDocument(Application application, String path) throws Throwable {
        String userId = securityUtil.getCurrentUserId();
        List<APIResponse> list = new ArrayList<>();

        DecisionDocument decisionDocument = DecisionDocument.builder()
                .application(application)
                .issuer(userRepository.getUserById(userId))
                .issueDate(LocalDate.now())
                .filePath(path)
                .build();
        decisionDocument = decisionDocumentRepository.save(decisionDocument);

            XWPFDocument document = new XWPFDocument();
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        try (
                FileOutputStream out = new FileOutputStream(
                        path + "Giay_quyet_dinh_" + timeStamp + ".docx"
                )
        ) {
            XWPFParagraph headerPara = document.createParagraph();
            headerPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun run1 = headerPara.createRun();
            run1.setText("CỘNG HÒA XÃ HỘI CHỦ NGHĨA VIỆT NAM");
            run1.setFontSize(20);
            run1.setBold(true);
            run1.setFontFamily("Times New Roman");

            XWPFParagraph subHeaderPara = document.createParagraph();
            subHeaderPara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun headerRun2 = subHeaderPara.createRun();
            headerRun2.setText("Độc lập - Tự do - Hạnh phúc");
            headerRun2.setBold(true);
            headerRun2.setFontSize(13);
            headerRun2.setFontFamily("Times New Roman");

            // Đường gạch chân dưới tiêu ngữ
            XWPFParagraph linePara = document.createParagraph();
            linePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun lineRun = linePara.createRun();
            lineRun.setText("_______________");

            // --- TÊN ĐƠN ---
            XWPFParagraph titlePara = document.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            titlePara.setSpacingBefore(400); // Khoảng cách phía trên
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText("TỜ KHAI ĐỀ NGHỊ TRỢ GIÚP XÃ HỘI");
            titleRun.setBold(true);
            titleRun.setFontSize(24);
            titleRun.setFontFamily("Times New Roman");

            // --- Note ---
            XWPFParagraph dearPara = document.createParagraph();
            dearPara.setSpacingBefore(200);
            XWPFRun run3 = dearPara.createRun();
            run3.setText("(Áp dụng đối với đối tượng quy định tại khoản " + application.getFormType().getName() + " Điều 5 Nghị định số ).");
            run3.setFontSize(13);
            run3.setFontFamily("Times New Roman");

            XWPFParagraph title2Para = document.createParagraph();
            title2Para.setAlignment(ParagraphAlignment.CENTER);
            title2Para.setSpacingBefore(200); // Khoảng cách phía trên
            XWPFRun title2Run = titlePara.createRun();
            title2Run.setText("THÔNG TIN CỦA ĐỐI TƯỢNG");
            title2Run.setBold(true);
            title2Run.setFontSize(24);
            title2Run.setFontFamily("Times New Roman");

            String[] content = {
                    "",
                    "",
                    "",
                    ""
            };

            for (String line : content) {
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.setFirstLineIndent(400);
                XWPFRun run = paragraph.createRun();
                run.setText(line);
                run.setFontFamily("Times New Roman");
                run.setFontSize(13);
            }

            XWPFParagraph signPara = document.createParagraph();
            signPara.setAlignment(ParagraphAlignment.RIGHT);
            signPara.setSpacingBefore(500);
            XWPFRun signRun = signPara.createRun();
            signRun.setText("Ngày " + LocalDate.now().getDayOfMonth() + " tháng " + LocalDate.now().getMonth() + " năm " + LocalDate.now().getYear() + "\n");
            signRun.setFontFamily("Times New Roman");
            signRun.setFontSize(13);

            XWPFRun signerRun = signPara.createRun();
            signerRun.setText("NGƯỜI KHAI    \n\n\n");
            signerRun.setBold(true);
            signerRun.setFontSize(13);

            document.write(out);
        }catch (IOException e){

            list.add(APIResponse.error(ErrorCode.FILE_IO_ERROR.getCode(), ErrorCode.FILE_IO_ERROR.getMessage()));

        }

        if (!list.isEmpty()) {
            throw new AppException(list);
        }

        SystemLog systemLog = SystemLog.builder()
                .createdAt(LocalDateTime.now())
                .action(Action.DECISION_CREATE.getAction())
                .userId(userId)
                .newValue(decisionDocument)
                .entityType(Table.DECISION_DOCUMENT.getTableName())
                .entityId(decisionDocument.getId() + "")
                .build();
        systemLogService.write(systemLog);

        DecisionDocumentResponse documentResponse = new DecisionDocumentResponse(decisionDocument);
        return APIResponse.<DecisionDocumentResponse>builder()
                .data(documentResponse)
                .build();
    }

    @Override
    public APIResponse<DecisionDocumentResponse> getDecisionDocument(int id) {
        DecisionDocument decisionDocument = decisionDocumentRepository.findById(id).orElse(null);
        if (decisionDocument == null)
        throw new InvalidArgsException(APIResponse.error(
                        ErrorCode.ARGUMENT_INVALID.getCode(),
                        ErrorCode.ARGUMENT_INVALID.getMessage()
        ));
        DecisionDocumentResponse documentResponse = new DecisionDocumentResponse(decisionDocument);
        APIResponse<DecisionDocumentResponse> response = APIResponse.success(documentResponse);
        return response;
    }

    @Override
    public APIResponse<Page<DecisionDocumentResponse>> getAllDecisionDocuments(int size, int page) {
        Pageable pageable = PageRequest.of(page, size);
        Page<DecisionDocument> documents = decisionDocumentRepository.findAll(pageable);
        Page<DecisionDocumentResponse> documentResponse = documents.map(DecisionDocumentResponse::new);

        APIResponse<Page<DecisionDocumentResponse>> response = APIResponse.success(documentResponse);
        return response ;
    }
}
