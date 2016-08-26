/*
 * Copyright (C) 2016 Tercio Gaudencio Filho
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with the License. You may
 * obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.github.x3333.dagger.interceptor.jpa;

import com.github.x3333.dagger.interceptor.MethodInterceptor;
import com.github.x3333.dagger.interceptor.MethodInvocation;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

/**
 * Intercept a method to make it transactional.
 * 
 * @author Tercio Gaudencio Filho (terciofilho [at] gmail.com)
 */
public class TransactionalInterceptor implements MethodInterceptor {

  private final JpaService service;
  private final ThreadLocal<Boolean> shouldClose = new ThreadLocal<Boolean>();

  //

  @Inject
  public TransactionalInterceptor(final JpaService service) {
    this.service = service;
  }

  //

  @Override
  @SuppressWarnings("unchecked")
  public <T> T invoke(final MethodInvocation invocation) throws Throwable {
    if (!service.hasBegun()) {
      service.begin();
      shouldClose.set(true);
    }

    final EntityManager em = service.get();
    final EntityTransaction transaction = em.getTransaction();

    // If there is an active transaction, join.
    if (transaction.isActive()) {
      return (T) invocation.proceed();
    }

    transaction.begin();

    final T result;
    try {
      result = (T) invocation.proceed();
    } catch (final Exception e) {
      final boolean rollback = doRollback(transaction, e, invocation.annotation(Transactional.class));
      if (rollback) {
        transaction.rollback();
      } else {
        transaction.commit();
      }
      throw e; // Continue exception flow
    } finally {
      // Close the EM in case we started work and transaction is not active anymore.
      if (Boolean.TRUE.equals(shouldClose.get()) && !transaction.isActive()) {
        shouldClose.remove();
        service.end();
      }
    }

    try {
      transaction.commit();
    } finally {
      // Close the EM if we begin the work
      if (Boolean.TRUE.equals(shouldClose.get())) {
        shouldClose.remove();
        service.end();
      }
    }

    return result;
  }

  private boolean doRollback(//
      final EntityTransaction transaction, //
      final Exception e, //
      final Transactional transactional) {
    for (final Class<? extends Exception> rollbackException : transactional.rollbackOn()) {
      if (rollbackException.isInstance(e)) {
        return true;
      }
    }
    return false;
  }

}
