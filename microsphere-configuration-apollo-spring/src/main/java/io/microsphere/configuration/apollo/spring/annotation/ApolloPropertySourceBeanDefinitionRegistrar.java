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

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.model.ConfigChange;
import com.ctrip.framework.apollo.model.ConfigChangeEvent;
import com.ctrip.framework.apollo.spring.annotation.EnableApolloConfig;
import com.ctrip.framework.apollo.spring.config.ConfigPropertySource;
import com.ctrip.framework.apollo.spring.config.PropertySourcesProcessor;
import io.microsphere.spring.config.env.event.PropertySourceChangedEvent;
import io.microsphere.spring.config.env.event.PropertySourcesChangedEvent;
import io.microsphere.spring.context.annotation.BeanCapableImportCandidate;
import io.microsphere.spring.core.annotation.ResolvablePlaceholderAnnotationAttributes;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.StringUtils;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APOLLO_CLUSTER;
import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APOLLO_META;
import static com.ctrip.framework.apollo.core.ApolloClientSystemConsts.APP_ID;
import static com.ctrip.framework.apollo.spring.config.PropertySourcesConstants.APOLLO_BOOTSTRAP_NAMESPACES;
import static com.ctrip.framework.apollo.spring.config.PropertySourcesConstants.APOLLO_PROPERTY_SOURCE_NAME;
import static io.microsphere.spring.config.env.event.PropertySourceChangedEvent.added;
import static io.microsphere.spring.config.env.event.PropertySourceChangedEvent.removed;
import static io.microsphere.spring.config.env.event.PropertySourceChangedEvent.replaced;
import static io.microsphere.spring.core.annotation.ResolvablePlaceholderAnnotationAttributes.of;

/**
 * {@link ApolloPropertySource} {@link ImportBeanDefinitionRegistrar} to register the Apollo Configuration
 * Beans
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @see EnableApolloConfig
 * @see ApolloPropertySource
 * @see PropertySourcesProcessor
 * @since 1.0.0
 */
public class ApolloPropertySourceBeanDefinitionRegistrar extends BeanCapableImportCandidate
        implements ImportBeanDefinitionRegistrar, BeanFactoryPostProcessor, ApplicationContextAware {

    private ResolvablePlaceholderAnnotationAttributes attributes;

    private ApplicationContext context;

    private ConcurrentMap<String, PropertySource> oldPropertySourcesMap;

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        Class<? extends Annotation> annotationType = ApolloPropertySource.class;
        Map<String, Object> annotationAttributes = metadata.getAnnotationAttributes(annotationType.getName());
        ResolvablePlaceholderAnnotationAttributes attributes = of(annotationAttributes, annotationType, getEnvironment());
        setSystemPropertiesFromAttributes(attributes);
        this.attributes = attributes;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
        MutablePropertySources propertySources = environment.getPropertySources();
        PropertySource propertySource = propertySources.get(APOLLO_PROPERTY_SOURCE_NAME);

        if (!(propertySource instanceof CompositePropertySource)) {
            logger.warn("No Apollo PropertySource was found by name : {}", APOLLO_PROPERTY_SOURCE_NAME);
            return;
        }

        CompositePropertySource compositePropertySource = (CompositePropertySource) propertySource;

        boolean autoRefreshed = attributes.getBoolean("autoRefreshed");

        if (autoRefreshed) {
            Collection<PropertySource<?>> subPropertySources = compositePropertySource.getPropertySources();
            oldPropertySourcesMap = new ConcurrentHashMap<>(subPropertySources.size());

            for (PropertySource<?> subPropertySource : subPropertySources) {
                if (subPropertySource instanceof ConfigPropertySource) {
                    ConfigPropertySource configPropertySource = (ConfigPropertySource) subPropertySource;
                    Config config = configPropertySource.getSource();
                    String configPropertySourceName = configPropertySource.getName();
                    oldPropertySourcesMap.computeIfAbsent(configPropertySourceName, name -> {
                        config.addChangeListener(event -> onChanged(name, configPropertySource, event));
                        return clonePropertySource(name, configPropertySource);
                    });
                }
            }
        }
    }

    private void onChanged(String configPropertySourceName, ConfigPropertySource configPropertySource, ConfigChangeEvent configChangeEvent) {

        ConcurrentMap<String, PropertySource> oldPropertySourcesMap = this.oldPropertySourcesMap;

        ApplicationContext context = this.context;

        PropertySource oldPropertySource = oldPropertySourcesMap.get(configPropertySourceName);

        Map<String, Object> addedProperties = new HashMap<>();

        Map<String, Object> modifiedProperties = new HashMap<>();

        Map<String, Object> deletedProperties = new HashMap<>();

        for (String key : configChangeEvent.changedKeys()) {
            ConfigChange configChange = configChangeEvent.getChange(key);
            switch (configChange.getChangeType()) {
                case ADDED:
                    addedProperties.put(key, configChange.getNewValue());
                    break;
                case MODIFIED:
                    modifiedProperties.put(key, configChange.getNewValue());
                    break;
                case DELETED:
                    deletedProperties.put(key, configChange.getNewValue());
                    break;
            }
        }

        MapPropertySource addedPropertySource = new MapPropertySource(configPropertySourceName + "#added", addedProperties);
        MapPropertySource modifiedPropertySource = new MapPropertySource(configPropertySourceName + "#modified", modifiedProperties);
        MapPropertySource deletedPropertySource = new MapPropertySource(configPropertySourceName + "#deleted", deletedProperties);

        PropertySourceChangedEvent addEvent = added(context, addedPropertySource);
        PropertySourceChangedEvent modifiedEvent = replaced(context, oldPropertySource, modifiedPropertySource);
        PropertySourceChangedEvent deletedEvent = removed(context, deletedPropertySource);

        PropertySourcesChangedEvent event = new PropertySourcesChangedEvent(context, addEvent, modifiedEvent, deletedEvent);

        context.publishEvent(event);

        // clone a new PropertySource as the old
        oldPropertySource = clonePropertySource(configPropertySourceName, configPropertySource);

        oldPropertySourcesMap.put(configPropertySourceName, oldPropertySource);
    }

    private PropertySource clonePropertySource(String configPropertySourceName, ConfigPropertySource configPropertySource) {
        String[] propertyNames = configPropertySource.getPropertyNames();
        Map<String, Object> properties = new HashMap<>(propertyNames.length);
        for (String propertyName : propertyNames) {
            Object propertyValue = configPropertySource.getProperty(propertyName);
            properties.put(propertyName, propertyValue);
        }
        return new MapPropertySource(configPropertySourceName, properties);
    }


    private static void setSystemPropertiesFromAttributes(ResolvablePlaceholderAnnotationAttributes attributes) {

        String appId = attributes.getString("appId");
        String meta = attributes.getString("meta");
        String cluster = attributes.getString("cluster");
        String[] namespace = attributes.getStringArray("namespace");

        Properties systemProperties = System.getProperties();

        setSystemProperty(systemProperties, APP_ID, appId);
        setSystemProperty(systemProperties, APOLLO_META, meta);
        setSystemProperty(systemProperties, APOLLO_CLUSTER, cluster);
        setSystemProperty(systemProperties, APOLLO_BOOTSTRAP_NAMESPACES, StringUtils.arrayToCommaDelimitedString(namespace));
    }

    private static void setSystemProperty(Properties systemProperties, String key, String value) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        if (systemProperties.contains(key)) {
            return;
        }
        systemProperties.put(key, value);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }
}
