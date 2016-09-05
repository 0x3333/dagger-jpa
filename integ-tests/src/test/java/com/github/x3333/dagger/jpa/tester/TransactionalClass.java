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

package com.github.x3333.dagger.jpa.tester;

import javax.inject.Provider;
import javax.persistence.EntityManager;

import com.github.x3333.dagger.jpa.Transactional;
import com.github.x3333.dagger.jpa.tester.domain.SomeEntity;

public abstract class TransactionalClass implements TransactionalInterface {

	public static class MyException extends Exception {

		private static final long serialVersionUID = -8123896745778519533L;

	}

	private Provider<EntityManager> em;

	public TransactionalClass(Provider<EntityManager> em) {
		this.em = em;
	}
	
	@Override
	@Transactional
	public int transactionalCommit() {
		SomeEntity entity = new SomeEntity();
		em.get().persist(entity);
		em.get().flush();
		return entity.getId();
	}

	@Override
	@Transactional(rollbackOn = MyException.class)
	public void transactionalExceptionNoRollback() throws MyException {
		transactionalCommit();
		throw new RuntimeException();
	}

	@Override
	@Transactional(rollbackOn = MyException.class)
	public void transactionalExceptionRollback() throws MyException {
		transactionalCommit();
		throw new MyException();
	}

}
