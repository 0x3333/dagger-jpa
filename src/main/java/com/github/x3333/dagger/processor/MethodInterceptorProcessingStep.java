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

package com.github.x3333.dagger.processor;

import com.github.x3333.dagger.MethodInterceptorAnnotations;

import java.lang.annotation.Annotation;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.common.MoreTypes;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

public class MethodInterceptorProcessingStep implements BasicAnnotationProcessor.ProcessingStep {

  private final ProcessingEnvironment processingEnv;

  MethodInterceptorProcessingStep(final ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
    this.processingEnv.getClass();
  }

  @Override
  public Set<? extends Class<? extends Annotation>> annotations() {
    return ImmutableSet.of(MethodInterceptorAnnotations.class);
  }

  @Override
  public Set<Element> process(final SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {
    final Set<Element> notProcessed = Sets.newHashSet();

    for (final Entry<Class<? extends Annotation>, Element> element : elementsByAnnotation.entries()) {
      // final TypeElement componentElement = MoreElements.asType(element);
      System.out.println(MoreTypes.asDeclared(element.getValue().asType()));
    }

    return notProcessed;
  }

}
