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
import org.apache.deltaspike.core.spi.config.ConfigSource;

import javax.enterprise.context.Dependent;
import java.util.*;

@Dependent
public class SnapshotAwareDataSource implements ConfigSource {
    private static final ThreadLocal<Map<String, String>> TX_CONFIG = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> TX_PER_REQUEST = new ThreadLocal<>();

    public static boolean isConfigTransactionStarted() {
        return TX_CONFIG.get() != null;
    }

    public static boolean isConfigTransactionStartedPerRequest() {
        return Boolean.TRUE.equals(TX_PER_REQUEST.get());
    }

    public static void begin(boolean isStartedForRequest) {
        TX_CONFIG.set(createConfigSnapshot());
        TX_PER_REQUEST.set(isStartedForRequest);
    }

    public static void end() {
        TX_CONFIG.set(null);
        TX_CONFIG.remove();

        TX_PER_REQUEST.set(null);
        TX_PER_REQUEST.remove();
    }

    private static Map<String, String> currentConfigSnapshot() {
        Map<String, String> txConfig = TX_CONFIG.get();

        if (txConfig != null) {
            return txConfig;
        }

        return Collections.emptyMap(); //no tx -> the other config-sources will handle it
    }

    private static Map<String, String> createConfigSnapshot() {
        List<ConfigSource> allConfigSources = new ArrayList<>(Arrays.asList(ConfigResolver.getConfigSources()));

        Iterator<ConfigSource> configSourceIterator = allConfigSources.iterator();

        while (configSourceIterator.hasNext()) {
            if (configSourceIterator.next() instanceof SnapshotAwareDataSource) {
                configSourceIterator.remove();
                break;
            }
        }

        // must use a new list because Arrays.asList() is resistant to sorting on some JVMs:
        List<ConfigSource> appConfigSources = sortAscending(new ArrayList<>(allConfigSources));
        Map<String, String> result = new HashMap<>();

        for (ConfigSource configSource : appConfigSources) {
            if (configSource.isScannable()) {
                result.putAll(configSource.getProperties());
            }
        }

        return Collections.unmodifiableMap(result);
    }

    private static List<ConfigSource> sortAscending(List<ConfigSource> configSources) {
        Collections.sort(configSources, new Comparator<ConfigSource>() {
            /**
             * {@inheritDoc}
             */
            @Override
            public int compare(ConfigSource configSource1, ConfigSource configSource2) {
                return (configSource1.getOrdinal() > configSource2.getOrdinal()) ? 1 : -1;
            }
        });
        return configSources;
    }

    @Override
    public int getOrdinal() {
        return Integer.MAX_VALUE;
    }

    @Override
    public Map<String, String> getProperties() {
        return currentConfigSnapshot();
    }

    @Override
    public String getPropertyValue(String key) {
        if (!isConfigTransactionStarted()) {
            return null;
        }
        return currentConfigSnapshot().get(key);
    }

    @Override
    public String getConfigName() {
        return "in-memory-snapshot";
    }

    @Override
    public boolean isScannable() {
        return true;
    }
}
