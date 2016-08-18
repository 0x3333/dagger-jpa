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

import static com.google.common.base.Preconditions.checkArgument;

import com.github.x3333.dagger.MethodInvocation;
import com.github.x3333.dagger.jpa.TransactionalInterceptor;
import com.github.x3333.dagger.jpa.annotations.Transactional;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Set;

import javax.annotation.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.common.MoreElements;
import com.google.common.base.Joiner;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

final class TransactionalProcessingStep implements BasicAnnotationProcessor.ProcessingStep {

  private final ProcessingEnvironment processingEnv;

  TransactionalProcessingStep(final ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;
  }

  @Override
  public Set<? extends Class<? extends Annotation>> annotations() {
    return ImmutableSet.of(Transactional.class);
  }

  @Override
  public Set<Element> process(final SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {
    final Multimap<TypeElement, ExecutableElement> binds = ArrayListMultimap.create();

    for (final Element element : elementsByAnnotation.get(Transactional.class)) {
      if (element.getKind() != ElementKind.METHOD) {
        printError(element, "@Transactional element must be a Method!");
        continue;
      }
      if (element.getModifiers().contains(Modifier.PRIVATE)) {
        printError(element, "@Transactional methods cannot be Private!");
        continue;
      }
      final TypeElement typeElement = MoreElements.asType(element.getEnclosingElement());
      if (typeElement.getModifiers().contains(Modifier.FINAL)) {
        printError(element, "@Transactional method's class cannot be Final!");
        continue;
      }
      if (!typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
        printError(element, "@Transactional method's class must be Abstract!");
        continue;
      }
      binds.put(typeElement, MoreElements.asExecutable(element));
    }

    generateTransactionals(binds);

    return Collections.<Element>emptySet();
  }

  private void generateTransactionals(final Multimap<TypeElement, ExecutableElement> binds) {
    for (final TypeElement typeElement : binds.keySet()) {
      // New Class
      final ClassName elementName = ClassName.get(typeElement);
      final String name = "Transactional_" + Joiner.on("_").join(elementName.simpleNames());
      final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(name)//
          .addOriginatingElement(typeElement)//
          .addAnnotation(AnnotationSpec.builder(Generated.class)//
              .addMember("value", "$S", TransactionalProcessor.class.getCanonicalName()).build())//
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)//
          .addField(ClassName.get(TransactionalInterceptor.class), "interceptor", Modifier.PRIVATE, Modifier.FINAL)//
          .superclass(ClassName.get(typeElement))//
          .addAnnotations(Lists.transform(typeElement.getAnnotationMirrors(), AnnotationSpec::get));

      // Constructors
      int constructorsCount = 0;
      int injectableConstructorsCount = 0;
      for (final Element element : typeElement.getEnclosedElements()) {
        if (element.getKind() == ElementKind.CONSTRUCTOR) {
          final MethodSpec.Builder copyConstructor = copyConstructor(MoreElements.asExecutable(element));
          constructorsCount++;
          if (element.getAnnotationsByType(Inject.class).length > 0) {
            injectableConstructorsCount++;
          }
          copyConstructor.addParameter(ClassName.get(TransactionalInterceptor.class), "interceptor", Modifier.FINAL)//
              .addStatement("this.interceptor = interceptor");
          classBuilder.addMethod(copyConstructor.build());
        }
      }
      if (constructorsCount > 0 && (injectableConstructorsCount == 0 || injectableConstructorsCount > 1)) {
        printError(typeElement,
            "@Transactional classes must have one constructor annotated with @Inject or no constructor");
      }

      // Methods
      for (final ExecutableElement methodElement : binds.get(typeElement)) {
        final MethodSpec.Builder newMethod = copyMethod(MoreElements.asExecutable(methodElement));
        if (methodElement.getReturnType().getKind() == TypeKind.VOID) {
          newMethod.addCode(
              "try {\n" //
                  + "  interceptor.invoke(new $T() {\n" //
                  + "    @Override\n" //
                  + "    public Object proceed() {\n" //
                  + "      $L.super.$L();\n" //
                  + "      return null;\n" //
                  + "    }\n" //
                  + "  });\n" //
                  + "} catch (Throwable e) {\n" //
                  + "  throw new RuntimeException(e);\n" //
                  + "}\n", //
              MethodInvocation.class, name, methodElement.getSimpleName().toString());
        } else {
          newMethod.addCode(
              "try {\n" //
                  + "  return ($T) interceptor.invoke(new $T() {\n" //
                  + "    @Override\n" //
                  + "    public Object proceed() {\n" //
                  + "      return $L.super.$L();\n" //
                  + "    }\n" //
                  + "  });\n" //
                  + "} catch (Throwable e) {\n" //
                  + "  throw new RuntimeException(e);\n" //
                  + "}\n", //
              TypeName.get(methodElement.getReturnType()), MethodInvocation.class, name,
              methodElement.getSimpleName().toString());
        }
        classBuilder.addMethod(newMethod.build());
      }

      try {
        JavaFile.builder(elementName.packageName(), classBuilder.build()).build().writeTo(processingEnv.getFiler());
      } catch (final IOException ioe) {
        final StringWriter sw = new StringWriter();
        final PrintWriter pw = new PrintWriter(sw);
        pw.println("Error generating source file for type " + classBuilder.build().name);
        ioe.printStackTrace(pw);
        pw.close();
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, sw.toString());
      }
    }
  }

  private MethodSpec.Builder copyConstructor(final ExecutableElement el) {
    checkArgument(el.getKind() == ElementKind.CONSTRUCTOR);

    return MethodSpec.constructorBuilder()//
        .addModifiers(el.getModifiers())//
        .addParameters(Lists.transform(el.getParameters(), this::copyParameter))//
        .addAnnotations(Lists.transform(el.getAnnotationMirrors(), AnnotationSpec::get))//
        .addStatement("super($L)", //
            Joiner.on(", ")//
                .join(Lists.transform(el.getParameters(), //
                    (final VariableElement variable) -> variable.getSimpleName().toString())));
  }

  private MethodSpec.Builder copyMethod(final ExecutableElement el) {
    final MethodSpec.Builder method = MethodSpec.methodBuilder(el.getSimpleName().toString())//
        .addModifiers(el.getModifiers())//
        .addParameters(Lists.transform(el.getParameters(), this::copyParameter))//
        .addAnnotations(Lists.newArrayList(el.getAnnotationMirrors().stream()
            .filter(annotationMirror -> !annotationMirror.getAnnotationType().toString()
                .equals(Transactional.class.getName()))
            .map(annotationMirror -> AnnotationSpec.get(annotationMirror)).iterator()));
    if (el.getReturnType().getKind() != TypeKind.VOID) {
      method.returns(TypeName.get(el.getReturnType()));
    }
    return method;
  }

  private ParameterSpec copyParameter(final VariableElement element) {
    final Set<Modifier> modifiers = element.getModifiers();
    return ParameterSpec
        .builder(//
            TypeName.get(element.asType()), //
            element.getSimpleName().toString(), //
            modifiers.toArray(new Modifier[modifiers.size()]))//
        .addAnnotations(Lists.transform(element.getAnnotationMirrors(), AnnotationSpec::get)).build();
  }

  private void printError(final Element element, final String message) {
    processingEnv.getMessager().printMessage(Kind.ERROR, message, element);
  }

}
