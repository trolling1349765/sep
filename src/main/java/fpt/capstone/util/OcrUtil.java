package fpt.capstone.util;

import jakarta.annotation.PostConstruct;
import net.sourceforge.tess4j.Tesseract;

import fpt.capstone.config.OcrConfig;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.exceprion.InvalidArgsException;
import fpt.capstone.enums.ErrorCode;
import net.sourceforge.tess4j.TesseractException;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import nu.pattern.OpenCV;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public final class OcrUtil {

    private final OcrConfig config;
    private final Tesseract tesseract;

    @PostConstruct
    public void init() {
        OpenCV.loadLocally();
    }

    public OcrUtil(OcrConfig config) {
        this.config = config;
        tesseract = new Tesseract();

        tesseract.setDatapath(config.getTessDataPath());
        tesseract.setLanguage(config.getLanguage());

        tesseract.setPageSegMode(3);
        tesseract.setOcrEngineMode(1);
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
            try {
                ImageIO.write(image, "png", new File("processed.png"));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            return tesseract.doOCR(image);
        } catch (TesseractException e) {
            APIResponse resp = new APIResponse();
            resp.setCode(ErrorCode.OCR_TEXT_INVALID.getCode());
            resp.setMessage(ErrorCode.OCR_TEXT_INVALID.getMessage());
            throw new InvalidArgsException(resp);
        }
    }

    private BufferedImage preprocess(BufferedImage image) {
        Mat src = bufferedImageToMat(image);
        Mat gray = new Mat();
        Mat binary = new Mat();
        Mat resized = new Mat();

        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);// chuyen mau xam

        Imgproc.resize(
                gray,
                gray,
                new Size(gray.width() * 2, gray.height() * 2),
                0,
                0,
                Imgproc.INTER_CUBIC
        );

        Imgproc.GaussianBlur(// lam min theo kich thuoc xung quanh
                gray,
                gray,
                new Size(5,5),
                0
        );

        Imgproc.threshold(// chuyen den trang
                gray,
                binary,
                0,
                255,
                Imgproc.THRESH_BINARY + Imgproc.THRESH_OTSU
        );

        BufferedImage result = matToBufferedImage(binary);

        try {
            ImageIO.write(
                    result,
                    "png",
                    new File("processed.png")
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    private BufferedImage matToBufferedImage(Mat resized) {
        MatOfByte matOfByte = new MatOfByte();

        Imgcodecs.imencode(".png", resized, matOfByte);

        try{
            return ImageIO.read( new ByteArrayInputStream(matOfByte.toArray()));
        } catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    private Mat bufferedImageToMat(BufferedImage image) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        try{
           ImageIO.write(image, "png", byteArrayOutputStream);

        } catch (IOException e) {
            throw new RuntimeException("input invalid");
        }
        return Imgcodecs.imdecode(
                new MatOfByte(byteArrayOutputStream.toByteArray()),
                Imgcodecs.IMREAD_UNCHANGED
        );
    }

    public String extractCitizenId(String text) {

        Pattern pattern =
                Pattern.compile("\\b\\d{12}\\b");

        Matcher matcher =
                pattern.matcher(text);

        return matcher.find()
                ? matcher.group()
                : null;
    }

    public String extractBirthDate(String text) {

        Pattern pattern =
                Pattern.compile(
                        "\\b\\d{2}/\\d{2}/\\d{4}\\b"
                );

        Matcher matcher =
                pattern.matcher(text);

        return matcher.find()
                ? matcher.group()
                : null;
    }

    public String extractGender(String text) {

        String lower =
                text.toLowerCase();

        if(lower.contains("nam")) {
            return "Nam";
        }

        if(lower.contains("nữ")
                || lower.contains("nu")) {
            return "Nữ";
        }

        return null;
    }

    public String extractFullName(String text) {

        Pattern pattern =
                Pattern.compile(
                        "Họ và tên.*?:([A-ZÀ-Ỹ\\s]{2,40})"
                );

        Matcher matcher =
                pattern.matcher(text);

        while(matcher.find()) {

            String candidate =
                    matcher.group().trim();

            if(candidate.split("\\s+").length >= 2) {
                return candidate;
            }
        }

        return null;
    }

    public String extractAddress(String text) {

        Pattern pattern = Pattern.compile(
                "Nơi thường trú.*?:\\s*(.+?)(?=\\n\\n|$)",
                Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );

        Matcher matcher = pattern.matcher(text);

        return matcher.find()
                ? matcher.group(1).trim()
                : null;
    }
}
