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
import com.github.x3333.dagger.interceptor.MethodInvocation;
import com.github.x3333.dagger.jpa.annotations.Transactional;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;

import javax.annotation.Generated;
import javax.annotation.processing.ProcessingEnvironment;
import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;

import com.google.auto.common.BasicAnnotationProcessor;
import com.google.auto.common.MoreElements;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
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
 *
 */
public class InterceptorProcessorStep implements BasicAnnotationProcessor.ProcessingStep {

  private final ProcessingEnvironment processingEnv;
  private final ImmutableMap<Class<? extends Annotation>, InterceptorService> services;

  //

  /**
   * Create a InterceptorProcessorStep instance.
   */
  public InterceptorProcessorStep(final ProcessingEnvironment processingEnv) {
    this.processingEnv = processingEnv;

    final Builder<Class<? extends Annotation>, InterceptorService> builder = ImmutableMap.builder();
    ServiceLoader//
        .load(InterceptorService.class, this.getClass().getClassLoader())//
        .forEach(service -> builder.put(service.annotation(), service));

    services = builder.build();
  }

  //

  @Override
  public Set<? extends Class<? extends Annotation>> annotations() {
    return services.keySet();
  }

  @Override
  public Set<Element> process(final SetMultimap<Class<? extends Annotation>, Element> elementsByAnnotation) {
    final Map<ExecutableElement, MethodBind.Builder> builders = new HashMap<>();
    for (final Class<? extends Annotation> annotation : services.keySet()) {
      final InterceptorService service = services.get(annotation);

      // Group by Method
      for (final Element element : elementsByAnnotation.get(annotation)) {
        if (element.getKind() != ElementKind.METHOD) {
          printWarning(element, "Ignoring element, not a Method!");
          continue;
        }
        final ExecutableElement methodElement = MoreElements.asExecutable(element);

        if (MoreElements.isAnnotationPresent(
            MoreElements.asType(Util.scanForElementKind(ElementKind.CLASS, methodElement)), Generated.class)) {
          printWarning(element, "Ignoring element, Generated code!");
          continue;
        }

        final String errorMessage = service.validateMethod(methodElement);
        if (errorMessage != null) {
          printError(methodElement, errorMessage);
          continue;
        }
        builders
            .computeIfAbsent(//
                methodElement, //
                key -> MethodBind.builder(methodElement))//
            .annotation(annotation);
      }
    }

    // Group by Class
    final Multimap<TypeElement, MethodBind> classes = ArrayListMultimap.create();
    builders.values().forEach(b -> {
      final MethodBind bind = b.build();
      classes.put(bind.getClassElement(), bind);
    });

    // Process binds by grouped Class
    classes.keySet().forEach(k -> processBind(k, classes.get(k)));

    return Collections.emptySet();
  }

  //

  private void processBind(final TypeElement superClassElement, final Collection<MethodBind> methodBinds) {
    // New Class
    final ClassName elementName = ClassName.get(superClassElement);
    final TypeName interceptorType = TypeName.get(MethodInterceptor.class);
    final String name = "Interceptor_" + Joiner.on("_").join(elementName.simpleNames());

    final TypeSpec.Builder classBuilder = TypeSpec.classBuilder(name) //
        .addOriginatingElement(superClassElement) //
        .addAnnotation(AnnotationSpec.builder(Generated.class) //
            .addMember("value", "$S", this.getClass().getCanonicalName()).build()) //
        .addModifiers(Modifier.PUBLIC, Modifier.FINAL) //
        .addField(interceptorType, "interceptor", Modifier.PRIVATE, Modifier.FINAL) //
        .superclass(ClassName.get(superClassElement)) //
        .addAnnotations(Util.annotationMirrorToSpec(superClassElement.getAnnotationMirrors()));

    // Constructor
    ExecutableElement constructorElement = null;
    for (final Element el : superClassElement.getEnclosedElements()) {
      if (el.getKind() == ElementKind.CONSTRUCTOR) {
        constructorElement = MoreElements.asExecutable(el);
        break;
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
    for (final MethodBind methodBind : methodBinds) {
      final ExecutableElement methodElement = methodBind.getMethodElement();
      final MethodSpec.Builder newMethod =
          Util.copyMethod(MoreElements.asExecutable(methodElement), Transactional.class);
      final String methodName = methodElement.getSimpleName().toString();

      if (methodElement.getReturnType().getKind() == TypeKind.VOID) {
        final TypeName methodInvocationClass = TypeName.get(MethodInvocation.class);

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
                + "    public Iterable<Annotation> annotations() {\n" //
                + "      return $L.this.$LAnnotations;\n" //
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
        final TypeName methodInvocationClass = TypeName.get(MethodInvocation.class);

        newMethod.addCode(
            "try {\n" //
                + "  return ($T) interceptor.invoke(new $T() {\n" //
                + "    @Override\n" //
                + "    public Object proceed() {\n" //
                + "      return $L.super.$L($L);\n" //
                + "    }\n" //
                + "\n" //
                + "    @Override\n" //
                + "    public Iterable<Annotation> annotations() {\n" //
                + "      return $L.this.$LAnnotations;\n" //
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
          ParameterizedTypeName.get(List.class, Annotation.class), //
          methodName + "Annotations", //
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
              + "  this.$LAnnotations = $T.asList(method.getAnnotations());\n", //
          methodName, parameters, methodName, Arrays.class);
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

  /**
   * Print an Error message to Processing Environment.
   *
   * <p>
   * <strong><em>This will just print the message, callers must stop processing in case of failure.</em></strong>
   *
   * @param element Element that generated the message.
   * @param message Message to be printed.
   */
  private void printError(final Element element, final String message) {
    processingEnv.getMessager().printMessage(Kind.ERROR, message, element);
  }

  /**
   * Print a Warning message to Processing Environment.
   * 
   * @param element Element that generated the message.
   * @param message Message to be printed.
   */
  private void printWarning(final Element element, final String message) {
    processingEnv.getMessager().printMessage(Kind.MANDATORY_WARNING, message, element);
  }

}
