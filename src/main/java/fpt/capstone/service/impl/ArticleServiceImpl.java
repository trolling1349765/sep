package fpt.capstone.service.impl;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.ArticleResponse;
import fpt.capstone.dto.response.PolicyDetailResponse;
import fpt.capstone.entity.Article;
import fpt.capstone.repository.ArticleRepository;
import fpt.capstone.service.ArticleService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ArticleServiceImpl implements ArticleService {

    ArticleRepository articleRepository;

    @Override
    public APIResponse<Page<ArticleResponse>> findAll(int chapterId, int size, int page) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Article> articles = articleRepository.findAll(pageable);
        List<ArticleResponse> list = articles.getContent().stream()
                .filter(article -> article.getChapter().getId() == chapterId)
                .map(ArticleResponse::new).collect(Collectors.toList());
        Page<ArticleResponse> articleResponsePage = new PageImpl<>(list, pageable, list.size());

        APIResponse<Page<ArticleResponse>> response = APIResponse.success(articleResponsePage);
        return response;
    }

    @Override
    public Page<ArticleResponse> getArticlesByChapterId(int chapterId, int size, int page) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Article> articles = articleRepository.findAllByDeleteFalseAndChapterIdEquals(chapterId, pageable);
        return articles.map(ArticleResponse::new);
    }
}
