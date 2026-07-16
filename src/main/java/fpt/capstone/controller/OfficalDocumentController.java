package fpt.capstone.controller;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.ArticleResponse;
import fpt.capstone.dto.response.ChapterResponse;
import fpt.capstone.dto.response.PolicyResponse;
import fpt.capstone.service.ArticleService;
import fpt.capstone.service.ChapterService;
import fpt.capstone.service.PolicyService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequestMapping("/policy")
public class OfficalDocumentController {

    PolicyService policyService;
    ChapterService chapterService;
    ArticleService articleService;

    @GetMapping()
    public APIResponse<Page<PolicyResponse>> getPolicies(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int page
    ) {
        return policyService.getPolicies(size, page);
    }

    @GetMapping("/{id}")
    public APIResponse<Page<ChapterResponse>> getChapters(
            @PathVariable int policyId,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int page
    ){
        return chapterService.getChaptersByPolicyId(policyId ,size, page);
    }

    @GetMapping("/chapter/{id}")
    public APIResponse<Page<ArticleResponse>> getArticle(
            @PathVariable int chapterId,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "0") int page
    ){
        return articleService.findAll(chapterId, size, page);
    }
}
