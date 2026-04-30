package com.kurtuba.auth.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.Serializer;
import org.springframework.core.serializer.support.DeserializingConverter;
import org.springframework.core.serializer.support.SerializingConverter;
import org.springframework.security.jackson2.SecurityJackson2Modules;
import org.springframework.session.config.SessionRepositoryCustomizer;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.session.jdbc.config.annotation.web.http.EnableJdbcHttpSession;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.FlashMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedHashSet;
import java.util.concurrent.CopyOnWriteArrayList;

@Configuration
@EnableJdbcHttpSession
public class SessionConfig implements BeanClassLoaderAware {

    private ClassLoader classLoader;

    @Value("${spring.datasource.hikari.schema:}")
    String schema;

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    @Bean
    public TableNameCustomizer tableNameCustomizer() {
        return new TableNameCustomizer();
    }

    @Bean("springSessionConversionService")
    public GenericConversionService springSessionConversionService(ObjectMapper objectMapper) {
        ObjectMapper sessionObjectMapper = objectMapper.copy();
        sessionObjectMapper.registerModules(SecurityJackson2Modules.getModules(this.classLoader));
        SecurityJackson2Modules.enableDefaultTyping(sessionObjectMapper);
        sessionObjectMapper.addMixIn(CopyOnWriteArrayList.class, AllowlistedSessionTypeMixin.class);
        sessionObjectMapper.addMixIn(FlashMap.class, AllowlistedSessionTypeMixin.class);
        sessionObjectMapper.addMixIn(LinkedHashSet.class, AllowlistedSessionTypeMixin.class);

        GenericConversionService conversionService = new GenericConversionService();
        conversionService.addConverter(Object.class, byte[].class,
                new SerializingConverter(new SessionJsonSerializer(sessionObjectMapper)));
        conversionService.addConverter(byte[].class, Object.class,
                new DeserializingConverter(new SessionJsonDeserializer(sessionObjectMapper)));
        return conversionService;
    }

    class TableNameCustomizer
            implements SessionRepositoryCustomizer<JdbcIndexedSessionRepository> {

        @Override
        public void customize(JdbcIndexedSessionRepository sessionRepository) {
            if(StringUtils.hasText(schema)){
                sessionRepository.setTableName(schema + ".SPRING_SESSION");
            }
        }

    }

    private static class SessionJsonSerializer implements Serializer<Object> {

        private final ObjectMapper objectMapper;

        private SessionJsonSerializer(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public void serialize(Object object, OutputStream outputStream) throws IOException {
            objectMapper.writeValue(outputStream, object);
        }
    }

    private static class SessionJsonDeserializer implements Deserializer<Object> {

        private final ObjectMapper objectMapper;

        private SessionJsonDeserializer(ObjectMapper objectMapper) {
            this.objectMapper = objectMapper;
        }

        @Override
        public Object deserialize(InputStream inputStream) throws IOException {
            return objectMapper.readValue(inputStream, Object.class);
        }
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY)
    private abstract static class AllowlistedSessionTypeMixin {
    }
}
