package fpt.capstone.controller;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.BenificiaryResponse;
import fpt.capstone.entity.Benificiary;
import fpt.capstone.service.BenificiaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/benificiary")
public class BenificiaryController {
    private final BenificiaryService benificiaryService;

//    @PostMapping
//    public APIResponse<BenificiaryResponse> createBenificiary(@RequestBody Benificiary benificiary) {
//        return benificiaryService.createBenificiary(benificiary);
//    }
//
//    @GetMapping
//    public APIResponse<Page<BenificiaryResponse>> getBenificiary(
//            @RequestParam(defaultValue = "20") int size,
//            @RequestParam(defaultValue = "0") int page
//    ) {
//        return benificiaryService.getBenificiary(size, page);
//    }
//
//    @GetMapping("/{id}")
//    public APIResponse<BenificiaryResponse> getBenificiary(@PathVariable int id) {
//        return benificiaryService.getBenificiary(id);
//    }
}
