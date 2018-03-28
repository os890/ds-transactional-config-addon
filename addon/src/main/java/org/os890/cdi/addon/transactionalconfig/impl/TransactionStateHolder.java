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

import javax.annotation.PreDestroy;
import javax.enterprise.context.RequestScoped;

//only used if there is no interceptor which controls SnapshotAwareDataSource directly
//needed to avoid leaks with thread-locals
//optional (in case there is an interceptor) to keep the request-scope optional
@RequestScoped
public class TransactionStateHolder {
    private boolean txStarted = false;

    public void markTransactionAsStarted() {
        txStarted = true;
    }

    public void markTransactionAsFinished() {
        txStarted = false;
    }

    public boolean isTransactionStarted() {
        return txStarted;
    }

    @PreDestroy
    protected void cleanupTransaction() { //just needed to end the transaction in case no try-block is used
        if (txStarted) {
            txStarted = false;
            SnapshotAwareDataSource.end();
        }
    }
}
