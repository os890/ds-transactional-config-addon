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

import org.os890.cdi.addon.transactionalconfig.api.ConfigTransactionAware;
import org.os890.cdi.addon.transactionalconfig.api.TransactionalConfig;

/**
 * Test configuration interface with three config properties.
 */
@TransactionalConfig
public interface SimpleConfig extends ConfigTransactionAware {

    /**
     * Returns the value for key "value1".
     *
     * @return the value1 string
     */
    String value1();

    /**
     * Returns the value for key "value2".
     *
     * @return the value2 integer
     */
    Integer value2();

    /**
     * Returns the value for key "random" (UUID from RandomConfigSource).
     *
     * @return a random string
     */
    String random();
}
