package fpt.capstone.service;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.ArticleResponse;
import org.springframework.data.domain.Page;

public interface ArticleService {
    APIResponse<Page<ArticleResponse>> findAll(int chapterId, int size, int page);

}
