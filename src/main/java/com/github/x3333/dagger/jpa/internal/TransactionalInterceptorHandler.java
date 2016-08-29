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

package com.github.x3333.dagger.jpa.internal;

import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.PUBLIC;

import com.github.x3333.dagger.aop.InterceptorHandler;
import com.github.x3333.dagger.aop.internal.InterceptorProcessor;
import com.github.x3333.dagger.jpa.JpaService;
import com.github.x3333.dagger.jpa.Transactional;
import com.github.x3333.dagger.jpa.TransactionalInterceptor;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.persistence.EntityManager;
import javax.tools.Diagnostic;

import com.google.auto.common.MoreElements;
import com.google.auto.service.AutoService;
import com.google.common.base.MoreObjects;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.WildcardTypeName;

import dagger.Module;
import dagger.Provides;

/**
 * @author Tercio Gaudencio Filho (terciofilho [at] gmail.com)
 */
@AutoService(InterceptorHandler.class)
public class TransactionalInterceptorHandler implements InterceptorHandler {

  private static final String DAGGER_JPA_PACKAGE = JpaService.class.getPackage().getName();
  private static final String INTERNAL_PACKAGE = ".internal";

  private static final String JPA_MODULE_NAME = "JpaModule";
  private static final String TRANSACTIONAL_INTERCEPTOR_IMPL_NAME = "TransactionalInterceptorImpl";

  @Override
  public Class<? extends Annotation> annotation() {
    return Transactional.class;
  }

  @Override
  public Class<TransactionalInterceptor> methodInterceptorClass() {
    return TransactionalInterceptor.class;
  }

  //

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

  @Override
  public void postProcess(final ProcessingEnvironment processingEnv, final Set<TypeElement> processedClasses) {
    generateJpaModule(processingEnv);
    generateTransactionalInterceptorImpl(processingEnv);
  }

  private void generateJpaModule(final ProcessingEnvironment processingEnv) {
    final TypeName unitNameType = TypeName.get(String.class);
    final TypeName propertiesType = ParameterizedTypeName.get(//
        ClassName.get(Map.class), //
        WildcardTypeName.subtypeOf(Object.class), //
        WildcardTypeName.subtypeOf(Object.class));
    final TypeName jpaServiceType = TypeName.get(JpaService.class);
    final ClassName transactionalInterceptorImplType = ClassName.get(//
        DAGGER_JPA_PACKAGE + INTERNAL_PACKAGE, //
        TRANSACTIONAL_INTERCEPTOR_IMPL_NAME);

    final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(JPA_MODULE_NAME) //
        .addModifiers(PUBLIC, FINAL)//
        .addJavadoc("This class is the default Dagger module for JPA.\n\n" + "<p>\n"
            + "This has been created in your project so Dagger can generate \n"
            + "Factory classes in your project, freeing us from distributing generated code.\n")//
        .addAnnotation(InterceptorProcessor.generatedAnnotation(TransactionalInterceptorHandler.class))//
        .addAnnotation(Module.class)//
        .addField(unitNameType, "persistenceUnitName", PRIVATE, FINAL)//
        .addField(propertiesType, "persistenceProperties", PRIVATE, FINAL);

    final MethodSpec constructor1 = MethodSpec.constructorBuilder()//
        .addModifiers(PUBLIC)//
        .addParameter(unitNameType, "persistenceUnitName", FINAL)//
        .addCode(CodeBlock.builder().addStatement("this.persistenceUnitName = persistenceUnitName").build())//
        .addCode(CodeBlock.builder().addStatement("this.persistenceProperties = null").build())//
        .build();

    final MethodSpec constructor2 = MethodSpec.constructorBuilder()//
        .addModifiers(PUBLIC)//
        .addParameter(unitNameType, "persistenceUnitName", FINAL)//
        .addParameter(propertiesType, "persistenceProperties", FINAL)//
        .addCode(CodeBlock.builder().addStatement("this.persistenceUnitName = persistenceUnitName").build())//
        .addCode(CodeBlock.builder().addStatement("this.persistenceProperties = persistenceProperties").build())//
        .build();

    final MethodSpec jpaServiceMethod = MethodSpec.methodBuilder("providesJpaService")//
        .addAnnotation(Provides.class)//
        .addAnnotation(Singleton.class)//
        .returns(jpaServiceType)//
        .addCode("$[return new $T(persistenceUnitName, persistenceProperties);\n$]", JpaServiceImpl.class)//
        .build();

    final MethodSpec entityManagerMethod = MethodSpec.methodBuilder("providesEntityManager")//
        .addAnnotation(Provides.class)//
        .returns(TypeName.get(EntityManager.class))//
        .addParameter(jpaServiceType, "jpaService", FINAL)//
        .addCode("$[return jpaService.get();\n$]", JpaServiceImpl.class)//
        .build();

    final MethodSpec transactionalInterceptorMethod = MethodSpec.methodBuilder("providesTransactionalInterceptor")//
        .addAnnotation(Provides.class)//
        .returns(TypeName.get(TransactionalInterceptor.class))//
        .addParameter(jpaServiceType, "jpaService", FINAL)//
        .addCode("$[return new $T(jpaService);\n$]", transactionalInterceptorImplType)//
        .build();

    classBuilder//
        .addMethod(constructor1)//
        .addMethod(constructor2)//
        .addMethod(jpaServiceMethod)//
        .addMethod(entityManagerMethod)//
        .addMethod(transactionalInterceptorMethod);

    writeClass(processingEnv, DAGGER_JPA_PACKAGE, JPA_MODULE_NAME, classBuilder);
  }

  private void generateTransactionalInterceptorImpl(final ProcessingEnvironment processingEnv) {
    final TypeName jpaServiceType = TypeName.get(JpaService.class);

    final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(TRANSACTIONAL_INTERCEPTOR_IMPL_NAME) //
        .superclass(ClassName.get(DAGGER_JPA_PACKAGE + INTERNAL_PACKAGE, "TransactionalInterceptorInternal"))
        .addModifiers(PUBLIC, FINAL)//
        .addJavadoc("This class is used as the default 'implementation' for TransactionalInterceptor.\n\n" + "<p>\n"
            + "This has been created in your project so Dagger can generate \n"
            + "Factory classes in your project, freeing us from distributing generated code.\n")//
        .addAnnotation(InterceptorProcessor.generatedAnnotation(TransactionalInterceptorHandler.class));

    final MethodSpec constructor = MethodSpec.constructorBuilder()//
        .addModifiers(PUBLIC)//
        .addAnnotation(Inject.class)//
        .addParameter(jpaServiceType, "jpaService", FINAL)//
        .addCode("$[super(jpaService);\n$]")//
        .build();

    classBuilder.addMethod(constructor);

    writeClass(processingEnv, DAGGER_JPA_PACKAGE + INTERNAL_PACKAGE, TRANSACTIONAL_INTERCEPTOR_IMPL_NAME, classBuilder);
  }

  private void writeClass(final ProcessingEnvironment processingEnv, final String packageName, final String moduleName,
      final TypeSpec.Builder classBuilder) {
    try {
      JavaFile.builder(packageName, classBuilder.build()).build().writeTo(processingEnv.getFiler());
    } catch (final IOException ioe) {
      final StringWriter sw = new StringWriter();
      try (final PrintWriter pw = new PrintWriter(sw);) {
        pw.println("Error generating source file for type " + moduleName);
        ioe.printStackTrace(pw);
        pw.close();
        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, sw.toString());
      }
    }
  }

  //

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)//
        .add("methodInterceptor", methodInterceptorClass())//
        .add("annotation", annotation()).toString();
  }

}