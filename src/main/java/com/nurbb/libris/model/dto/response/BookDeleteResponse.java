package com.nurbb.libris.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookDeleteResponse {
    private String title;
    private int previousCount;
    private int newCount;
    private String message;
}
