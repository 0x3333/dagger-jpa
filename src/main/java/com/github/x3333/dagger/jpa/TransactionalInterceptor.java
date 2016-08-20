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
import com.github.x3333.dagger.jpa.annotations.Transactional;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

/**
 * Intercepts all methods in a class to made it transactional.
 * 
 * @author Tercio Gaudencio Filho (tercio [at] imapia.com.br)
 */
public class TransactionalInterceptor implements MethodInterceptor<Transactional> {

  private final JpaService service;
  private final ThreadLocal<Boolean> shouldClose = new ThreadLocal<Boolean>();

  //

  @Inject
  public TransactionalInterceptor(final JpaService service) {
    this.service = service;
  }

  //

  @Override
  public Object invoke(final MethodInvocation<Transactional> invocation) throws Throwable {
    if (!service.hasBegun()) {
      service.begin();
      shouldClose.set(true);
    }

    final EntityManager em = service.get();
    final EntityTransaction transaction = em.getTransaction();

    // If there is an active transaction, join.
    if (transaction.isActive()) {
      return invocation.proceed();
    }

    transaction.begin();

    final Object result;
    try {
      result = invocation.proceed();
    } catch (final Exception e) {
      final boolean rollback = doRollback(transaction, e, invocation.annotation().rollbackOn());
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
      final Class<? extends Exception>[] rollbackOn) {
    for (final Class<? extends Exception> rollbackException : rollbackOn) {
      if (rollbackException.isInstance(e)) {
        return true;
      }
    }
    return false;
  }

}
