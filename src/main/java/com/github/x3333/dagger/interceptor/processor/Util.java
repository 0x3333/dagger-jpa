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

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.annotation.Annotation;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;

import com.google.auto.common.MoreTypes;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;

/**
 * Utilities for handling types in annotation processors.
 * 
 * @author Tercio Gaudencio Filho (terciofilho [at] gmail.com)
 */
// FIXME: Remove the visibility to outside
public final class Util {

  /**
   * Return the nearest Element of Kind <code>kind</code> in the enclosing elements.
   * 
   * @param kind Kind of the element to scan.
   * @param element Element that will be scanned.
   * @return Nearest Element.
   */
  public static Element scanForElementKind(final ElementKind kind, final Element element) {
    Element enclosingElement = element.getEnclosingElement();
    while (enclosingElement != null && enclosingElement.getKind() != kind) {
      enclosingElement = enclosingElement.getEnclosingElement();
    }
    return enclosingElement;
  }

  public static Iterable<AnnotationSpec> annotationMirrorToSpec(final Iterable<? extends AnnotationMirror> mirrors) {
    return Iterables.transform(mirrors, AnnotationSpec::get);
  }

  public static MethodSpec.Builder copyConstructor(final ExecutableElement el) {
    checkArgument(el.getKind() == ElementKind.CONSTRUCTOR);

    return MethodSpec.constructorBuilder() //
        .addModifiers(el.getModifiers()) //
        .addParameters(Lists.transform(el.getParameters(), parameter -> copyParameter(parameter).build())) //
        .addAnnotations(Util.annotationMirrorToSpec(el.getAnnotationMirrors())) //
        .addStatement("super($L)", //
            Joiner.on(", ") //
                .join(Lists.transform(el.getParameters(), //
                    (final VariableElement variable) -> variable.getSimpleName().toString())));
  }

  public static MethodSpec.Builder copyMethod(final ExecutableElement el,
      final Class<? extends Annotation> targetAnnotation) {
    final Iterable<AnnotationSpec> annotations = Util.annotationMirrorToSpec(//
        Iterables.filter(//
            el.getAnnotationMirrors(), //
            mirror -> !MoreTypes.isTypeOf(targetAnnotation, mirror.getAnnotationType())));

    final MethodSpec.Builder method = MethodSpec.methodBuilder(el.getSimpleName().toString()) //
        .addModifiers(el.getModifiers()) //
        .addParameters(Lists.transform(el.getParameters(), parameter -> copyParameter(parameter).build())) //
        .addExceptions(Lists.transform(el.getThrownTypes(), typeMirror -> TypeName.get(typeMirror))) //
        .addAnnotations(annotations);
    if (el.getReturnType().getKind() != TypeKind.VOID) {
      method.returns(TypeName.get(el.getReturnType()));
    }
    return method;
  }

  public static ParameterSpec.Builder copyParameter(final VariableElement element) {
    final Modifier[] modifiers = element.getModifiers().toArray(new Modifier[element.getModifiers().size()]);
    return ParameterSpec
        .builder(//
            TypeName.get(element.asType()), //
            element.getSimpleName().toString(), //
            modifiers) //
        .addAnnotations(Util.annotationMirrorToSpec(element.getAnnotationMirrors())); //
  }

}
