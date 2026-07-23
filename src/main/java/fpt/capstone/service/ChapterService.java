package fpt.capstone.service;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.ChapterResponse;
import org.springframework.data.domain.Page;

public interface ChapterService {

    Page<ChapterResponse>  getChaptersByPolicyId(int policyId, int size, int page);
}
