package fpt.capstone.service.impl;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OcrServiceImplTest {

    @Test
    void shouldParseTypicalCccdText() {
        String rawOcrText = "Ho va ten: NGUYEN VAN A\n"
                + "So: 123456789012\n"
                + "Ngay sinh: 01/01/1990\n"
                + "Gioi tinh: Nam\n"
                + "Quoc tich: Viet Nam\n"
                + "Que quan: Ha Noi\n"
                + "Noi thuong tru: Ha Noi\n"
                + "Ngay cap: 10/10/2020\n"
                + "Noi cap: Ha Noi\n"
                + "Can bo cap: CANH SAT\n";

        Map<String, String> result = OcrServiceImpl.parseCccdText(rawOcrText);

        assertEquals("123456789012", result.get("idNumber"));
        assertEquals("NGUYEN VAN A", result.get("fullName"));
        assertEquals("01/01/1990", result.get("dateOfBirth"));
        assertEquals("Nam", result.get("gender"));
        assertEquals("Viet Nam", result.get("nationality"));
        assertEquals("Ha Noi", result.get("placeOfOrigin"));
        assertEquals("Ha Noi", result.get("placeOfResidence"));
        assertEquals("10/10/2020", result.get("issueDate"));
        assertEquals("Ha Noi", result.get("issuePlace"));
        assertEquals("CANH SAT", result.get("issueSigner"));
    }

    @Test
    void shouldFallbackToNextLineIfHeaderAndValueOnSeparateLines() {
        String rawOcrText = "Ho va ten\n"
                + "TRAN THI B\n"
                + "So: 987654321098\n"
                + "Ngay sinh: 05/05/1985\n";

        Map<String, String> result = OcrServiceImpl.parseCccdText(rawOcrText);

        assertEquals("TRAN THI B", result.get("fullName"));
        assertEquals("987654321098", result.get("idNumber"));
    }
}
