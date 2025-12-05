package io.microsphere.configuration.nacos.spring.annotation;

import io.microsphere.spring.config.context.annotation.PropertySourceExtensionAttributes;
import org.springframework.core.env.PropertyResolver;

import java.util.Map;

/**
 * @Author huangxingw
 * @Date 2024-11-07
 */
public class NacosPropertySourceAttributes extends PropertySourceExtensionAttributes<NacosPropertySource> {
    public NacosPropertySourceAttributes(Map<String, Object> another, Class<NacosPropertySource> annotationType, PropertyResolver propertyResolver) {
        super(another, annotationType, propertyResolver);
    }

    public final String[] getKeys() {
        return getValue();
    }

    public final String getTarget() {
        return getString("target");
    }

    public final String[] getEndpoints() {
        return getStringArray("endpoints");
    }

    public final String getNameSpaceId() {
        return getString("nameSpaceId");
    }

    public final String getGroup() {
        return getString("group");
    }

    public final String getDataId() {
        return getString("dataId");
    }

}
