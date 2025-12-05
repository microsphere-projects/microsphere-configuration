package io.microsphere.configuration.nacos.spring.annotation;

import io.microsphere.nacos.client.common.namespace.model.Namespace;
import io.microsphere.spring.config.context.annotation.PropertySourceExtension;
import io.microsphere.spring.config.env.support.DefaultResourceComparator;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.DefaultPropertySourceFactory;
import org.springframework.core.io.support.PropertySourceFactory;

import java.lang.annotation.*;
import java.util.Comparator;

import static io.microsphere.nacos.client.constants.Constants.DEFAULT_GROUP_NAME;
import static io.microsphere.nacos.client.constants.Constants.DEFAULT_NAMESPACE_ID;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
@PropertySourceExtension
@Import(NacosPropertySourceLoader.class)
public @interface NacosPropertySource {
    /**
     * The name of nacos {@link PropertySource}
     */
    @AliasFor(annotation = PropertySourceExtension.class)
    String name() default "defaultNacosPropertySource";

    /**
     * It indicates the property source is auto-refreshed when the configuration is
     * changed.
     *
     * @return default value is <code>true</code>
     */
    @AliasFor(annotation = PropertySourceExtension.class)
    boolean autoRefreshed() default true;

    /**
     * Indicates current {@link PropertySource} is first order or not If specified ,
     * {@link #before()} and {@link #after()} will be ignored, or last order.
     *
     * @return default value is <code>false</code>
     */
    @AliasFor(annotation = PropertySourceExtension.class)
    boolean first() default false;

    /**
     * The relative order before specified {@link PropertySource}
     * <p>
     * If not specified , current {@link PropertySource} will be added last.
     * <p>
     * If {@link #first()} specified , current attribute will be ignored.
     *
     * @return the name of {@link PropertySource}, default value is the empty string
     */
    @AliasFor(annotation = PropertySourceExtension.class)
    String before() default "";

    /**
     * The relative order after specified {@link PropertySource}
     * <p>
     * If not specified , current {@link PropertySource} will be added last.
     * <p>
     * If {@link #first()} specified , current attribute will be ignored.
     *
     * @return the name of {@link PropertySource}, default value is the empty string
     */
    @AliasFor(annotation = PropertySourceExtension.class)
    String after() default "";

    /**
     * Indicate the key(s) of the property source resource(s) to be loaded.
     * <p>The resource format is supported by the specified {@link #factory()}
     * , for example, {@code "/com/myco/app.properties"} or {@code "/path/to/file.xml"}.
     * <p>The etcd key wildcards (e.g. /*.properties) also are permitted;
     * <p>${...} placeholders will be resolved against any/all property sources already
     * registered with the {@code Environment}.
     * <p>Each key will be added to the enclosing {@code Environment} as its own
     * property source, and in the order declared.
     *
     * @see #key()
     */
    @AliasFor(annotation = PropertySourceExtension.class)
    String[] value() default {};

    /**
     * Indicate the key(s) of the property source resource(s) to be loaded.
     * <p>The resource format is supported by the specified {@link #factory()}
     * , for example, {@code "/com/myco/app.properties"} or {@code "/path/to/file.xml"}.
     * <p>The etcd key wildcards (e.g. /*.properties) also are permitted;
     * <p>${...} placeholders will be resolved against any/all property sources already
     * registered with the {@code Environment}.
     * <p>Each key will be added to the enclosing {@code Environment} as its own
     * property source, and in the order declared.
     *
     * @see #value()
     */
    @AliasFor(annotation = PropertySourceExtension.class, attribute = "value")
    String[] key() default {};

    /**
     * Indicate the resources to be sorted when {@link #value()} specifies the resource location wildcards
     * <p>For example, {@code "classpath:/com/myco/*.properties"}, suppose there are two resources named
     * "a.properties" and "b.properties" where two instances of {@link Resource} will be resolved, they are
     * the sources of {@link org.springframework.core.env.PropertySource}, thus it has to sort
     * them to indicate the order of {@link org.springframework.core.env.PropertySource} that will be added to
     * the enclosing {@code Environment}.
     *
     * <p>Default is {@link DefaultResourceComparator}
     *
     * @see DefaultResourceComparator
     */
    @AliasFor(annotation = PropertySourceExtension.class)
    Class<? extends Comparator<Resource>> resourceComparator() default DefaultResourceComparator.class;

    /**
     * Indicate if a failure to find a {@link #value property resource} should be
     * ignored.
     * <p>{@code true} is appropriate if the properties file is completely optional.
     * <p>Default is {@code false}.
     */
    @AliasFor(annotation = PropertySourceExtension.class)
    boolean ignoreResourceNotFound() default false;

    /**
     * A specific character encoding for the given resources.
     * <p>Default is "UTF-8"
     */
    @AliasFor(annotation = PropertySourceExtension.class)
    String encoding() default "UTF-8";

    /**
     * Specify a custom {@link PropertySourceFactory}, if any.
     * <p>By default, a default factory for standard resource files will be used.
     * <p>Default is {@link DefaultPropertySourceFactory}
     *
     * @see DefaultPropertySourceFactory
     */
    @AliasFor(annotation = PropertySourceExtension.class)
    Class<? extends PropertySourceFactory> factory() default DefaultPropertySourceFactory.class;

    /**
     * {@link Namespace#getNamespaceId() the id of namespace}, a.k.a the "tenant".
     * if not specified, the {@link DEFAULT_NAMESPACE_ID "public" namespace} will be used.
     */
    String nameSpaceId() default DEFAULT_NAMESPACE_ID;

    /**
     * the group of Config. if not specified, the "DEFAULT_GROUP" will be used.
     */
    String group() default DEFAULT_GROUP_NAME;

    /**
     * the data id of Config
     */
    String dataId() default "DEFAULT_DATA_ID";
}
