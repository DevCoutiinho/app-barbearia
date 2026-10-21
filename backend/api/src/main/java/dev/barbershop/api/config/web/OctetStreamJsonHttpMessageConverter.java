package dev.barbershop.api.config.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.lang.reflect.Type;

/**
 * Permite que o Spring desserialize partes de requisições multipart enviadas com
 * Content-Type 'application/octet-stream' (padrão em clientes como Swagger UI e Postman
 * quando o Content-Type individual da parte multipart/form-data não é especificado).
 */
public class OctetStreamJsonHttpMessageConverter extends AbstractJackson2HttpMessageConverter {

    public OctetStreamJsonHttpMessageConverter() {
        super(Jackson2ObjectMapperBuilder.json().build(), MediaType.APPLICATION_OCTET_STREAM);
    }

    public OctetStreamJsonHttpMessageConverter(ObjectMapper objectMapper) {
        super(objectMapper != null ? objectMapper : Jackson2ObjectMapperBuilder.json().build(), MediaType.APPLICATION_OCTET_STREAM);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        // Nunca utiliza este conversor para escrever respostas HTTP
        return false;
    }

    @Override
    public boolean canWrite(Type type, Class<?> clazz, MediaType mediaType) {
        // Nunca utiliza este conversor para escrever respostas HTTP
        return false;
    }
}
