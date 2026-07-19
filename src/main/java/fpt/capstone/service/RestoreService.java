package fpt.capstone.service;

import fpt.capstone.dto.request.RestoreRequest;
import fpt.capstone.dto.response.RestoreResultResponse;

public interface RestoreService {

    RestoreResultResponse restore(RestoreRequest request);
}
