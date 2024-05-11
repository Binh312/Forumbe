package com.web.dto.request;

import lombok.Getter;
import lombok.Setter;

import javax.validation.constraints.NotNull;
import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class BlogRequest {

    private Integer currentPage;

    private Integer size;

    private String keyword;

    private Long id;

    private String title;

    private String description;

    private String image;

    private String content;

//    private List<FileDto> linkFiles = new ArrayList<>();

    private List<Long> listCategoryId = new ArrayList<>();
}
