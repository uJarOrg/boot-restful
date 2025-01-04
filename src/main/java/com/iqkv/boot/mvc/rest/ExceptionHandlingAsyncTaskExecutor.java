/*
 * Copyright 2024 IQKV.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iqkv.boot.mvc.rest;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.task.AsyncTaskExecutor;

/**
 * ExceptionHandlingAsyncTaskExecutor class.
 */
public class ExceptionHandlingAsyncTaskExecutor implements AsyncTaskExecutor, InitializingBean, DisposableBean {

  static final String EXCEPTION_MESSAGE = "Caught async exception";

  private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandlingAsyncTaskExecutor.class);

  private final AsyncTaskExecutor executor;

  /**
   * Constructor for ExceptionHandlingAsyncTaskExecutor.
   *
   * @param executor a {@link org.springframework.core.task.AsyncTaskExecutor} object.
   */
  public ExceptionHandlingAsyncTaskExecutor(AsyncTaskExecutor executor) {
    this.executor = executor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void execute(Runnable task) {
    executor.execute(createWrappedRunnable(task));
  }


  private <T> Callable<T> createCallable(Callable<T> task) {
    return () -> {
      try {
        return task.call();
      } catch (Exception e) {
        handle(e);
        throw e;
      }
    };
  }

  private Runnable createWrappedRunnable(Runnable task) {
    return () -> {
      try {
        task.run();
      } catch (Exception e) {
        handle(e);
      }
    };
  }

  /**
   * handle
   *
   * @param e a {@link java.lang.Exception} object.
   */
  protected void handle(Exception e) {
    LOG.error(EXCEPTION_MESSAGE, e);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Future<?> submit(Runnable task) {
    return executor.submit(createWrappedRunnable(task));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public <T> Future<T> submit(Callable<T> task) {
    return executor.submit(createCallable(task));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void destroy() throws Exception {
    if (executor instanceof DisposableBean) {
      DisposableBean bean = (DisposableBean) executor;
      bean.destroy();
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void afterPropertiesSet() throws Exception {
    if (executor instanceof InitializingBean) {
      InitializingBean bean = (InitializingBean) executor;
      bean.afterPropertiesSet();
    }
  }
}
