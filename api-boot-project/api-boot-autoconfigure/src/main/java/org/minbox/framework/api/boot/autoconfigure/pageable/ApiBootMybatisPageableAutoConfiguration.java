/*
 * Copyright [2019] [恒宇少年 - 于起宇]
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 *
 */

package org.minbox.framework.api.boot.autoconfigure.pageable;

import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.session.SqlSessionFactory;
import org.minbox.framework.mybatis.pageable.common.configure.PageableConfigurer;
import org.minbox.framework.mybatis.pageable.interceptor.MyBatisExecutePageableInterceptor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Properties;

/**
 * ApiBoot Mybatis Pageable Auto Configuration
 *
 * @author 恒宇少年
 */
@ConditionalOnBean(SqlSessionFactory.class)
@EnableConfigurationProperties(ApiBootMyBatisPageableProperties.class)
@ConditionalOnClass(MyBatisExecutePageableInterceptor.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
public class ApiBootMybatisPageableAutoConfiguration {
    /**
     * mybatis pageable properties
     */
    private ApiBootMyBatisPageableProperties pageableProperties;
    /**
     * mybatis sqlSession factory
     */
    private List<SqlSessionFactory> sqlSessionFactoryList;
    /**
     * mybatis pageable configurer
     */
    private PageableConfigurer pageableConfigurer;

    public ApiBootMybatisPageableAutoConfiguration(ApiBootMyBatisPageableProperties myBatisPageableProperties,
                                                   ObjectProvider<List<SqlSessionFactory>> interceptorsProvider,
                                                   ObjectProvider<PageableConfigurer> pageableConfigurerObjectProvider) {
        this.pageableProperties = myBatisPageableProperties;
        this.sqlSessionFactoryList = interceptorsProvider.getIfAvailable();
        this.pageableConfigurer = pageableConfigurerObjectProvider.getIfAvailable();
    }

    /**
     * init interceptors
     */
    @PostConstruct
    void addInterceptors() {
        Interceptor interceptor = new MyBatisExecutePageableInterceptor();
        // set properties to interceptor
        Properties properties = pageableProperties.convertProperties();
        interceptor.setProperties(properties);

        for (SqlSessionFactory sqlSessionFactory : sqlSessionFactoryList) {
            // pre
            addPreInterceptors(sqlSessionFactory);
            // mybatis pageable interceptor
            sqlSessionFactory.getConfiguration().addInterceptor(interceptor);
            // post
            addPostInterceptors(sqlSessionFactory);
        }
    }

    /**
     * add pre interceptors
     *
     * @param sqlSessionFactory The {@link SqlSessionFactory} instance
     */
    void addPreInterceptors(SqlSessionFactory sqlSessionFactory) {
        if (allowPageableConfigurer() && pageableConfigurer.getPrePlugins() != null) {
            loopAddInterceptor(pageableConfigurer.getPrePlugins(), sqlSessionFactory);
        }
    }

    /**
     * add after interceptors
     *
     * @param sqlSessionFactory The {@link SqlSessionFactory} instance
     */
    void addPostInterceptors(SqlSessionFactory sqlSessionFactory) {
        if (allowPageableConfigurer() && pageableConfigurer.getPostPlugins() != null) {
            loopAddInterceptor(pageableConfigurer.getPostPlugins(), sqlSessionFactory);
        }
    }

    /**
     * add interceptor to sqlSessionFactory
     *
     * @param interceptors      interceptors
     * @param sqlSessionFactory sqlSessionFactory instance
     */
    void loopAddInterceptor(List<Interceptor> interceptors, SqlSessionFactory sqlSessionFactory) {
        for (Interceptor interceptor : interceptors) {
            sqlSessionFactory.getConfiguration().addInterceptor(interceptor);
        }
    }

    /**
     * check have pageable configurer
     *
     * @return
     */
    boolean allowPageableConfigurer() {
        return pageableConfigurer != null;
    }
}
