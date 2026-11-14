package com.example.courseservice.dto;

import org.springframework.data.domain.Slice;
import java.util.List;

public record PageResponse<T>(List<T> content, int page, int size, boolean hasNext) {
    public static <T> PageResponse<T> from(Slice<T> slice) {
        return new PageResponse<>(slice.getContent(), slice.getNumber(), slice.getSize(), slice.hasNext());
    }
}
