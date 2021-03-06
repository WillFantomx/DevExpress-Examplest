package org.itkk.udf.starter.core;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.autoconfigure.ConfigurationCustomizer;
import com.baomidou.mybatisplus.extension.MybatisMapWrapperFactory;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.BlockAttackInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.itkk.udf.starter.core.datasource.DruidBuild;
import org.itkk.udf.starter.core.exception.handle.RmsResponseErrorHandler;
import org.itkk.udf.starter.core.id.IdWorker;
import org.itkk.udf.starter.core.trace.TraceContentCachingFilter;
import org.itkk.udf.starter.core.xss.XssFilter;
import org.itkk.udf.starter.core.xss.XssStringJsonSerializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;
import java.util.concurrent.TimeUnit;

@EnableAsync
@EnableRetry
@EnableScheduling
@Configuration
@Slf4j
public class CoreConfiguration {

    /**
     * coreProperties
     */
    @Autowired
    private CoreProperties coreProperties;

    /**
     * mybatisPlus????????????
     */
    @Bean
    public List<ConfigurationCustomizer> configurationCustomizersProvider() {
        return Lists.newArrayList(
                configuration -> configuration.setObjectWrapperFactory(new MybatisMapWrapperFactory()),
                configuration -> configuration.setUseDeprecatedExecutor(false)
        );
    }

    /**
     * ??????Guava??????
     *
     * @return
     */
    @Bean
    @Primary
    public Cache<String, Object> defGuavaCache() {
        //??????
        return CacheBuilder.newBuilder()
                //????????????????????????????????????????????????????????????????????????
                .concurrencyLevel(coreProperties.getGuavaCacheProperties().getConcurrencyLevel())
                //?????????????????????????????????
                .initialCapacity(coreProperties.getGuavaCacheProperties().getInitialCapacity())
                //?????????????????????????????????????????????????????????LRU??????????????????????????????????????????
                .maximumSize(coreProperties.getGuavaCacheProperties().getMaximumSize())
                //??????????????????n????????????
                .expireAfterWrite(coreProperties.getGuavaCacheProperties().getConcurrencyLevel(), TimeUnit.MINUTES)
                //?????????????????????
                //.removalListener(notification -> log.info("defGuavaCache????????????,key : {} , cause : {} ", notification.getKey(), notification.getCause()))
                //??????
                .build();
    }

    /**
     * internalObjectMapper
     *
     * @param builder
     * @return
     */
    @Bean("internalObjectMapper")
    public ObjectMapper internalObjectMapper(Jackson2ObjectMapperBuilder builder) {
        return builder.createXmlMapper(false).build();
    }

    /**
     * xssObjectMapper
     *
     * @param builder builder
     * @return ObjectMapper
     */
    @Bean
    @Primary
    public ObjectMapper xssObjectMapper(Jackson2ObjectMapperBuilder builder) {
        //?????????
        ObjectMapper objectMapper = builder.createXmlMapper(false).build();
        // ??????xss?????????
        SimpleModule xssModule = new SimpleModule("XssStringJsonSerializer");
        xssModule.addSerializer(new XssStringJsonSerializer());
        objectMapper.registerModule(xssModule);
        // ??????
        return objectMapper;
    }

    /**
     * druidBuild
     *
     * @return DruidBuild
     */
    @Bean
    public DruidBuild druidBuild() {
        return new DruidBuild();
    }

    /**
     * idWorker
     *
     * @return IdWorker
     */
    @Bean
    public IdWorker idWorker() {
        return new IdWorker();
    }

    /**
     * ??????????????????,?????????????????????mybatis?????????,???????????? MybatisConfiguration#useDeprecatedExecutor = false ????????????????????????(?????????????????????????????????????????????)
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new BlockAttackInnerInterceptor());
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    /**
     * traceContentCachingFilter
     *
     * @return TraceContentCachingFilter
     */
    @Bean
    public TraceContentCachingFilter traceContentCachingFilter() {
        return new TraceContentCachingFilter();
    }

    /**
     * xssFilter
     *
     * @return XssFilter
     */
    @Bean
    public XssFilter xssFilter() {
        return new XssFilter();
    }

    /**
     * xssFilterRegistry
     *
     * @return FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean xssFilterRegistry() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(xssFilter());
        registration.addUrlPatterns(coreProperties.getXssFilterPathPattern());
        registration.setName("xssFilter");
        registration.setOrder(Integer.MIN_VALUE);
        return registration;
    }

    /**
     * traceContentCachingFilterRegistry
     *
     * @return FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean traceContentCachingFilterRegistry() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(traceContentCachingFilter());
        registration.addUrlPatterns(coreProperties.getTraceFilterPathPattern());
        registration.setName("traceContentCachingFilter");
        registration.setOrder(Integer.MIN_VALUE + 1);
        return registration;
    }

    /**
     * restTemplate
     *
     * @param requestFactory requestFactory
     * @return RestTemplate
     */
    @Bean
    @Primary
    public RestTemplate restTemplate(ClientHttpRequestFactory requestFactory) {
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        restTemplate.setErrorHandler(new RmsResponseErrorHandler());
        return restTemplate;
    }

    /**
     * requestFactory
     *
     * @return ClientHttpRequestFactory
     */
    @Bean
    public ClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(coreProperties.getHttpConnectTimeout());
        requestFactory.setReadTimeout(coreProperties.getHttpReadTimeout());
        requestFactory.setBufferRequestBody(coreProperties.getBufferRequestBody());
        return new BufferingClientHttpRequestFactory(requestFactory);
    }

    /**
     * ????????????
     *
     * @return WebMvcConfigurer
     */
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping(coreProperties.getCorsPathPattern())
                        .allowedOrigins(coreProperties.getCorsAllowedOrigins().split(","))
                        .allowCredentials(true)
                        .allowedMethods("GET", "POST", "DELETE", "PUT", "PATCH")
                        .allowedHeaders("*")
                        .exposedHeaders("traceId");
            }
        };
    }

}
