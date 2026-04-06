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
import org.apache.deltaspike.core.api.provider.BeanProvider;
import org.apache.deltaspike.core.spi.config.ConfigSource;
import org.os890.cdi.addon.transactionalconfig.api.SnapshotAwareConfigSource;

import jakarta.enterprise.context.Dependent;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A {@link ConfigSource} that provides a thread-local, immutable snapshot of
 * all configuration values for the duration of a configuration transaction.
 *
 * <p>During a transaction, this source has the highest ordinal
 * ({@link Integer#MAX_VALUE}) so it overrides all other sources.</p>
 */
@Dependent
public class SnapshotAwareDataSource implements ConfigSource {

    private static final ThreadLocal<Map<String, String>> TX_CONFIG = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> TX_PER_REQUEST = new ThreadLocal<>();

    /**
     * Checks whether a configuration transaction is currently active on this thread.
     *
     * @return {@code true} if a transaction is active
     */
    public static boolean isConfigTransactionStarted() {
        return TX_CONFIG.get() != null;
    }

    /**
     * Checks whether the current configuration transaction was started per-request.
     *
     * @return {@code true} if started per-request
     */
    public static boolean isConfigTransactionStartedPerRequest() {
        return Boolean.TRUE.equals(TX_PER_REQUEST.get());
    }

    /**
     * Begins a configuration transaction by creating a snapshot of all config sources.
     *
     * @param isStartedForRequest           whether the transaction is per-request scoped
     * @param limitToPartialBeanConfigKeys  whether to limit non-scannable source queries
     *                                      to known partial-bean config keys
     */
    public static void begin(boolean isStartedForRequest, boolean limitToPartialBeanConfigKeys) {
        for (int i = 0; i < 5; i++) { //max. 5 retries
            TX_CONFIG.set(createConfigSnapshot(limitToPartialBeanConfigKeys));

            if (TX_CONFIG.get() != null) {
                break;
            }
        }
        TX_PER_REQUEST.set(isStartedForRequest);
    }

    /**
     * Ends the current configuration transaction and cleans up thread-local state.
     */
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

    private static Map<String, String> createConfigSnapshot(boolean limitToPartialBeanConfigKeys) {
        List<ConfigSource> allConfigSources = new ArrayList<>(Arrays.asList(ConfigResolver.getConfigSources()));

        Iterator<ConfigSource> configSourceIterator = allConfigSources.iterator();

        while (configSourceIterator.hasNext()) {
            if (configSourceIterator.next() instanceof SnapshotAwareDataSource) {
                configSourceIterator.remove();
            }
        }

        // must use a new list because Arrays.asList() is resistant to sorting on some JVMs:
        List<ConfigSource> appConfigSources = sortAscending(new ArrayList<>(allConfigSources));
        Map<String, String> result = new HashMap<>();

        long readConfigStartTimestamp = System.currentTimeMillis();

        List<ConfigSource> manualQueryConfigSources = new ArrayList<>();
        List<SnapshotAwareConfigSource> snapshotAwareConfigSources = new ArrayList<>();

        for (ConfigSource configSource : appConfigSources) {
            if (configSource.isScannable()) {
                result.putAll(configSource.getProperties());
            } else if (limitToPartialBeanConfigKeys) {
                manualQueryConfigSources.add(configSource);
            }

            //after the values were processed, check if there config-source changed in the meantime
            if (configSource instanceof SnapshotAwareConfigSource) {
                snapshotAwareConfigSources.add((SnapshotAwareConfigSource) configSource);
            }
        }

        if (limitToPartialBeanConfigKeys) {
            ConfigKeyCollectorExtension configKeyCollectorExtension =
                    BeanProvider.getContextualReference(ConfigKeyCollectorExtension.class);

            for (String configKey : configKeyCollectorExtension.getConfigKeys()) {
                for (ConfigSource configSource : manualQueryConfigSources) {
                    String value = configSource.getPropertyValue(configKey);
                    if (value != null) {
                        result.put(configKey, value);
                        break;
                    }
                }
            }
        }

        for (SnapshotAwareConfigSource configSource : snapshotAwareConfigSources) {
            if (configSource.getTimestampOfLatestChange() > readConfigStartTimestamp) {
                return null;
            }
        }

        return Collections.unmodifiableMap(result);
    }

    private static List<ConfigSource> sortAscending(List<ConfigSource> configSources) {
        Collections.sort(configSources, new Comparator<ConfigSource>() {
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
