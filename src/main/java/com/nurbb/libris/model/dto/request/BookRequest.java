package com.nurbb.libris.model.dto.request;

import com.nurbb.libris.model.entity.Genre;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BookRequest {

    @NotBlank
    private String title;

    @NotNull
    private Set<UUID> authorIds; //name?

    @NotBlank
    private String isbn;

    @NotNull
    private LocalDate publishedDate;

    @NotNull
    private Genre genre;

    @NotNull
    private Integer count;

}
