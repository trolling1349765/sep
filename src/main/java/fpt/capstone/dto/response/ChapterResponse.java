package fpt.capstone.dto.response;

import fpt.capstone.entity.Chapter;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@NoArgsConstructor
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChapterResponse {
    Integer id;
    String tittle;
    int policyId;

    public ChapterResponse(Chapter chapter) {
        this.id = chapter.getId();
        this.tittle = chapter.getTitle();
        this.policyId = chapter.getPolicy().getId();
    }
}
