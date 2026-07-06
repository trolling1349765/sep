package fpt.capstone.service;

import fpt.capstone.dto.request.RelativeRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.RelativeResponse;

public interface RelativeService {
    APIResponse<RelativeResponse> createRelative(RelativeRequest relativeRequest);
}
