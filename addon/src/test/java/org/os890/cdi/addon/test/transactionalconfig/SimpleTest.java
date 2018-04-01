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

import org.apache.deltaspike.cdise.api.ContextControl;
import org.apache.deltaspike.testcontrol.api.junit.CdiTestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.os890.cdi.addon.transactionalconfig.impl.SnapshotAwareDataSource;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import static java.lang.Integer.valueOf;

@RunWith(CdiTestRunner.class)
public class SimpleTest {
    @Inject
    private SimpleConfig simpleConfig;

    @Inject
    private SimpleRefreshAwareConfig simpleResetAwareConfig;
    @Inject
    private ContextControl contextControl;

    @Test
    public void implicitConfigTransaction() throws Exception {
        Assert.assertEquals("val 1", simpleConfig.value1());
        Assert.assertEquals(valueOf(2), simpleConfig.value2());
    }

    @Test
    public void resetAwareSimpleConfigValues() {
        String firstValue = simpleConfig.random();
        Assert.assertEquals(firstValue, simpleConfig.random());

        simpleResetAwareConfig.refresh();

        Assert.assertNotEquals(firstValue, simpleConfig.random());
    }

    @Test
    public void stableConfigWithTransaction() throws Exception {
        String firstValue;

        try (SimpleConfig txConfig = simpleConfig) {
            firstValue = txConfig.random();
            Assert.assertEquals(firstValue, txConfig.random());
            Assert.assertEquals(firstValue, txConfig.random());
            Assert.assertEquals(firstValue, txConfig.random());
        }

        try (SimpleConfig txConfig = simpleConfig) {
            Assert.assertNotEquals(firstValue, txConfig.random());
        }
    }

    @Test
    public void simulateConfigTransactionInterceptor() throws Exception {
        contextControl.stopContext(RequestScoped.class); //to ensure that the fallback handling isn't used
        SnapshotAwareDataSource.begin(false, true); //this would be in an interceptor

        String firstValue;

        firstValue = simpleConfig.random();
        Assert.assertEquals(firstValue, simpleConfig.random());
        Assert.assertEquals(firstValue, simpleConfig.random());
        Assert.assertEquals(firstValue, simpleConfig.random());

        SnapshotAwareDataSource.end(); //this would be in an interceptor

        SnapshotAwareDataSource.begin(false, true); //this would be in an interceptor

        Assert.assertNotEquals(firstValue, simpleConfig.random());

        SnapshotAwareDataSource.end(); //this would be in an interceptor
    }
}
