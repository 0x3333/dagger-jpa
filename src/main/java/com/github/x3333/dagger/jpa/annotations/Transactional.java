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

package com.github.x3333.dagger.jpa.annotations;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Any method or class marked with this annotation will be considered for transactionality. Marking a method {@code @Transactional} will
 * start a new transaction before the method executes and commit it after the method returns.
 * 
 * <p>
 * If the method throws an exception the transaction will be rolled back.
 * 
 * @author Tercio Gaudencio Filho (terciofilho [at] gmail.com)
 */
@Documented
@Retention(CLASS)
@Target(METHOD)
public @interface Transactional {

}
