package io.microsphere.configuration.nacos.spring.annotation;

import io.microsphere.nacos.client.NacosClientConfig;
import io.microsphere.nacos.client.common.config.ConfigClient;
import io.microsphere.nacos.client.common.config.event.ConfigChangedEvent;
import io.microsphere.nacos.client.common.config.model.Config;
import io.microsphere.nacos.client.transport.OpenApiHttpClient;
import io.microsphere.nacos.client.v1.config.OpenApiConfigClient;
import io.microsphere.nacos.client.v1.config.util.ConfigUtil;
import io.microsphere.spring.boot.context.properties.bind.util.BindUtils;
import io.microsphere.spring.config.context.annotation.PropertySourceExtensionLoader;
import io.microsphere.util.ArrayUtils;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.nio.charset.Charset;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;

/**
 * @Author huangxingw
 * @Date 2024-11-06
 */
public class NacosPropertySourceLoader extends PropertySourceExtensionLoader<NacosPropertySource, NacosPropertySourceAttributes> {

    @Override
    protected Resource[] resolveResources(NacosPropertySourceAttributes extensionAttributes, String propertySourceName, String resourceValue) throws Throwable {
        ConfigClient client = getClient(extensionAttributes);

        String nameSpaceId = extensionAttributes.getNameSpaceId();
        String group = extensionAttributes.getGroup();
        String dataId = extensionAttributes.getDataId();

        Config config = client.getConfig(nameSpaceId, group, dataId);

        if(config == null || !hasText(config.getContent())){
            return null;
        }

        String encoding = extensionAttributes.getEncoding();
        Charset charset = Charset.forName(encoding);

        return ArrayUtils.of(new ByteArrayResource(config.getContent().getBytes(charset), "The nacos configuration from the path : " + resourceValue));
    }

    private OpenApiConfigClient getClient(NacosPropertySourceAttributes extensionAttributes) {
        String nameSpaceId = extensionAttributes.getNameSpaceId();
        String group = extensionAttributes.getGroup();
        String dataId = extensionAttributes.getDataId();
        String configId = ConfigUtil.buildConfigId(nameSpaceId, group, dataId);
        return OpenApiConfigClientHolder.getNacosClientMap().computeIfAbsent(configId, (n) -> {
            NacosClientConfig nacosClientConfig = getNacosClientConfig();
            OpenApiHttpClient openApiHttpClient = new OpenApiHttpClient(nacosClientConfig);
            Assert.notNull(openApiHttpClient, "OpenApiHttpClient must not be null");
            return new OpenApiConfigClient(openApiHttpClient, nacosClientConfig);
        });
    }

    @Override
    protected void configureResourcePropertySourcesRefresher(NacosPropertySourceAttributes extensionAttributes, List<PropertySourceResource> propertySourceResources, CompositePropertySource propertySource, ResourcePropertySourcesRefresher refresher) throws Throwable {
        OpenApiConfigClient client = getClient(extensionAttributes);
        String nameSpaceId = extensionAttributes.getNameSpaceId();
        String group = extensionAttributes.getGroup();
        String dataId = extensionAttributes.getDataId();

        int size = propertySourceResources.size();
        for (int i = 0; i < size; i++) {
            PropertySourceResource propertySourceResource = propertySourceResources.get(i);
            String resourceValue = propertySourceResource.getResourceValue();
            client.addEventListener(nameSpaceId, group, dataId, (event) -> {

                if (event.getKind().equals(ConfigChangedEvent.Kind.MODIFIED)) {
                    String content = event.getContent();
                    if(!hasText(content)){
                        return;
                    }
                    String encoding = extensionAttributes.getEncoding();
                    Charset charset = Charset.forName(encoding);
                    try {
                        refresher.refresh(resourceValue, new ByteArrayResource(content.getBytes(charset)));
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }

            });
        }

    }

    private NacosClientConfig getNacosClientConfig(){
        ConfigurableEnvironment environment = getEnvironment();
        return BindUtils.bind(environment, "microsphere.nacos.client", NacosClientConfig.class);
    }
}
