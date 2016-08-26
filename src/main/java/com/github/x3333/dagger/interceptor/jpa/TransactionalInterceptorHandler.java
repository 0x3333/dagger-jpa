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

package com.github.x3333.dagger.interceptor.jpa;

import com.github.x3333.dagger.interceptor.processor.InterceptorHandler;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;

import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.google.common.base.MoreObjects;

/**
 * @author Tercio Gaudencio Filho (terciofilho [at] gmail.com)
 */
@AutoService(InterceptorHandler.class)
public class TransactionalInterceptorHandler implements InterceptorHandler {

  @Override
  public Class<? extends Annotation> annotation() {
    return Transactional.class;
  }

  @Override
  public Class<TransactionalInterceptor> methodInterceptorClass() {
    return TransactionalInterceptor.class;
  }

  @Override
  public String validateMethod(final ExecutableElement methodElement) {
    final ElementKind kind = methodElement.getKind();
    final Set<Modifier> modifiers = methodElement.getModifiers();

    // Is a method
    if (kind != ElementKind.METHOD) {
      return "@Transactional element must be a Method!";
    }
    // Is not private
    if (modifiers.contains(Modifier.PRIVATE)) {
      return "@Transactional methods cannot be Private!";
    }
    // Is not static
    if (modifiers.contains(Modifier.STATIC)) {
      return "@Transactional methods cannot be Static!";
    }

    // Is inside a OuterClass or a Static InnerClass.
    Element enclosingElement = methodElement.getEnclosingElement();
    while (enclosingElement != null && enclosingElement.getKind() != ElementKind.CLASS) {
      enclosingElement = enclosingElement.getEnclosingElement();
    }
    final TypeElement classElement = MoreElements.asType(enclosingElement);

    // Cannot be Local
    if (classElement.getNestingKind() == NestingKind.LOCAL) {
      return "@Transactional method's class cannot be Local!";
    }
    // If Inner Class, must be static
    if (classElement.getNestingKind() == NestingKind.MEMBER && !classElement.getModifiers().contains(Modifier.STATIC)) {
      return "@Transactional method's class must be Static if it's an Inner Class!";
    }
    // Cannot be Final
    if (classElement.getModifiers().contains(Modifier.FINAL)) {
      return "@Transactional method's class cannot be Final!";
    }
    // Must be Abstract
    // XXX: This is to avoid user instantiating it instead the generated version of the class
    if (!classElement.getModifiers().contains(Modifier.ABSTRACT)) {
      return "@Transactional method's class must be Abstract!";
    }
    // Must have one constructor or no constructor at all
    if (classElement.getEnclosedElements().stream()
        .filter(enclosedElement -> enclosedElement.getKind() == ElementKind.CONSTRUCTOR).count() > 1) {
      return "@Transactional method's class must have only one constructor!";
    }

    return null;
  }

  //

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this).add("annotation", annotation()).toString();
  }

}
