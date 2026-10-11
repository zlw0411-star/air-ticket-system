package com.airline.ticket.config;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Configuration
public class JacksonConfig {
    private static final DateTimeFormatter DATE_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter LOCAL_DATE =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final class LenientLocalDateTimeDeserializer extends LocalDateTimeDeserializer {
        private final DateTimeFormatter spaceFormatter;

        private LenientLocalDateTimeDeserializer(DateTimeFormatter spaceFormatter) {
            super(spaceFormatter);
            this.spaceFormatter = spaceFormatter;
        }

        @Override
        public LocalDateTime deserialize(com.fasterxml.jackson.core.JsonParser p,
                                         com.fasterxml.jackson.databind.DeserializationContext ctxt)
                throws java.io.IOException {
            String text = p.getValueAsString();
            if (text != null) {
                String trimmed = text.trim();
                if (!trimmed.isEmpty()) {
                    try {
                        return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                    } catch (java.time.format.DateTimeParseException ignored) {
                        return LocalDateTime.parse(trimmed, spaceFormatter);
                    }
                }
            }
            return super.deserialize(p, ctxt);
        }
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jacksonCustomizer() {
        return builder -> {
            SimpleModule module = new SimpleModule();
            module.addDeserializer(LocalDateTime.class, new LenientLocalDateTimeDeserializer(DATE_TIME));
            module.addSerializer(LocalDateTime.class, new LocalDateTimeSerializer(DATE_TIME));
            module.addDeserializer(LocalDate.class, new LocalDateDeserializer(LOCAL_DATE));
            module.addSerializer(LocalDate.class, new LocalDateSerializer(LOCAL_DATE));
            builder.modules(module);
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }

}
