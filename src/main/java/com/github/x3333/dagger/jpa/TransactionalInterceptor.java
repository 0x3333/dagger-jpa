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

package com.github.x3333.dagger.jpa;

import com.github.x3333.dagger.MethodInterceptor;
import com.github.x3333.dagger.MethodInvocation;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.transaction.InvalidTransactionException;

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
  public Object invoke(final MethodInvocation invocation) throws Throwable {
    if (!service.hasBegun()) {
      service.begin();
      shouldClose.set(true);
    }

    final EntityManager em = service.get();
    final EntityTransaction transaction = em.getTransaction();

    // If there is an active transaction, join.
    if (transaction.isActive()) {
      if (Boolean.TRUE.equals(shouldClose.get())) {
        // Should never occur
        throw new InvalidTransactionException("There is an Active transaction in a new EntityManager!");
      }

      return invocation.proceed();
    }

    transaction.begin();

    final Object result;
    try {
      result = invocation.proceed();
    } catch (final Exception e) {
      transaction.rollback(); // Rollback if an Exception has been catch
      throw e; // Continue exception flow
    } finally {
      // Close the EM in case catch has fired(!transaction.isActive())
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

}
