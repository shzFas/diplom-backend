package kz.bilimedu.api.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * PATCH: null означает «не трогать это поле».
 *
 * <p>Роль здесь изменить нельзя намеренно. Учитель связан с
 * teaching_assignments и является автором выставленных оценок
 * (grades.graded_by с ON DELETE RESTRICT); превращение его в ученика оставило
 * бы назначения без учителя, а оценки — без объяснимого автора. Смена роли —
 * это деактивация одной учётной записи и заведение другой.
 */
public record UpdateUserRequest(

        @Size(max = 200) String fullName,

        @Email @Size(max = 320) String email,

        @Size(max = 2000) String avatarUrl) {
}
