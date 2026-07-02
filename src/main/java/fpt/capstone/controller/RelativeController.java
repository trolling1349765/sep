package fpt.capstone.controller;

import fpt.capstone.service.RelativeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RelativeController {
    private final RelativeService relativeService;
}
