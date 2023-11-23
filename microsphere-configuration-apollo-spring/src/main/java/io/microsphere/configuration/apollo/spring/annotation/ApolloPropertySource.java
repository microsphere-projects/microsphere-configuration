/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.microsphere.configuration.apollo.spring.annotation;

import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.PropertySource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.ctrip.framework.apollo.core.ConfigConsts.APOLLO_META_KEY;
import static com.ctrip.framework.apollo.core.ConfigConsts.NAMESPACE_APPLICATION;

/**
 * The annotation for Apollo {@link PropertySource}
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see EnableApolloConfig
 * @see ApolloPropertySourceBeanDefinitionRegistrar
 * @since 1.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@EnableApolloConfig
@Import(ApolloPropertySourceBeanDefinitionRegistrar.class)
public @interface ApolloPropertySource {

    /**
     * Alias for {@link EnableApolloConfig#value()}
     *
     * @see EnableApolloConfig#value()
     */
    @AliasFor(annotation = EnableApolloConfig.class, attribute = "value")
    String[] namespace() default "${" + APOLLO_META_KEY + ":" + NAMESPACE_APPLICATION + "}";

    /**
     * Alias for {@link EnableApolloConfig#order()}
     *
     * @see EnableApolloConfig#order()
     */
    @AliasFor(annotation = EnableApolloConfig.class, attribute = "order")
    int order() default Ordered.LOWEST_PRECEDENCE;

    /**
     * It indicates the property source is auto-refreshed when the configuration is
     * changed.
     *
     * @return default value is <code>true</code>
     */
    boolean autoRefreshed() default true;
}
