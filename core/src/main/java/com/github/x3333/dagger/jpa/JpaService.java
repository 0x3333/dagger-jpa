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

import javax.persistence.EntityManager;

/**
 * JPA Persistence service. Used to manage the overall startup and stop of the persistence module including manual control of
 * {@link EntityManager}.
 * 
 * <p>
 * In a non-transactional({@link Transactional @Transactional}) threads you must {@link #begin()} before requesting a {@link EntityManager}
 * and {@link #end()} after using it. Asking for a {@link EntityManager} before {@link #begin()} will thrown an exception.
 * 
 * <p>
 * Operations will always be binded to the local thread. Beginning/ending corresponds to opening and closing the thread's
 * {@code EntityManager}. Always {@link #end()} in a <code>finally</code> block.
 * 
 * @author Tercio Gaudencio Filho (terciofilho [at] gmail.com)
 */
public interface JpaService {

  /**
   * Starts the underlying persistence engine and makes JpaService ready for use. For instance, it creates an EntityManagerFactory and may
   * open connection pools. This method must be called by your code prior to using any JPA artifacts. If already started, calling this
   * method does nothing, if already stopped, it starts it again.
   */
  void start();

  /**
   * Tells if this JpaService is already started.
   * 
   * @return true if already started, false otherwise.
   */
  boolean hasStarted();

  /**
   * Stops the underlying persistence engine. For instance, it closes the {@code EntityManagerFactory}. If already stopped or not started
   * yet, calling this method does nothing.
   */
  void stop();

  /**
   * Provides an {@link EntityManager} instance.
   * 
   * @return Return newly created {@link EntityManager}.
   */
  EntityManager getEntityManager();

  /**
   * Begin EntityManager work. If already called, calling this method does nothing.
   */
  void begin();

  /**
   * End EntityManager work. If already called, calling this method does nothing.
   */
  void end();

  /**
   * Check if EntityManager has already begun.
   * 
   * @return boolean true if already begun, false otherwise.
   */
  boolean hasBegun();

}
