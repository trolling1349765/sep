package fpt.capstone.controller;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.BenificiaryResponse;
import fpt.capstone.entity.Benificiary;
import fpt.capstone.service.BenificiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/benificiary")
public class BenificiaryController {
    private final BenificiaryService benificiaryService;

    @GetMapping
    public APIResponse<Page<BenificiaryResponse>> getBenificiary(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int page
    ) {
        return benificiaryService.getBenificiaries(size, page);
    }

    @GetMapping("/{id}")
    public APIResponse<BenificiaryResponse> getBenificiary(@PathVariable int id) {
        return benificiaryService.getBenificiary(id);
    }

    @GetMapping("/application/{id}")
    public APIResponse<List<BenificiaryResponse>> getBenificiariesByApplicationId(@PathVariable int applicationId) {
        return APIResponse.success(benificiaryService.getBenificiariesByApplicationId(applicationId));
    }
}
