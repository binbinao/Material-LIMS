package com.lims.common.result;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {

    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int size;
    private int number;

    public static <T> PageResult<T> of(List<T> content, long totalElements, int size, int number) {
        PageResult<T> result = new PageResult<>();
        result.setContent(content);
        result.setTotalElements(totalElements);
        result.setTotalElements(totalElements);
        result.setSize(size);
        result.setNumber(number);
        result.setTotalPages((int) Math.ceil((double) totalElements / size));
        return result;
    }
}
