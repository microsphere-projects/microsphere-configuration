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
package io.microsphere.configuration.nacos.spring.annotation;

import io.microsphere.nacos.client.NacosClientConfig;
import io.microsphere.nacos.client.OpenApiVersion;
import io.microsphere.nacos.client.common.OpenApiTemplateClient;
import io.microsphere.nacos.client.common.config.ConfigClient;
import io.microsphere.nacos.client.common.config.event.ConfigChangedEvent;
import io.microsphere.nacos.client.v1.OpenApiNacosClient;
import io.microsphere.nacos.client.v2.OpenApiNacosClientV2;
import io.microsphere.spring.config.context.annotation.PropertySourceExtensionLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.microsphere.util.ShutdownHookUtils.addShutdownHookCallback;

/**
 * {@link NacosPorpertySource} {@link PropertySource} Loader to load the nacos Configuration:
 *
 * @author <a href="mailto:walklown@gmail.com">Mercy</a>
 * @since 1.0.0
 */
public class NacosPropertySourceLoader extends PropertySourceExtensionLoader<NacosPorpertySource, NacosPropertySourceAttributes> {

    private static final Logger logger = LoggerFactory.getLogger(NacosPropertySourceLoader.class);

    private static final Map<String, ConfigClient> configCientCache;

    static {
        configCientCache = new HashMap<>();
        addShutdownHookCallback(new Runnable() {
            @Override
            public void run() {
                // Close clients
                for (ConfigClient value : configCientCache.values()) {
                    try {
                        ((OpenApiTemplateClient) value).getOpenApiClient().close();
                    } catch (Exception e) {
                        logger.error("Fail to close nacos client", e);
                    }
                }
                // Clear clients cache when JVM is shutdown
                configCientCache.clear();
            }
        });
    }

    @Override
    protected Resource[] resolveResources(NacosPropertySourceAttributes nacosPropertySourceAttributes,
                                          String propertySourceName, String resourceValue) throws Throwable {

        ConfigClient client = getClient(nacosPropertySourceAttributes);
        String value = client.getConfigContent(resourceValue);
        Resource[] resources = new Resource[1];
        resources[0] = new ByteArrayResource(value.getBytes(), resourceValue);
        return resources;
    }

    @Override
    protected void configureResourcePropertySourcesRefresher(NacosPropertySourceAttributes nacosPropertySourceAttributes,
                                                             List<PropertySourceResource> propertySourceResources,
                                                             CompositePropertySource propertySource,
                                                             ResourcePropertySourcesRefresher refresher) throws Throwable {
        ConfigClient client = getClient(nacosPropertySourceAttributes);
        int size = propertySourceResources.size();
        for (PropertySourceResource propertySourceResource : propertySourceResources) {
            String resourceValue = propertySourceResource.getResourceValue();
            client.addEventListener(resourceValue, event -> onConfigChanged(event, refresher));
        }
    }

    private void onConfigChanged(ConfigChangedEvent event, ResourcePropertySourcesRefresher refresher) {
        if (event.isCreated() || event.isModified()) {
            ByteArrayResource resource = new ByteArrayResource(event.getContent().getBytes());
            try {
                refresher.refresh(event.getDataId(), resource);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }
    }

    private ConfigClient getClient(NacosPropertySourceAttributes nacosPropertySourceAttributes) {
        String key = nacosPropertySourceAttributes.getName();
        return configCientCache.computeIfAbsent(key, k -> {
            NacosClientConfig config = new NacosClientConfig();
            config.setServerAddress(nacosPropertySourceAttributes.getServerAddress());
            if (OpenApiVersion.V1.equals(nacosPropertySourceAttributes.getVersion())) {
                return new OpenApiNacosClient(config);
            } else if (OpenApiVersion.V2.equals(nacosPropertySourceAttributes.getVersion())) {
                return new OpenApiNacosClientV2(config);
            } else {
                throw new RuntimeException("Unsupported nacos open api version " + nacosPropertySourceAttributes.getVersion());
            }
        });
    }

}
