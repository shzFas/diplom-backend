package kz.bilimedu.api.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Колонка email имеет тип citext, но JDBC-драйвер передаёт параметр как
     * varchar — при таком сочетании PostgreSQL сравнивает строки с учётом
     * регистра. Явное приведение возвращает регистронезависимое сравнение
     * и по-прежнему использует уникальный индекс по email.
     */
    @Query(value = "SELECT * FROM users WHERE email = CAST(:email AS citext)", nativeQuery = true)
    Optional<User> findByEmail(@Param("email") String email);
}
