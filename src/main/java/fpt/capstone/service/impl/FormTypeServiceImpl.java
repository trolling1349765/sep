package fpt.capstone.service.impl;

import fpt.capstone.entity.FormType;
import fpt.capstone.repository.FormTypeRepository;
import fpt.capstone.service.FormTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FormTypeServiceImpl implements FormTypeService {

    private final FormTypeRepository formTypeRepository;

    @Override
    public FormType getFormType(int id) {
        return formTypeRepository.findById(id).get();
    }
}
