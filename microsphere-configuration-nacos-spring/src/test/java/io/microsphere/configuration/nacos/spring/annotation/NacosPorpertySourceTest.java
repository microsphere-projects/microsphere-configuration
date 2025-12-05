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
import io.microsphere.nacos.client.v1.OpenApiNacosClient;
import io.microsphere.nacos.client.v2.OpenApiNacosClientV2;
import io.microsphere.spring.config.env.support.JsonPropertySourceFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StreamUtils;

import static org.junit.Assert.assertEquals;

/**
 * {@link NacosPorpertySource} Test
 *
 * @author <a href="mailto:mercyblitz@gmail.com">Mercy</a>
 * @since 1.0.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
        NacosPorpertySourceTest.class,
        NacosPorpertySourceTest.Config.class
})
public class NacosPorpertySourceTest {

    @Autowired
    private Environment environment;

    private static ConfigClient client;

    @BeforeClass
    public static void init() throws Exception {
        NacosPorpertySource annotation =
                NacosPorpertySourceTest.Config.class.getAnnotation(NacosPorpertySource.class);

        client = buildClient(annotation);

        // 添加模拟数据
        mockConfig();
    }

    private static ConfigClient buildClient(NacosPorpertySource annotation) throws Exception {
        NacosClientConfig config = new NacosClientConfig();
        config.setServerAddress(annotation.serverAddress());
        if (OpenApiVersion.V1.equals(annotation.openApiVersion())) {
            return new OpenApiNacosClient(config);
        } else if (OpenApiVersion.V2.equals(annotation.openApiVersion())) {
            return new OpenApiNacosClientV2(config);
        } else {
            throw new RuntimeException("Unsupported nacos open api version " + annotation.openApiVersion());
        }
    }

    private static void mockConfig() throws Exception {

        ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();

        Resource[] resources = patternResolver.getResources("classpath:/META-INF/nacos/*.json");

        for (Resource resource : resources) {
            // test.json
            String fileName = resource.getFilename();
            String key = fileName;
            byte[] data = StreamUtils.copyToByteArray(resource.getInputStream());
            writeConfig(key, new String(data));
        }
    }

    private static void writeConfig(String stringKey, String data) throws Exception {
        client.publishConfigContent("DEFAULT_GROUP", stringKey, data);
    }

    @AfterClass
    public static void destroy() throws Exception {
        ((OpenApiTemplateClient) client).getOpenApiClient().close();
    }


    @Test
    public void test() throws Exception {
        assertEquals("microsphere", environment.getProperty("my.name"));

        writeConfig("test.json", "{ \"my.name\": \"Microsphere\" }");

        Thread.sleep(5 * 1000);

        assertEquals("Microsphere", environment.getProperty("my.name"));
    }

    @NacosPorpertySource(
            key = "test.json",
            factory = JsonPropertySourceFactory.class,
            serverAddress = "http://192.168.0.111:8848",
            openApiVersion = OpenApiVersion.V2)
    static class Config {

    }

}
