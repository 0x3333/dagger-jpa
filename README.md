# Dagger2 JPA

### ***This is a work in progress. API is not defined yet.*** ###

Usage
-----

dagger-jpa generates a class with a `Transactional_` prefix, next to your class and that extends it,
whose some method(s) are annotated with @Transactional. This class has a method
interceptor which will manage JPA transactions, similar as [Guice Persist extension](https://github.com/google/guice/wiki/Transactions)

The `TransactionalInterceptor` will delegate to the appropriate method wrapping it with the logic behind a transaction management. [See here - `TransactionalInterceptor`](https://github.com/0x3333/dagger-jpa/blob/master/src/main/java/com/github/x3333/dagger/jpa/TransactionalInterceptor.java)

dagger-jpa processor is triggered by `@Transactional` annotations, so you need to put dagger-jpa in your processor path to get it to work, and bind your transactional classes in the module to the `Transactional_*` version of your classes. The transactional classes adds a dependecy to the already present dependencies. You have to provide a `TransactionalInterceptor` as a dependency to the newly created class.

Classes that have `@Transactional` methods must be abstract, non-final and have only one constructor or no constructor.

You must start the `JpaService` before using it. You must provide the `JPA Unit Name` to the `JpaModule` module.

If your class have `@Inject` fields, but no constructor with `@Inject`, means to Dagger that it can inject those fields when requested but it will not create new instances of this class. This behavour is changed when using dagger-jpa, because it creates a constructor annotated with `@Inject` if none is present. This is not an issue to most people, but something to consider in unusual use cases.

This is a initial work, I'll split into projects to get things a little more concise.

More docs later as I finish things up.

Example
-------

```java
YourComponent component = DaggerYourComponent.builder().yourModule(new YourModule()).jpaModule(new JpaModule("jpa-unit-name")).build();

component.jpaService().start();
```

```java
// You MUST add JpaModule to your module list
@Component(modules = { JpaModule.class, YourModule.class })
@Singleton
public interface YourComponent {

  YourApp yourApp();
  
  JpaService jpaService();

}
```

```java
@Module
public class YourModule {

  @Provides
  public DbWork providesDbWork(SomeDep someDep, Lazy<EntityManager> em, TransactionalInterceptor interceptor) {
    // Note the 'TransactionalInterceptor interceptor' dependency
    return new Transactional_DbWork(someDep, em, interceptor);
  }

}
```

```java
package com.github.x3333.dagger.jpa.tester;

import javax.inject.Inject;
import javax.persistence.EntityManager;

import com.github.x3333.dagger.jpa.annotations.Transactional;

import dagger.Lazy;

public abstract class DbWork {

  // Must be Lazy! Otherwise Dagger will try to inject it before initialization
  private Lazy<EntityManager> em;

  @Inject
  public Transac(SomeDep someDep, Lazy<EntityManager> em) {
    this.someDep = someDep;
    this.em = em;
  }

  @Transactional
  protected void someWork() {
    // Do some DB work with 'em'
    em.get().isOpen();
  }

  @Transactional
  protected String someWork2() {
    return "Hello Dagger-JPA!";
  }

}
```

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

