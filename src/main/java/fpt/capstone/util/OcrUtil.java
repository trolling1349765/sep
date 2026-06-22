package fpt.capstone.util;

import net.sourceforge.tess4j.Tesseract;

import fpt.capstone.config.OcrConfig;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.exceprion.InvalidArgsException;
import fpt.capstone.exceprion.enums.ErrorCode;
import net.sourceforge.tess4j.TesseractException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;

@Component
public final class OcrUtil {

    private final OcrConfig config;

    public OcrUtil(OcrConfig config) {
        this.config = config;
    }

    public String extractText(MultipartFile multipartFile) {
        try{
            BufferedImage image = ImageIO.read(multipartFile.getInputStream());
            return extractText(image);
        } catch (IOException e) {
            APIResponse resp = new APIResponse();
            resp.setCode(ErrorCode.OCR_TEXT_INVALID.getCode());
            resp.setMessage(ErrorCode.OCR_TEXT_INVALID.getMessage());
            resp.setData(multipartFile.getOriginalFilename());
            throw new InvalidArgsException(resp);
        }
    }

    public String extractText(BufferedImage image) {
        try {
            Tesseract tesseract = new Tesseract();
            tesseract.setDatapath(config.getTessDataPath());
            tesseract.setLanguage(config.getLanguage());
            return tesseract.doOCR(image);
        } catch (TesseractException e) {
            APIResponse resp = new APIResponse();
            resp.setCode(ErrorCode.OCR_TEXT_INVALID.getCode());
            resp.setMessage(ErrorCode.OCR_TEXT_INVALID.getMessage());
            throw new InvalidArgsException(resp);
        }
    }
}
