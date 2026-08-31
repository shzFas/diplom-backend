package kz.bilimedu.api.common;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Формат страницы из контракта: {items, page, size, total}.
 * Пагинация обязательна на всех коллекциях — в версии 2023 года
 * GET /marksall отдавал все оценки школы одним массивом.
 */
public record PageResponse<T>(List<T> items, int page, int size, long total) {

    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(), page.getTotalElements());
    }
}
