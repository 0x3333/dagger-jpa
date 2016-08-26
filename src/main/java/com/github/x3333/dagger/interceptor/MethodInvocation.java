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

package com.github.x3333.dagger.interceptor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import com.google.common.collect.Iterables;

/**
 * Represents a Method Invocation.
 * 
 * <p>
 * A instance of this interface is provided to the Method Interceptor to proceed the invocation when necessary.
 * 
 * @author Tercio Gaudencio Filho (terciofilho [at] gmail.com)
 */
public interface MethodInvocation {

  Object getInstance();

  Method getMethod();

  Iterable<Annotation> getAnnotations();

  Object proceed() throws Throwable;

  /**
   * Return the Annotation instance of the Method based on it's Class.
   * 
   * @param <A> Type of the Annotation to be returned.
   * @param annotationClass Class of the Annotation to be returned.
   * @return Annotation instance if present, or null otherwise.
   */
  @SuppressWarnings("unchecked")
  default <A extends Annotation> A annotation(final Class<A> annotationClass) {
    return (A) Iterables.find(getAnnotations(), a -> a.getClass().equals(annotationClass), null);
  }

}
