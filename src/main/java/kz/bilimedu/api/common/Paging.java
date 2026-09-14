package kz.bilimedu.api.common;

import org.springframework.data.domain.PageRequest;

/**
 * Единая политика пагинации: она обязательна на всех коллекциях контракта.
 * В версии 2023 года GET /marksall отдавал все оценки школы одним массивом.
 */
public final class Paging {

    /** Верхняя граница страницы: запрос ?size=100000 не должен выгружать базу. */
    public static final int MAX_SIZE = 200;

    private Paging() {
    }

    public static PageRequest of(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_SIZE));
    }
}
