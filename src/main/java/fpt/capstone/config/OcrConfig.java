package fpt.capstone.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class OcrConfig {

    @Value("${ocr.tessdata.path}")
    private String tessDataPath;

    @Value("${ocr.language}")
    private String Language;
}
