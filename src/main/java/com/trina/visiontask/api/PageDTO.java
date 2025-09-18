package com.trina.visiontask.api;

import lombok.Data;

@Data
public class PageDTO<T> {
    private Integer pageNum;
    private Integer pageSize;
    private Long total;
    private T data;
}
