package io.microsphere.configuration.nacos.spring.annotation;

import io.microsphere.nacos.client.NacosClientConfig;
import io.microsphere.nacos.client.common.config.ConfigClient;
import io.microsphere.nacos.client.common.config.ConfigType;
import io.microsphere.nacos.client.common.config.model.NewConfig;
import io.microsphere.nacos.client.transport.OpenApiHttpClient;
import io.microsphere.nacos.client.v1.config.OpenApiConfigClient;
import io.microsphere.nacos.client.v1.config.util.ConfigUtil;
import io.microsphere.spring.config.env.support.JsonPropertySourceFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.Assert;
import org.springframework.util.StreamUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.Charset;
import java.util.HashSet;
import java.util.Set;

import static io.microsphere.nacos.client.constants.Constants.DEFAULT_GROUP_NAME;
import static io.microsphere.nacos.client.constants.Constants.DEFAULT_NAMESPACE_ID;
import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @Author huangxingw
 * @Date 2024-11-08
 */
@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {
        NacosPropertySourceTest.class,
        NacosPropertySourceTest.Config.class,
})
public class NacosPropertySourceTest {

    public static final Set<String> TEST_CONFIG_TAGS = new HashSet<>(asList("test-tag-1", "test-tag-2", "test-tag-3"));

    public static final String TEST_CONFIG_APP_NAME = "test-app";

    public static final String TEST_CONFIG_DESCRIPTION = "This is a description for test-config";

    public static final String TEST_CONFIG_OPERATOR = "microsphere-nacos-client";

    public static final String TEST_CONFIG_USE = "test";

    public static final String TEST_CONFIG_EFFECT = "Effect 1";

    public static final String TEST_CONFIG_SCHEMA = "test config schema";

    public static final String TEST_DATA_ID = "DEFAULT_DATA_ID";

    public static final ConfigType CONFIG_TYPE = ConfigType.TEXT;

    @Autowired
    private Environment environment;

    private static GenericContainer nacosServer;

    private static ConfigClient client;

    @BeforeClass
    public static void initNacosServer() throws Exception {
        String nacosServerImage = "nacos/nacos-server:latest";
        nacosServer = new GenericContainer(DockerImageName.parse(nacosServerImage))
                .withEnv("MODE", "standalone")
                .withExposedPorts(8848);
        nacosServer.start();
        String serverAddress = nacosServer.getHost() + ":" + nacosServer.getFirstMappedPort();

        System.setProperty("microsphere.nacos.client.serverAddress", serverAddress);

        String configId = ConfigUtil.buildConfigId(DEFAULT_NAMESPACE_ID, DEFAULT_GROUP_NAME, TEST_DATA_ID);
        client = OpenApiConfigClientHolder.getNacosClientMap().computeIfAbsent(configId, e -> {
            NacosClientConfig config = new NacosClientConfig();
            config.setLongPollingTimeout(5000);
            config.setServerAddress(serverAddress);
            OpenApiHttpClient openApiHttpClient = new OpenApiHttpClient(config);
            Assert.notNull(openApiHttpClient, "OpenApiHttpClient must not be null");
            return new OpenApiConfigClient(openApiHttpClient, config);
        });
        mockConfig();
    }


    private static void mockConfig() throws Exception {
        ResourcePatternResolver patternResolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = patternResolver.getResources("classpath:/META-INF/nacos/*.json");
        for (Resource resource : resources) {
            String fileName = resource.getFilename();
            String key = fileName;
            String data = StreamUtils.copyToString(resource.getInputStream(), Charset.defaultCharset());
            writeObject(key, data);
        }
    }

    private static void writeObject(String key, String data) {
        NewConfig newConfig = createNewConfig(data);
        client.publishConfig(newConfig);
    }

    private static NewConfig createNewConfig(String data) {
        NewConfig newConfig = new NewConfig();
        newConfig.setNamespaceId(DEFAULT_NAMESPACE_ID);
        newConfig.setGroup(DEFAULT_GROUP_NAME);
        newConfig.setDataId(TEST_DATA_ID);
        newConfig.setContent(data);
        newConfig.setTags(TEST_CONFIG_TAGS);
        newConfig.setAppName(TEST_CONFIG_APP_NAME);
        newConfig.setDescription(TEST_CONFIG_DESCRIPTION);
        newConfig.setOperator(TEST_CONFIG_OPERATOR);
        newConfig.setUse(TEST_CONFIG_USE);
        newConfig.setEffect(TEST_CONFIG_EFFECT);
        newConfig.setSchema(TEST_CONFIG_SCHEMA);
        newConfig.setType(CONFIG_TYPE);
        return newConfig;
    }


    @AfterClass
    public static void afterNacosServer(){
        if (nacosServer != null) {
            nacosServer.stop();
            System.getProperties().remove("microsphere.nacos.client.serverAddress");
        }
    }

    @Test
    public void test() throws Exception {
        assertEquals("mercyblitz", environment.getProperty("my.name"));

        reWriteObject("test.json", "{ \"my.name\": \"Mercy Ma\" }");

        Thread.sleep(6 * 1000);

        assertEquals("Mercy Ma", environment.getProperty("my.name"));
    }

    private void reWriteObject(String key, String data){
        assertTrue(client.publishConfigContent(DEFAULT_NAMESPACE_ID, DEFAULT_GROUP_NAME, TEST_DATA_ID, data));
    }

    @NacosPropertySource(
            key = "test.json",
            factory = JsonPropertySourceFactory.class)
    static class Config {

    }
}
