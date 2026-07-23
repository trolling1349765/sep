package fpt.capstone.controller;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.ArticleResponse;
import fpt.capstone.dto.response.ChapterResponse;
import fpt.capstone.dto.response.PolicyResponse;
import fpt.capstone.enums.ErrorCode;
import fpt.capstone.exceprion.InvalidArgsException;
import fpt.capstone.service.ArticleService;
import fpt.capstone.service.ChapterService;
import fpt.capstone.service.PolicyService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/policy")
public class PolicyController {

    PolicyService policyService;
    ChapterService chapterService;
    ArticleService articleService;

    @GetMapping()
    public APIResponse<PagedModel<PolicyResponse>> getPolicies(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int page
    ) {
        if (page < 0 || size <= 0) {
            throw new InvalidArgsException(
                    APIResponse.error(
                            ErrorCode.INVALID_PAGE.getCode(),
                            ErrorCode.INVALID_PAGE.getMessage()
                    )
            );
        }
        Page<PolicyResponse> policyPage = policyService.getPolicies(size, page);
        if (page > policyPage.getTotalPages()) {
            throw new InvalidArgsException(
                    APIResponse.error(
                            ErrorCode.INVALID_PAGE.getCode(),
                            ErrorCode.INVALID_PAGE.getMessage()
                    )
            );
        }
        PagedModel<PolicyResponse> response = new PagedModel<>(policyPage);
        return APIResponse.success(response);
    }

    @GetMapping("/{policyId}")
    public APIResponse<PagedModel<ChapterResponse>> getChapters(
            @PathVariable int policyId,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int page
    ){
        if (page < 0 || size <= 0) {
            throw new InvalidArgsException(
                    APIResponse.error(
                            ErrorCode.INVALID_PAGE.getCode(),
                            ErrorCode.INVALID_PAGE.getMessage()
                    )
            );
        }
        Page<ChapterResponse> chapterPage = chapterService.getChaptersByPolicyId(policyId ,size, page);
        if (page > chapterPage.getTotalPages()) {
            throw new InvalidArgsException(
                    APIResponse.error(
                            ErrorCode.INVALID_PAGE.getCode(),
                            ErrorCode.INVALID_PAGE.getMessage()
                    )
            );
        }
        PagedModel<ChapterResponse> response = new PagedModel<>(chapterPage);
        return APIResponse.success(response);
    }

    @GetMapping("/chapter/{chapterId}")
    public APIResponse<Page<ArticleResponse>> getArticle(
            @PathVariable int chapterId,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int page
    ){
        if (page < 0 || size <= 0) {
            throw new InvalidArgsException(
                    APIResponse.error(
                            ErrorCode.INVALID_PAGE.getCode(),
                            ErrorCode.INVALID_PAGE.getMessage()
                    )
            );
        }
        Page<ArticleResponse> articleResponses = articleService.getArticlesByChapterId(chapterId, size, page);
        if (page > articleResponses.getTotalPages()) {
            throw new InvalidArgsException(
                    APIResponse.error(
                            ErrorCode.INVALID_PAGE.getCode(),
                            ErrorCode.INVALID_PAGE.getMessage()
                    )
            );
        }
        return APIResponse.success(articleResponses);
    }
}
