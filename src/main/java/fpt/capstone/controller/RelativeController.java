package fpt.capstone.controller;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.RelativeResponse;
import fpt.capstone.service.RelativeService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
@RequestMapping("/relative")
public class RelativeController {
    RelativeService relativeService;

    @GetMapping("/{id}")
    public APIResponse<RelativeResponse> getRelativeByApplicationId(@PathVariable int applicationId) {
        RelativeResponse relativeResponse = relativeService.getRelativeByApplicationId(applicationId);
        return APIResponse.success(relativeResponse);
    }
}
