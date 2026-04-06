/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.os890.cdi.addon.transactionalconfig.impl;

import org.apache.deltaspike.core.util.AnnotationUtils;
import org.apache.deltaspike.core.util.ReflectionUtils;
import org.os890.cdi.addon.transactionalconfig.api.ConfigTransactionAware;
import org.os890.cdi.addon.transactionalconfig.api.RefreshAware;
import org.os890.cdi.addon.transactionalconfig.api.TransactionalConfig;

import jakarta.enterprise.event.Observes;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.Extension;
import jakarta.enterprise.inject.spi.ProcessAnnotatedType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * CDI extension that collects configuration keys from interfaces annotated
 * with {@link TransactionalConfig} during bean discovery.
 *
 * <p>Method names on those interfaces (excluding {@link RefreshAware} and
 * {@link ConfigTransactionAware} methods) are treated as configuration keys.</p>
 */
public class ConfigKeyCollectorExtension implements Extension {

    private Set<String> configKeys = new HashSet<>();

    /**
     * Observes annotated types and collects method names from
     * {@link TransactionalConfig}-annotated interfaces as config keys.
     *
     * @param pat the annotated type event
     * @param bm  the bean manager
     */
    protected void inspectConfigKey(@Observes ProcessAnnotatedType<?> pat, BeanManager bm) {
        Class<?> beanClass = pat.getAnnotatedType().getJavaClass();
        if (!beanClass.isInterface()) {
            return;
        }

        Set<Annotation> annotations = pat.getAnnotatedType().getAnnotations();
        TransactionalConfig transactionalConfig = AnnotationUtils.findAnnotation(
                bm, annotations.toArray(new Annotation[0]), TransactionalConfig.class);

        if (transactionalConfig == null) {
            return;
        }

        for (Method method : ReflectionUtils.getAllDeclaredMethods(beanClass)) {
            Class<?> currentClass = method.getDeclaringClass();
            if (RefreshAware.class.equals(currentClass) || ConfigTransactionAware.class.equals(currentClass)) {
                continue;
            }
            configKeys.add(method.getName());
        }
    }

    /**
     * Returns the collected configuration keys.
     *
     * @return an unmodifiable set of config keys
     */
    public Set<String> getConfigKeys() {
        return Collections.unmodifiableSet(configKeys);
    }
}
