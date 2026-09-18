package kz.bilimedu.api.attendance;

/**
 * Статусы посещаемости (правило D11). Тип attendance_status в схеме.
 *
 * <p>Булев markFalse версии 2023 года не отличал болезнь от прогула,
 * а школа отчитывается именно этим различием — поэтому оно в типе,
 * а не в комментарии.
 */
public enum AttendanceStatus {

    PRESENT,

    /** Отсутствовал по уважительной причине. */
    EXCUSED,

    /** Прогул. */
    UNEXCUSED,

    LATE
}
