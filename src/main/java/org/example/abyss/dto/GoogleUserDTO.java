package org.example.abyss.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data; // <--- This generates getEmail(), getName(), etc.
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GoogleUserDTO {
    private String sub;
    private String name;
    private String email;
    private String picture;
}