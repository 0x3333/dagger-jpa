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

import com.github.x3333.dagger.jpa.impl.JpaServiceImpl;

import java.util.Map;

import javax.inject.Singleton;
import javax.persistence.EntityManager;

import dagger.Module;
import dagger.Provides;

@Module
public class JpaModule {

  protected final String persistenceUnitName;
  protected final Map<?, ?> persistenceProperties;

  //

  public JpaModule(final String persistenceUnitName) {
    this.persistenceUnitName = persistenceUnitName;
    persistenceProperties = null;
  }

  public JpaModule(final String persistenceUnitName, final Map<?, ?> persistenceProperties) {
    this.persistenceUnitName = persistenceUnitName;
    this.persistenceProperties = persistenceProperties;
  }

  //

  @Provides
  @Singleton
  JpaService providesJpaService() {
    return new JpaServiceImpl(persistenceUnitName, persistenceProperties);
  }

  @Provides
  EntityManager providesEntityManager(final JpaService service) {
    return service.get();
  }

}
