/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.luckyframework.proxy.async;

import com.luckyframework.annotations.Configuration;
import com.luckyframework.annotations.EnableAsync;
import org.springframework.lang.Nullable;

import java.util.concurrent.Executor;

/**
 * Interface to be implemented by @{@link Configuration
 * Configuration} classes annotated with @{@link EnableAsync} that wish to customize the
 * {@link Executor} instance used when processing async method invocations or the
 * {@link AsyncUncaughtExceptionHandler} instance used to process exception thrown from
 * async method with {@code void} return type.
 *
 * <p>See @{@link EnableAsync} for usage examples.
 *
 * @author FK7075
 * @version 1.0.0
 * @date 2022/8/21 11:16
 * @see EnableAsync
 */
public interface AsyncConfigurer {

    /**
     * The {@link Executor} instance to be used when processing async
     * method invocations.
     */
    @Nullable
    Executor getAsyncExecutor();

    /**
     * The {@link AsyncUncaughtExceptionHandler} instance to be used
     * when an exception is thrown during an asynchronous method execution
     * with {@code void} return type.
     */
    @Nullable
    AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler();

}
