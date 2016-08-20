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

import com.github.x3333.dagger.jpa.annotations.Transactional;

import javax.persistence.EntityManager;

/**
 * Used for manual control over the {@link EntityManager}.
 * 
 * <p>
 * This is to be used mostly in a non-transactional({@link Transactional @Transactional}) threads.
 * 
 * <p>
 * The operations will always be binded to the local thread. Beginning/ending a JpaWork corresponds to opening and closing the thread's
 * {@code EntityManager}. Always {@link #end()} in a <code>finally</code> block.
 * 
 * @author Tercio Gaudencio Filho (terciofilho [at] gmail.com)
 */
public interface JpaWork {

  /**
   * Begin EntityManager work.
   */
  void begin();

  /**
   * End EntityManager work.
   */
  void end();

  /**
   * Check if EntityManager has already begun.
   * 
   * @return boolean true if already begun, false otherwise.
   */
  boolean hasBegun();

}
