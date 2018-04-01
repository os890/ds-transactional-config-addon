package org.os890.cdi.addon.transactionalconfig.impl;

import org.apache.deltaspike.core.util.AnnotationUtils;
import org.apache.deltaspike.core.util.ReflectionUtils;
import org.os890.cdi.addon.transactionalconfig.api.ConfigTransactionAware;
import org.os890.cdi.addon.transactionalconfig.api.RefreshAware;
import org.os890.cdi.addon.transactionalconfig.api.TransactionalConfig;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ConfigKeyCollectorExtension implements Extension {
    private Set<String> configKeys = new HashSet<>();

    protected void inspectConfigKey(@Observes ProcessAnnotatedType pat, BeanManager bm) {
        Class beanClass = pat.getAnnotatedType().getJavaClass();
        if (!beanClass.isInterface()) {
            return;
        }

        Set<Annotation> annotations = pat.getAnnotatedType().getAnnotations();
        TransactionalConfig transactionalConfig = AnnotationUtils.findAnnotation(bm, annotations.toArray(new Annotation[annotations.size()]), TransactionalConfig.class);

        if (transactionalConfig == null) {
            return;
        }

        for (Method method : ReflectionUtils.getAllDeclaredMethods(beanClass)) {
            Class currentClass = method.getDeclaringClass();
            if (RefreshAware.class.equals(currentClass) || ConfigTransactionAware.class.equals(currentClass)) {
                continue;
            }
            configKeys.add(method.getName());
        }
    }

    public Set<String> getConfigKeys() {
        return Collections.unmodifiableSet(configKeys);
    }
}
