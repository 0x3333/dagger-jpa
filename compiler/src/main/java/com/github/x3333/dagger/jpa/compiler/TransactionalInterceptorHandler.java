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

package com.github.x3333.dagger.jpa.compiler;

import static javax.lang.model.element.Modifier.ABSTRACT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PUBLIC;
import static javax.lang.model.element.Modifier.STATIC;

import com.github.x3333.dagger.aop.InterceptorHandler;
import com.github.x3333.dagger.aop.Sources;
import com.github.x3333.dagger.jpa.JpaService;
import com.github.x3333.dagger.jpa.Transactional;
import com.github.x3333.dagger.jpa.TransactionalInterceptor;
import com.github.x3333.dagger.jpa.impl.JpaServiceImpl;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Singleton;
import javax.lang.model.element.TypeElement;

import com.google.auto.service.AutoService;
import com.google.common.base.MoreObjects;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;

/**
 * @author Tercio Gaudencio Filho (terciofilho [at] gmail.com)
 */
@AutoService(InterceptorHandler.class)
public class TransactionalInterceptorHandler implements InterceptorHandler {

  private static final String DAGGER_JPA_PACKAGE = JpaService.class.getPackage().getName();
  private static final String JPA_MODULE_NAME = "JpaModule";

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
  public void postProcess(final ProcessingEnvironment processingEnv, final Set<TypeElement> processedClasses) {
    generateJpaModule(processingEnv);
  }

  private void generateJpaModule(final ProcessingEnvironment processingEnv) {
    final TypeName jpaServiceType = TypeName.get(JpaService.class);
    final TypeName jpaServiceImplType = TypeName.get(JpaServiceImpl.class);

    final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(JPA_MODULE_NAME) //
        .addModifiers(PUBLIC, ABSTRACT)//
        .addJavadoc("This class is the default Dagger module for JPA.\n\n" + "<p>\n"
            + "This has been created in your project so Dagger can generate \n"
            + "Factory classes in your project, freeing us from distributing generated code.\n")//
        .addAnnotation(Sources.generatedAnnotation(TransactionalInterceptorHandler.class))//
        .addAnnotation(Module.class);

    final MethodSpec jpaServiceMethod = MethodSpec.methodBuilder("providesJpaService")//
        .addModifiers(ABSTRACT)//
        .addAnnotation(Binds.class)//
        .addAnnotation(Singleton.class)//
        .returns(jpaServiceType)//
        .addParameter(jpaServiceImplType, "impl", FINAL)//
        .build();

    final MethodSpec entityManagerMethod = MethodSpec.methodBuilder("providesEntityManager")//
        .addModifiers(STATIC)//
        .addAnnotation(Provides.class)//
        .returns(ClassName.get("javax.persistence", "EntityManager"))//
        .addParameter(jpaServiceType, "jpaService", FINAL)//
        .addCode("$[return jpaService.get();\n$]", JpaServiceImpl.class)//
        .build();

    classBuilder//
        .addMethod(jpaServiceMethod)//
        .addMethod(entityManagerMethod);//

    Sources.writeClass(//
        processingEnv, //
        DAGGER_JPA_PACKAGE, //
        classBuilder.build());
  }

  //

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)//
        .add("annotation", annotation())//
        .add("methodInterceptor", methodInterceptorClass()).toString();
  }

}
