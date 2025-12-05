package io.microsphere.configuration.nacos.spring.annotation;

import io.microsphere.nacos.client.v1.config.OpenApiConfigClient;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author huangxingw
 * @Date 2024-11-12
 */
public class OpenApiConfigClientHolder {

    private static final Map<String, OpenApiConfigClient> nacosClientMap = new ConcurrentHashMap<>();

    public static Map<String, OpenApiConfigClient> getNacosClientMap() {
        return nacosClientMap;
    }
}
