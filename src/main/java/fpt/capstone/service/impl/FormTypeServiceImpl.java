package fpt.capstone.service.impl;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.FormTypeResponse;
import fpt.capstone.entity.FormType;
import fpt.capstone.repository.FormTypeRepository;
import fpt.capstone.service.FormTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FormTypeServiceImpl implements FormTypeService {

    private final FormTypeRepository formTypeRepository;

    @Override
    public FormType getFormType(int id) {
        return formTypeRepository.findById(id).get();
    }

    @Override
    public APIResponse<List<FormTypeResponse>> getAllFormTypes() {
        List<FormType> formTypes = formTypeRepository.findAll();
        List<FormTypeResponse> formTypesResponse = formTypes.stream().map(
                formType -> new FormTypeResponse(formType)).collect(Collectors.toList()
        );
        APIResponse<List<FormTypeResponse>> response = APIResponse.success(formTypesResponse);
        return response;
    }
}
