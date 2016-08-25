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

import com.github.x3333.dagger.interceptor.MethodInterceptor;
import com.github.x3333.dagger.interceptor.MethodInvocation;
import com.github.x3333.dagger.jpa.annotations.Transactional;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.annotation.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.NestingKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.common.MoreElements;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.SetMultimap;
import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

/**
 * @author Tercio Gaudencio Filho (terciofilho [at] gmail.com)
 */
// FIXME: Cleanup code, it's a mess.
// FIXME: Generalize this class, so we can create multiple method interceptors binded by some annotation.
// We could find Annotations that are annotated by @MethodInterceptor or something and the Dagger module binds the
// MethodInterceptor<Annotation> to some instance of this interceptor, the way TransactionalInterceptor is binded.
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
      final TypeElement classElement = validateElement(element);
      if (classElement != null) {
        binds.put(classElement, MoreElements.asExecutable(element));
      }
    }

    generateTransactionals(binds);

    return Collections.<Element>emptySet();
  }

  private void generateTransactionals(final Multimap<TypeElement, ExecutableElement> binds) {
    for (final TypeElement typeElement : binds.keySet()) {
      // New Class
      final ClassName elementName = ClassName.get(typeElement);
      final ParameterizedTypeName interceptorType =
          ParameterizedTypeName.get(MethodInterceptor.class, Transactional.class);
      final String name = "Transactional_" + Joiner.on("_").join(elementName.simpleNames());
      final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(name) //
          .addOriginatingElement(typeElement) //
          .addAnnotation(AnnotationSpec.builder(Generated.class) //
              .addMember("value", "$S", this.getClass().getCanonicalName()).build()) //
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL) //
          .addField(interceptorType, "interceptor", Modifier.PRIVATE, Modifier.FINAL) //
          .superclass(ClassName.get(typeElement)) //
          .addAnnotations(Util.annotationMirrorToSpec(typeElement.getAnnotationMirrors()));

      // Constructor
      ExecutableElement constructorElement = null;
      for (final Element element : typeElement.getEnclosedElements()) {
        if (element.getKind() == ElementKind.CONSTRUCTOR) {
          if (constructorElement != null) {
            printError(typeElement, "@Transactional classes must have only one constructor!");
            continue;
          }
          constructorElement = MoreElements.asExecutable(element);
        }
      }
      final MethodSpec.Builder copyConstructor = constructorElement == null ? //
          MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
          : //
          Util.copyConstructor(constructorElement) //
              .addAnnotation(Inject.class) //
              .addParameter(interceptorType, //
                  "interceptor", Modifier.FINAL) //
              .addStatement("this.interceptor = interceptor");

      // Start Annotation Cache statements.
      copyConstructor.addCode("try {\n" //
          + "  $T method;\n", Method.class);

      // Methods
      for (final ExecutableElement methodElement : binds.get(typeElement)) {
        final MethodSpec.Builder newMethod =
            Util.copyMethod(MoreElements.asExecutable(methodElement), Transactional.class);
        final String methodName = methodElement.getSimpleName().toString();

        if (methodElement.getReturnType().getKind() == TypeKind.VOID) {
          final ParameterizedTypeName methodInvocationClass = ParameterizedTypeName//
              .get(ClassName.get(MethodInvocation.class), //
                  ClassName.get(Transactional.class) //
          );

          newMethod.addCode(
              "try {\n" //
                  + "  interceptor.invoke(new $T() {\n" //
                  + "    @Override\n" //
                  + "    public Object proceed() {\n" //
                  + "      $L.super.$L($L);\n" //
                  + "      return null;\n" //
                  + "    }\n" //
                  + "\n" //
                  + "    @Override\n" //
                  + "    public Transactional annotation() {\n" //
                  + "      return $L.this.$LAnnotation;\n" //
                  + "    }\n" //
                  + "\n" //
                  + "  });\n" //
                  + "} catch (Throwable e) {\n" //
                  + "  throw new RuntimeException(e);\n" //
                  + "}\n", //
              methodInvocationClass, name, methodName,
              Joiner.on(", ") //
                  .join(Lists.transform(methodElement.getParameters(), //
                      variable -> variable.getSimpleName().toString())),
              name, methodName);
        } else {
          final TypeName returnTypeName = TypeName.get(methodElement.getReturnType());
          final ParameterizedTypeName methodInvocationClass = ParameterizedTypeName.get(//
              ClassName.get(MethodInvocation.class), //
              ClassName.get(Transactional.class));

          newMethod.addCode(
              "try {\n" //
                  + "  return ($T) interceptor.invoke(new $T() {\n" //
                  + "    @Override\n" //
                  + "    public Object proceed() {\n" //
                  + "      return $L.super.$L($L);\n" //
                  + "    }\n" //
                  + "\n" //
                  + "    @Override\n" //
                  + "    public Transactional annotation() {\n" //
                  + "      return $L.this.$LAnnotation;\n" //
                  + "    }\n" //
                  + "\n" //
                  + "  });\n" //
                  + "} catch (Throwable e) {\n" //
                  + "  throw new RuntimeException(e);\n" //
                  + "}\n", //
              returnTypeName, methodInvocationClass, name, methodName,
              Joiner.on(", ") //
                  .join(Lists.transform(methodElement.getParameters(), //
                      variable -> variable.getSimpleName().toString())),
              name, methodName);
        }
        classBuilder.addMethod(newMethod.build());

        classBuilder.addField(//
            ClassName.get(Transactional.class), //
            methodName + "Annotation", //
            Modifier.PRIVATE, //
            Modifier.FINAL);

        final List<TypeName> parametersTypes =
            Lists.transform(methodElement.getParameters(), parameter -> TypeName.get(parameter.asType()));

        final CodeBlock parameters = parametersTypes.size() == 0 ? //
            CodeBlock.of("") //
            : CodeBlock.builder().add(//
                Strings.repeat(", $T.class", parametersTypes.size()), parametersTypes.toArray()).build();
        copyConstructor.addCode( //
            "  method = super.getClass().getMethod($S$L);\n" //
                + "  this.$LAnnotation = method.getAnnotation(Transactional.class);\n", //
            methodName, parameters, methodName);
      }
      copyConstructor.addCode("} catch (NoSuchMethodException | SecurityException e) {\n" //
          + "  throw new RuntimeException(e);\n" //
          + "}\n");

      // Add constructor at the end so methods can create the annotation cache.
      classBuilder.addMethod(copyConstructor.build());

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

  /**
   * Print an Error message to Processing Environment.
   * 
   * @param element Element that generated the message.
   * @param message Message to be printed.
   */
  private void printError(final Element element, final String message) {
    processingEnv.getMessager().printMessage(Kind.ERROR, message, element);
  }

  /**
   * Validate an Element prior generation step.
   * 
   * @param element Element to be validated.
   * @return TypeElement of the declaring class.
   */
  private TypeElement validateElement(final Element element) {
    final ElementKind kind = element.getKind();
    final Set<Modifier> modifiers = element.getModifiers();

    // Is a method
    if (kind != ElementKind.METHOD) {
      printError(element, "@Transactional element must be a Method!");
      return null;
    }
    // Is not private
    if (modifiers.contains(Modifier.PRIVATE)) {
      printError(element, "@Transactional methods cannot be Private!");
      return null;
    }
    // Is not static
    if (modifiers.contains(Modifier.STATIC)) {
      printError(element, "@Transactional methods cannot be Static!");
      return null;
    }

    // Is inside a OuterClass or a Static InnerClass.
    final TypeElement typeElement = MoreElements.asType(Util.scanForElementKind(ElementKind.CLASS, element));

    // Cannot be Local
    if (typeElement.getNestingKind() == NestingKind.LOCAL) {
      printError(typeElement, "@Transactional method's class cannot be Local!");
      return null;
    }
    // If Inner Class, must be static
    if (typeElement.getNestingKind() == NestingKind.MEMBER && !typeElement.getModifiers().contains(Modifier.STATIC)) {
      printError(typeElement, "@Transactional method's class must be Static if it's an Inner Class!");
      return null;
    }
    // Cannot be Final
    if (typeElement.getModifiers().contains(Modifier.FINAL)) {
      printError(typeElement, "@Transactional method's class cannot be Final!");
      return null;
    }
    // Must be Abstract
    // XXX: This is to avoid user instantiating it instead the generated version of the class
    if (!typeElement.getModifiers().contains(Modifier.ABSTRACT)) {
      printError(typeElement, "@Transactional method's class must be Abstract!");
      return null;
    }
    // Must have one constructor or no constructor at all
    if (typeElement.getEnclosedElements().stream()
        .filter(enclosedElement -> enclosedElement.getKind() == ElementKind.CONSTRUCTOR).count() > 1) {
      printError(typeElement, "@Transactional method's class must have only one constructor!");
      return null;
    }

    return typeElement;
  }

}
