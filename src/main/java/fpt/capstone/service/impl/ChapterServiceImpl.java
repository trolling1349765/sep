package fpt.capstone.service.impl;

import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.ChapterResponse;
import fpt.capstone.entity.Chapter;
import fpt.capstone.repository.ChapterRepository;
import fpt.capstone.service.ChapterService;
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
public class ChapterServiceImpl implements ChapterService {

    ChapterRepository chapterRepository;

    @Override
    public Page<ChapterResponse> getChaptersByPolicyId(int policyId, int size, int page) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Chapter> chapters = chapterRepository.findAllByDeleteFalseAndPolicyIdEquals(policyId, pageable);

        return chapters.map(ChapterResponse::new);
    }
}
