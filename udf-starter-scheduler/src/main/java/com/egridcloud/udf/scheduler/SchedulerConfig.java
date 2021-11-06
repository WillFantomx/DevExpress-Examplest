/**
 * SchedulerConfig.java
 * Created at 2017-06-01
 * Created by Administrator
 * Copyright (C) 2016 egridcloud.com, All rights reserved.
 */
package com.egridcloud.udf.scheduler;

import java.io.IOException;
import java.util.Properties;

import javax.sql.DataSource;

import org.quartz.spi.JobFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * 描述 : SchedulerConfig
 *
 * @author Administrator
 *
 */
@Configuration
public class SchedulerConfig {

  /**
   * 描述 : schedulerProperties
   */
  @Autowired
  private SchedulerProperties schedulerProperties;

  /**
   * 描述 : jobFactory
   *
   * @param applicationContext applicationContext
   * @return JobFactory
   */
  @Bean
  public JobFactory jobFactory(ApplicationContext applicationContext) {
    AutowiringSpringBeanJobFactory jobFactory = new AutowiringSpringBeanJobFactory();
    jobFactory.setApplicationContext(applicationContext);
    return jobFactory;
  }

  /**
   * 描述 : schedulerFactoryBean
   *
   * @param dataSource dataSource
   * @param jobFactory jobFactory
   * @param quartzProperties quartzProperties
   * @return SchedulerFactoryBean
   * @throws IOException IOException
   */
  @Bean
  public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource, JobFactory jobFactory,
      Properties quartzProperties) throws IOException {
    SchedulerFactoryBean factory = new SchedulerFactoryBean();
    factory.setDataSource(dataSource);
    factory.setJobFactory(jobFactory);
    //属性设置
    factory.setQuartzProperties(quartzProperties);
    factory.setOverwriteExistingJobs(schedulerProperties.getOverwriteExistingJobs());
    factory.setAutoStartup(schedulerProperties.getAutoStartup());
    factory.setStartupDelay(schedulerProperties.getStartupDelay());
    factory.setApplicationContextSchedulerContextKey(
        schedulerProperties.getApplicationContextSchedulerContextKey());
    //返回
    return factory;
  }

  /**
   * 描述 : quartzProperties
   *
   * @return Properties
   * @throws IOException IOException
   */
  @Bean
  public Properties quartzProperties() throws IOException {
    PropertiesFactoryBean propertiesFactoryBean = new PropertiesFactoryBean();
    propertiesFactoryBean
        .setLocation(new ClassPathResource(schedulerProperties.getQuartzPropertiesPath()));
    propertiesFactoryBean.afterPropertiesSet();
    return propertiesFactoryBean.getObject();
  }

}
