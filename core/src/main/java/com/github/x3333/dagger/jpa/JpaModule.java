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

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.persistence.EntityManager;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

/**
 * Default JPA Module to be used in Dagger Component.
 * 
 * @author Tercio Gaudencio Filho (terciofilho [at] gmail.com)
 */
@Module
public abstract class JpaModule {

  @Provides
  @Singleton
  public static JpaServiceImpl providesJpaServiceImpl(@Named("jpa.unitname") final String persistenceUnitName,
      @Nullable @Named("jpa.properties") final Map<?, ?> persistenceProperties) {
    return new JpaServiceImpl(persistenceUnitName, persistenceProperties);
  }

  @Binds
  abstract JpaService providesJpaService(final JpaServiceImpl impl);

  @Binds
  abstract JpaWork providesJpaWork(final JpaServiceImpl impl);

  @Provides
  public static EntityManager providesEntityManager(final JpaWork jpaWork) {
    return jpaWork.getEntityManager();
  }

}
