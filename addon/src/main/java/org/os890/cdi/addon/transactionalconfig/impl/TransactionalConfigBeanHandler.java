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

import org.apache.deltaspike.core.api.config.ConfigResolver;
import org.apache.deltaspike.core.util.ClassDeactivationUtils;
import org.apache.deltaspike.core.util.ServiceUtils;
import org.os890.cdi.addon.transactionalconfig.api.ConfigTransactionAware;
import org.os890.cdi.addon.transactionalconfig.api.RefreshAware;
import org.os890.cdi.addon.transactionalconfig.api.TransactionalConfig;
import org.os890.cdi.addon.transactionalconfig.spi.ConverterFactory;
import org.os890.cdi.addon.transactionalconfig.spi.ValueConverter;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

@ApplicationScoped
@TransactionalConfig
public class TransactionalConfigBeanHandler implements InvocationHandler {
    @Inject
    private TransactionStateHolder transactionStateHolder;

    private Map<Class, ValueConverter> converterCache = new HashMap<Class, ValueConverter>();

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (RefreshAware.class.equals(method.getDeclaringClass())) {
            if (!SnapshotAwareDataSource.isConfigTransactionStartedPerRequest()) {
                SnapshotAwareDataSource.end();
            } else {
                transactionStateHolder.cleanupTransaction();
            }
            return null;
        }

        if (!SnapshotAwareDataSource.isConfigTransactionStarted()) {
            if (ConfigTransactionAware.class.isAssignableFrom(proxy.getClass())) {
                if (!transactionStateHolder.isTransactionStarted()) {
                    transactionStateHolder.markTransactionAsStarted();
                    SnapshotAwareDataSource.begin(true, true);
                }
            }
        }

        if (AutoCloseable.class.equals(method.getDeclaringClass())) {
            if (SnapshotAwareDataSource.isConfigTransactionStartedPerRequest()) {
                transactionStateHolder.markTransactionAsFinished();
                SnapshotAwareDataSource.end();
            }
            return null;
        }

        String valueAsString = ConfigResolver.getPropertyValue(method.getName());

        if (valueAsString == null) {
            return null;
        }

        final Class<?> targetType = method.getReturnType();

        ValueConverter valueConverter = converterCache.get(targetType);

        if (valueConverter == null) {
            valueConverter = createValueConverter(targetType);
        }

        if (valueConverter != null) {
            return valueConverter.convert(valueAsString);
        }
        return null;
    }

    private synchronized ValueConverter createValueConverter(Class<?> targetType) {
        ValueConverter valueConverter = converterCache.get(targetType);

        if (valueConverter != null) {
            return valueConverter;
        }

        for (ConverterFactory converterFactory : ServiceUtils.loadServiceImplementations(ConverterFactory.class)) {
            if (!ClassDeactivationUtils.isActivated(converterFactory.getClass())) {
                continue;
            }

            if (converterFactory.isResponsibleFor(targetType)) {
                valueConverter = converterFactory.createConverter(targetType);
                if (valueConverter != null) {
                    converterCache.put(targetType, valueConverter);
                    return valueConverter;
                }
            }
        }
        return null;
    }
}
