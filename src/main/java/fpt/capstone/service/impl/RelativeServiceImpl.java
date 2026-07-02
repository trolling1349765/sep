package fpt.capstone.service.impl;

import fpt.capstone.repository.RelativeRepository;
import fpt.capstone.service.RelativeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RelativeServiceImpl implements RelativeService {
    private final RelativeRepository relativeRepository;
}
