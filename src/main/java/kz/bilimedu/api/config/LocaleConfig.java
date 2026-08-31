package kz.bilimedu.api.config;

import java.util.List;
import java.util.Locale;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;

/**
 * Язык ответа определяется заголовком Accept-Language: ru или kk.
 * Локали школы, а не браузера пользователя вообще — всё остальное
 * сводится к ru.
 */
@Configuration
public class LocaleConfig {

    static final Locale RU = Locale.forLanguageTag("ru");
    static final Locale KK = Locale.forLanguageTag("kk");

    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setSupportedLocales(List.of(RU, KK));
        resolver.setDefaultLocale(RU);
        return resolver;
    }
}
