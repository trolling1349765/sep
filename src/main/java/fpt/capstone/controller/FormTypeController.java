package fpt.capstone.controller;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.FormTypeResponse;
import fpt.capstone.service.FormTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/form-type")
public class FormTypeController {
    private final FormTypeService formTypeService;

    @GetMapping
    public APIResponse<List<FormTypeResponse>> getAllFormTypes() {
        return formTypeService.getAllFormTypes();
    }
}
