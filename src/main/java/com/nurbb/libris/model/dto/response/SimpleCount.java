package com.nurbb.libris.model.dto.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimpleCount {

    private String name;
    private long count;
}
