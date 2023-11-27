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

import com.ctrip.framework.apollo.core.ApolloClientSystemConsts;
import com.ctrip.framework.apollo.core.ConfigConsts;
import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.ctrip.framework.apollo.spring.config.PropertySourcesConstants;
import io.microsphere.spring.util.PropertySourcesUtils;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.PropertySource;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APOLLO_CLUSTER;
import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APOLLO_META;
import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APP_ID;
import static com.ctrip.framework.apollo.core.ConfigConsts.CLUSTER_NAME_DEFAULT;
import static com.ctrip.framework.apollo.core.ConfigConsts.NAMESPACE_APPLICATION;
import static com.ctrip.framework.apollo.spring.config.PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES;

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
     * The application id of Apollo config.
     * If the Spring property "app.id" is present, its value will be put
     * {@link System#getProperties() the System Properties}.
     *
     * @return "default" as the default if the Spring property "app.id" is missing
     * @see PropertySourcesUtils#getDefaultPropertiesPropertySource(ConfigurableEnvironment)
     * @see ApolloClientSystemConsts#APP_ID
     */
    String appId() default "${" + APP_ID + ":default}";

    /**
     * The meta-server of Apollo config.
     * If the Spring property "app.meta" is present, its value will be put
     * {@link System#getProperties() the System Properties}.
     *
     * @return the value of Spring property "app.id" as the default
     * @see ApolloClientSystemConsts#APOLLO_META
     */
    String meta() default "${" + APOLLO_META + ":}";

    /**
     * The cluster of Apollo config.
     * If the Spring property "app.cluster" is present, its value will be put
     * {@link System#getProperties() the System Properties}.
     *
     * @return "default" as the default if the Spring property "apollo.bootstrap.namespaces" is missing
     * @see ApolloClientSystemConsts#APOLLO_CLUSTER
     * @see ConfigConsts#CLUSTER_NAME_DEFAULT
     */
    String cluster() default "${" + APOLLO_CLUSTER + ":" + CLUSTER_NAME_DEFAULT + "}";

    /**
     * The namespace(s) of Apollo config.
     * Alias for {@link EnableApolloConfig#value()}.
     * If the Spring property "apollo.bootstrap.namespaces" is present,
     * its value will be put {@link System#getProperties() the System Properties}
     *
     * @return "application" as the default if the Spring property "apollo.bootstrap.namespaces" is missing
     * @see EnableApolloConfig#value()
     * @see PropertySourcesConstants#APOLLO_BOOTSTRAP_NAMESPACES
     * @see ConfigConsts#NAMESPACE_APPLICATION
     */
    @AliasFor(annotation = EnableApolloConfig.class, attribute = "value")
    String[] namespace() default "${" + APOLLO_BOOTSTRAP_NAMESPACES + ":" + NAMESPACE_APPLICATION + "}";

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
