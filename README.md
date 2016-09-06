# Dagger2 JPA

### ***This is a work in progress. API may change***

Lightweight JPA transaction management library, based on the [dagger-aop](https://github.com/0x3333/dagger-aop) project.

## What is it?

***dagger-jpa*** is a library that  provides abstractions for working with JPA transactions in a Dagger 2 application. It works in all kind of aplication, Java desktop, server application, Servlet environment, etc.

As it is an extension to [dagger-aop](https://github.com/0x3333/dagger-aop), it will generate code at compile time using an annotation-based API.

This project has been inspired by [Guice Persist](https://github.com/google/guice/wiki/GuicePersist).

Classes that have `@Transactional` methods must be abstract and have only one constructor or no constructor.

## Usage

If you are using Maven, add a dependency:

```xml
  <!-- Core -->
  <dependency>
      <groupId>com.github.0x3333.dagger.jpa</groupId>
      <artifactId>dagger-jpa-core</artifactId>
      <version>1.0-SNAPSHOT</version>
  </dependency>
  <!-- This is only necessary on compile time, optional -->
  <dependency>
      <groupId>com.github.0x3333.dagger.jpa</groupId>
      <artifactId>dagger-jpa-compiler</artifactId>
      <version>1.0-SNAPSHOT</version>
      <optional>true</optional>
  </dependency>
```

Despite adding dagger-jpa as a dependency, you need to include the `JpaModule` and `InterceptorModule` in your Dagger Component/Module and annotated your methods using `@Transactional` annotation.

Also you need to start the `JpaService` before any transactional method is called.

```java
// Adding JpaModule and InterceptorModule
@Component(modules = { MyModule.class, JpaModule.class, InterceptorModule.class })
public interface MyComponent {

	DbWork dbWork();
	
	JpaService jpaService();

}
```

This is a normal module where an interface is binded to an implementation:

```java
@Module
public abstract class MyModule {

	@Binds
	abstract DbWork providesTransac(DbWorkImpl impl);
	
}
```

In your transactional classes, annotated all methods that need to be transactional with `@Transactional`:

```java
// Class must be ABSTRACT
public abstract class DbWorkImpl implements DbWork {

  private final Provider<EntityManager> emProvider;

  // Inject a Provider when the instance is Singleton or used outside the scope.
  public DbWorkImpl(Provider<EntityManager> emProvider) {
    this.emProvider = emProvider;
  }

  @Override
  @Transactional
  public void doSomeWork() {
    // You can get an EntityManager only inside a Transacional method.
    /// emProvider.get().createQuery(....);
  }

}
```

To create your Component you need to install `JpaModule` which will require the JPA Unit name.

```java
MyComponent component = DaggerMyComponent.builder().jpaModule(new JpaModule("jpa-unit-name")).build();

component.jpaService().start();
```

This is all. The `InterceptorModule` will bind `DbWorkImpl` to the generated `Interceptor_DbWorkImpl`, which is a subclass of `DbWorkImpl`. Everytime a `DbWork` is requested, a `Interceptor_DbWorkImpl` will be returned. This subclass will manage the transaction for you.

## Cavets

If your class have `@Inject` fields, but no constructor with `@Inject`, means to Dagger that it can inject those fields when requested but it will not create new instances of this class. This behavour is changed when using `dagger-jpa`, because it creates a constructor annotated with `@Inject` if none is present. Thus, the instance will be created by Dagger and also members injected. This is not an issue to most people, but something to consider in unusual use cases.

License
-------

    Copyright (C) 2016 Tercio Gaudencio Filho

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
