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

package com.github.x3333.dagger.interceptor.processor;

import com.github.x3333.dagger.interceptor.MethodInterceptor;

import java.lang.annotation.Annotation;

import javax.lang.model.element.ExecutableElement;

/**
 * A Marker interface for Method Interceptor.
 * 
 * <p>
 * MethodInterceptors must be a Java Service.
 * 
 * @author Tercio Gaudencio Filho (terciofilho [at] gmail.com)
 */
public interface InterceptorService {

  /**
   * The {@link Annotation} class that this service is binded to.
   */
  Class<? extends Annotation> annotation();

  /**
   * The {@link MethodInterceptor} class that this service is binded to.
   */
  Class<? extends MethodInterceptor> methodInterceptor();

  /**
   * Validate a Method Element to be accepted by the Processor.
   * 
   * @return <code>Null</code> if this element has passed validation, otherwise, a String with the error message.
   */
  default String validateMethod(final ExecutableElement methodElement) {
    return null;
  }

}
