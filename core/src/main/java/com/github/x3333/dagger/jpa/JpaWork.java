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
 * This interface is used to gain manual control over the JPA unit of work. This is mostly to do work in
 * non-transactional({@link Transactional @Transactional}) threads.
 * 
 * <p>
 * In a non-transactional threads you must {@link #begin()} before requesting an {@link EntityManager} and {@link #end()} after using it.
 * Asking for an {@link EntityManager} before {@link #begin()} will thrown an exception.
 * 
 * <p>
 * Operations will always be binded to the local thread. Beginning/ending corresponds to opening and closing the thread's
 * {@code EntityManager}. Always {@link #end()} in a <code>finally</code> block.
 * 
 * @author Tercio Gaudencio Filho (terciofilho [at] gmail.com)
 */
public interface JpaWork {

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
