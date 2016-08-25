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

package com.github.x3333.dagger.interceptor.service;

import com.github.x3333.dagger.interceptor.MethodInterceptor;
import com.github.x3333.dagger.interceptor.processor.InterceptorService;
import com.github.x3333.dagger.jpa.LogInterceptor;
import com.github.x3333.dagger.jpa.annotations.Log;

import java.lang.annotation.Annotation;

import com.google.auto.service.AutoService;
import com.google.common.base.MoreObjects;

/**
 * @author Tercio Gaudencio Filho (terciofilho [at] gmail.com)
 */
@AutoService(InterceptorService.class)
public class LogInterceptorService implements InterceptorService {

  @Override
  public Class<? extends Annotation> annotation() {
    return Log.class;
  }

  @Override
  public Class<? extends MethodInterceptor> methodInterceptor() {
    return LogInterceptor.class;
  }

  //

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("annotation", annotation()).toString();
  }

}
