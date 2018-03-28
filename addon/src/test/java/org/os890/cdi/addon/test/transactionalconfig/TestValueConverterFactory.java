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

import org.os890.cdi.addon.transactionalconfig.spi.ConverterFactory;
import org.os890.cdi.addon.transactionalconfig.spi.ValueConverter;

public class TestValueConverterFactory implements ConverterFactory {
    @Override
    public boolean isResponsibleFor(Class<?> targetType) {
        return String.class.isAssignableFrom(targetType) || Integer.class.isAssignableFrom(targetType);
    }

    @Override
    public <T> ValueConverter<T> createConverter(Class<T> targetType) {
        if (String.class.isAssignableFrom(targetType)) {
            return (ValueConverter<T>) new ValueConverter<String>() {
                @Override
                public String convert(String value) {
                    return value;
                }
            };
        } else if (Integer.class.isAssignableFrom(targetType)) {
            return (ValueConverter<T>) new ValueConverter<Integer>() {
                @Override
                public Integer convert(String value) {
                    return Integer.parseInt(value);
                }
            };
        }
        throw new IllegalArgumentException(targetType + " isn't supported");
    }
}
