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

package org.os890.cdi.addon.test.transactionalconfig;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.os890.cdi.addon.dynamictestbean.EnableTestBeans;
import org.os890.cdi.addon.transactionalconfig.impl.SnapshotAwareDataSource;

import static java.lang.Integer.valueOf;

/**
 * Integration tests for the transactional config addon using
 * the dynamic-cdi-test-bean-addon.
 */
@EnableTestBeans
class SimpleTest {

    @Inject
    private SimpleConfig simpleConfig;

    @Inject
    private SimpleRefreshAwareConfig simpleResetAwareConfig;

    @Test
    void implicitConfigTransaction() throws Exception {
        Assertions.assertEquals("val 1", simpleConfig.value1());
        Assertions.assertEquals(valueOf(2), simpleConfig.value2());
    }

    @Test
    void resetAwareSimpleConfigValues() {
        String firstValue = simpleConfig.random();
        Assertions.assertEquals(firstValue, simpleConfig.random());

        simpleResetAwareConfig.refresh();

        Assertions.assertNotEquals(firstValue, simpleConfig.random());
    }

    @Test
    void stableConfigWithTransaction() throws Exception {
        String firstValue;

        try (SimpleConfig txConfig = simpleConfig) {
            firstValue = txConfig.random();
            Assertions.assertEquals(firstValue, txConfig.random());
            Assertions.assertEquals(firstValue, txConfig.random());
            Assertions.assertEquals(firstValue, txConfig.random());
        }

        try (SimpleConfig txConfig = simpleConfig) {
            Assertions.assertNotEquals(firstValue, txConfig.random());
        }
    }

    @Test
    void simulateConfigTransactionInterceptor() throws Exception {
        SnapshotAwareDataSource.begin(false, true); //this would be in an interceptor

        String firstValue;

        firstValue = simpleConfig.random();
        Assertions.assertEquals(firstValue, simpleConfig.random());
        Assertions.assertEquals(firstValue, simpleConfig.random());
        Assertions.assertEquals(firstValue, simpleConfig.random());

        SnapshotAwareDataSource.end(); //this would be in an interceptor

        SnapshotAwareDataSource.begin(false, true); //this would be in an interceptor

        Assertions.assertNotEquals(firstValue, simpleConfig.random());

        SnapshotAwareDataSource.end(); //this would be in an interceptor
    }
}
