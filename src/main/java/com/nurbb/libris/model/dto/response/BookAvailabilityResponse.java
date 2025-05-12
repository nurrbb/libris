package com.nurbb.libris.model.dto.response;

import lombok.*;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookAvailabilityResponse {

    private UUID bookId;
    private boolean isAvailable;
    private String title;
}
