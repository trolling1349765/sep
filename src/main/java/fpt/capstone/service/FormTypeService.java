package fpt.capstone.service;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.FormTypeResponse;
import fpt.capstone.entity.FormType;

import java.util.List;

public interface FormTypeService {
    FormType getFormType(int id);

    APIResponse<List<FormTypeResponse>> getAllFormTypes();
}
