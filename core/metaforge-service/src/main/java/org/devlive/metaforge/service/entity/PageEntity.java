package org.devlive.metaforge.service.entity;

import com.fasterxml.jackson.annotation.JsonView;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Data;
import lombok.ToString;
import org.devlive.metaforge.common.view.EntityView;
import org.springframework.data.domain.Page;

import java.util.List;

@Data
@ToString
@SuppressFBWarnings(value = {"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class PageEntity<T>
{
    @JsonView(value = {EntityView.UserView.class, EntityView.AdminView.class})
    private int page;

    @JsonView(value = {EntityView.AdminView.class, EntityView.UserView.class})
    private int size;

    @JsonView(value = {EntityView.AdminView.class, EntityView.UserView.class})
    private long total;

    @JsonView(value = {EntityView.AdminView.class, EntityView.UserView.class})
    private long totalPage;

    @JsonView(value = {EntityView.AdminView.class, EntityView.UserView.class})
    private List<T> content;

    private PageEntity()
    {
    }

    public static <T> PageEntity<T> build(Page<T> page)
    {
        PageEntity<T> pageEntity = new PageEntity<>();
        pageEntity.setPage(page.getNumber() + 1);
        pageEntity.setSize(page.getSize());
        pageEntity.setTotal(page.getTotalElements());
        pageEntity.setTotalPage(page.getTotalPages());
        pageEntity.setContent(page.getContent());
        return pageEntity;
    }
}
