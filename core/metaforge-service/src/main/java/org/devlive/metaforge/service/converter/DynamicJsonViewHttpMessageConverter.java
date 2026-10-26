package org.devlive.metaforge.service.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@Slf4j
@RestControllerAdvice
@SuppressFBWarnings(value = {"EI_EXPOSE_REP2"})
public class DynamicJsonViewHttpMessageConverter
        implements ResponseBodyAdvice<Object>
{
    private final ObjectMapper objectMapper;

    public DynamicJsonViewHttpMessageConverter(ObjectMapper objectMapper)
    {
        this.objectMapper = objectMapper;
        log.info("DynamicJsonViewHttpMessageConverter initialized");
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType)
    {
        return MappingJackson2HttpMessageConverter.class.isAssignableFrom(converterType);
    }

    @Override
    public Object beforeBodyWrite(Object body,
            MethodParameter returnType,
            MediaType selectedContentType,
            Class<? extends HttpMessageConverter<?>> selectedConverterType,
            ServerHttpRequest request,
            ServerHttpResponse response)
    {
        try {
            if (request instanceof ServletServerHttpRequest) {
                ServletServerHttpRequest servletRequest = (ServletServerHttpRequest) request;
                Class<?> jsonViewClass = (Class<?>) servletRequest.getServletRequest().getAttribute("JSON_VIEW");

                log.debug("Processing response body with view: {}", jsonViewClass);

                if (jsonViewClass != null && body != null) {
                    // 使用指定的 JsonView 重新序列化对象
                    // Serialize the object using the specified JsonView
                    String json = objectMapper.writerWithView(jsonViewClass)
                            .writeValueAsString(body);
                    return objectMapper.readValue(json, body.getClass());
                }
            }
        }
        catch (Exception e) {
            log.error("Error processing response body", e);
        }

        return body;
    }
}
