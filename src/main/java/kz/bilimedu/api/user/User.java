package kz.bilimedu.api.user;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Одна сущность пользователя вместо разделённых User и Student.
 *
 * <p>В версии 2023 года это были две коллекции с почти одинаковыми полями,
 * из-за чего login, register, getMe и changePassword были написаны дважды.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    // citext — регистронезависимый тип PostgreSQL. Без columnDefinition
    // Hibernate ждал бы varchar и не прошёл бы валидацию схемы.
    @Column(nullable = false, columnDefinition = "citext")
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(nullable = false)
    private Role role;

    private String avatarUrl;

    /** Пишется только сервером после подтверждения владения чатом. */
    private Long telegramChatId;

    @Column(nullable = false, insertable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false, insertable = false, updatable = false)
    private Instant updatedAt;

    /** Мягкое удаление: уволенный учитель остаётся автором выставленных оценок. */
    private Instant deactivatedAt;

    protected User() {
    }

    public User(String fullName, String email, String passwordHash, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public boolean isActive() {
        return deactivatedAt == null;
    }

    public Long getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public Long getTelegramChatId() {
        return telegramChatId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getDeactivatedAt() {
        return deactivatedAt;
    }

    public void deactivate() {
        this.deactivatedAt = Instant.now();
    }
}
