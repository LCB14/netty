/*
 * Copyright 2016 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.channel;

import io.netty.util.IntSupplier;

/**
 * Default select strategy.
 */
final class DefaultSelectStrategy implements SelectStrategy {
    static final SelectStrategy INSTANCE = new DefaultSelectStrategy();

    private DefaultSelectStrategy() {
    }

    @Override
    public int calculateStrategy(IntSupplier selectSupplier, boolean hasTasks) throws Exception {
        /**
         * 三种情况：
         * 1、存在异步执行任务且存在IO就绪事件  -- 返回一个大于 0 的值；
         * 2、存在异步执行任务但不存在IO就绪事件 -- 返回一个等于 0 的值；
         * 3、无待执行的异步任务 -- 返回 -1；
         */
        return hasTasks ? selectSupplier.get() : SelectStrategy.SELECT;
    }
}
