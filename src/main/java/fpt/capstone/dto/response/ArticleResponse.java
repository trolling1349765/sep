package fpt.capstone.dto.response;

import fpt.capstone.entity.Article;
import fpt.capstone.entity.Chapter;
import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Getter
@Setter
@NoArgsConstructor
public class ArticleResponse {
    Integer id;
    Integer chapterId;
    Integer articleNo;
    String title;
    String content;

    public ArticleResponse(Article article) {
        this.id = article.getId();
        this.chapterId = article.getChapter().getId();
        this.articleNo = article.getArticleNo();
        this.title = article.getTitle();
        this.content = article.getContent();
    }
}
