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

import com.github.x3333.dagger.jpa.annotations.Transactional;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

import com.google.auto.common.MoreElements;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;

/**
 * @author Tercio Gaudencio Filho (terciofilho [at] gmail.com)
 */
public class MethodBind {

  public static Builder builder(final ExecutableElement method) {
    return new Builder(method);
  }

  public static class Builder {

    private final ExecutableElement method;
    private final ImmutableList.Builder<Class<? extends Annotation>> annotations;

    public Builder(final ExecutableElement method) {
      this.method = method;
      annotations = ImmutableList.builder();
    }

    public Builder annotation(final Class<? extends Annotation> annotation) {
      annotations.add(annotation);
      return this;
    }

    public MethodBind build() {
      return new MethodBind(method, annotations.build());
    }
  }

  //

  private final TypeElement classElement;
  private final ExecutableElement methodElement;
  private final ImmutableList<Class<? extends Annotation>> annotations;

  //

  private MethodBind(final ExecutableElement methodElement,
      final ImmutableList<Class<? extends Annotation>> annotations) {
    classElement = MoreElements.asType(Util.scanForElementKind(ElementKind.CLASS, methodElement));
    this.methodElement = methodElement;
    this.annotations = annotations;
  }

  //

  public TypeElement getClassElement() {
    return classElement;
  }

  public ExecutableElement getMethodElement() {
    return methodElement;
  }

  public ImmutableList<Class<? extends Annotation>> getAnnotations() {
    return annotations;
  }

  public static void main(final String[] args) {
    final List<Class<?>> as = new ArrayList<>();
    as.add(Transactional.class);
    as.add(Deprecated.class);
    System.out.println(as.stream().map(a -> a.getSimpleName()).collect(Collectors.joining(", ")));
  }

  //

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)//
        .add("class", classElement.getSimpleName())//
        .add("method", methodElement.toString())//
        .add("annotations", annotations.stream().map(a -> a.getSimpleName()).collect(Collectors.joining(", ")))
        .toString();
  }
}
