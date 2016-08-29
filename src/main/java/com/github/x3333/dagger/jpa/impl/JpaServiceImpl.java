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

package com.github.x3333.dagger.jpa.impl;

import static com.google.common.base.Preconditions.checkState;

import com.github.x3333.dagger.jpa.JpaService;

import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;

/**
 * Default implementation of {@link JpaService}.
 * 
 * @author Tercio Gaudencio Filho (terciofilho [at] gmail.com)
 */
public final class JpaServiceImpl implements JpaService {

  private final Logger logger = LoggerFactory.getLogger(JpaServiceImpl.class);

  private final String persistenceUnitName;
  private final Map<?, ?> persistenceProperties;

  private final Object emFactoryLock = new Object();
  private volatile EntityManagerFactory emFactory;
  private final ThreadLocal<EntityManager> entityManager = new ThreadLocal<EntityManager>();

  public JpaServiceImpl(final String persistenceUnitName, final Map<?, ?> persistenceProperties) {
    logger.trace("Creating");
    this.persistenceUnitName = persistenceUnitName;
    this.persistenceProperties = persistenceProperties;
  }

  @Override
  public void start() {
    logger.trace("Starting");
    synchronized (emFactoryLock) {
      if (emFactory != null) {
        return;
      }

      emFactory = Persistence.createEntityManagerFactory(persistenceUnitName, persistenceProperties);
    }
  }

  @Override
  public boolean isStarted() {
    return emFactory != null;
  }

  @Override
  public void stop() {
    logger.trace("Stopping");
    synchronized (emFactoryLock) {
      if (emFactory == null) {
        return;
      }

      // Should never occurs!
      checkState(emFactory.isOpen(), "Persistence service is already shut down!");

      try {
        emFactory.close();
      } finally {
        emFactory = null;
      }
    }
  }

  @Override
  public EntityManager get() {
    logger.trace("Get EntityManager");
    Preconditions.checkState(hasBegun(), "EntityManager requested, but work hasn't been initiated. "
        + "You should call JpaService.being() and JpaService.end(), or use Transactional method interceptor.");

    return entityManager.get();
  }

  @Override
  public void begin() {
    logger.trace("Begin work");
    if (entityManager.get() != null) {
      return;
    }

    entityManager.set(emFactory.createEntityManager());
  }

  @Override
  public void end() {
    logger.trace("End work");
    final EntityManager em = entityManager.get();
    if (em == null) {
      return;
    }

    try {
      em.close();
    } finally {
      entityManager.remove();
    }
  }

  @Override
  public boolean hasBegun() {
    return entityManager.get() != null;
  }

}
