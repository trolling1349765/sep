package fpt.capstone.controller;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.BenifitHistoryResponse;
import fpt.capstone.service.BenifitHistoryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/benifit-history")
public class BenifitHistoryController {
    BenifitHistoryService benifitHistoryService;

    @GetMapping
    public APIResponse<Page<BenifitHistoryResponse>> getBenifitHistory(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int page) {
        return benifitHistoryService.getBenificiaries(size, page);
    }
}
