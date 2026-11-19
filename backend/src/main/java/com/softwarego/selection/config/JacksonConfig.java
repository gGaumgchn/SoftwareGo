package com.softwarego.selection.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;
import java.util.TimeZone;

/**
 * JSON 时间格式：{@code yyyy-MM-dd'T'HH:mm:ss}，无时区后缀，统一 Asia/Shanghai。
 *
 * <p>{@code spring.jackson.date-format} 只影响 {@code java.util.Date}，
 * {@link java.time.LocalDateTime} 需要单独注册序列化器。</p>
 */
@Configuration(proxyBeanMethods = false)
public class JacksonConfig {

    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd'T'HH:mm:ss";

    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            builder.timeZone(TimeZone.getTimeZone(ClockConfig.ZONE_ID));
            builder.simpleDateFormat(DATE_TIME_PATTERN);
            builder.serializers(new LocalDateTimeSerializer(DATE_TIME_FORMATTER));
            builder.deserializers(new LocalDateTimeDeserializer(DATE_TIME_FORMATTER));
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }
}
