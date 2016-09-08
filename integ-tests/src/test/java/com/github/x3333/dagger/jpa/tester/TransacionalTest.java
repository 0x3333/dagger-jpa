package com.github.x3333.dagger.jpa.tester;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.sql.DriverManager;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.x3333.dagger.jpa.JpaModule;
import com.github.x3333.dagger.jpa.JpaService;
import com.github.x3333.dagger.jpa.tester.TransactionalClass.MyException;
import com.github.x3333.dagger.jpa.tester.domain.SomeEntity;

public class TransacionalTest {

	private static JpaService jpaService;
	private static TransactionalInterface transactional;

	@BeforeClass
	public static void start() throws Exception {
		try {
			Class.forName("org.apache.derby.jdbc.EmbeddedDriver");
			DriverManager.getConnection("jdbc:derby:memory:dagger-jpa;create=true").close();
		} catch (Exception e) {
			fail("Exception during database startup.");
			e.printStackTrace();
		}

		
		TestComponent component = DaggerTestComponent.builder().build();

		jpaService = component.jpaService();
		jpaService.start();

		transactional = component.transactional();
	}

	@AfterClass
	public static void stop() {
		jpaService.stop();
	}

	@Test
	public void testTransactionalCommit() {
		cleanup();
		int id = transactional.transactionalCommit();

		jpaService.begin();
		try {
			EntityManager em = jpaService.get();
			TypedQuery<SomeEntity> query = em.createQuery("FROM SomeEntity WHERE id = :id", SomeEntity.class);
			query.setParameter("id", id);
			SomeEntity entity = query.getSingleResult();
			assertNotNull(entity);
		} catch (Exception e) {
			fail("Exception during query.");
			e.printStackTrace();
		} finally {
			jpaService.end();
		}
	}

	@Test
	public void transactionalExceptionNoRollback() {
		cleanup();
		try {
			transactional.transactionalExceptionNoRollback();
			fail("Should have thrown a MyException.");
		} catch (RuntimeException e) {
		} catch (Throwable e) {
			fail("Should have thrown a RuntimeException, but thrown a Throwable.");
			e.printStackTrace();
		}

		jpaService.begin();
		try {
			EntityManager em = jpaService.get();
			TypedQuery<Long> query = em.createQuery("SELECT COUNT(e.id) FROM SomeEntity e", Long.class);
			Long count = query.getSingleResult();
			assertEquals(count, (Long) 1l);
		} catch (Exception e) {
			fail("Exception during query.");
			e.printStackTrace();
		} finally {
			jpaService.end();
		}
	}

	@Test
	public void transactionalExceptionRollback() {
		cleanup();
		try {
			transactional.transactionalExceptionRollback();
			fail("Should have thrown a MyException.");
		} catch (MyException e) {
		} catch (Throwable e) {
			fail("Should have thrown a MyException, but thrown a Throwable.");
			e.printStackTrace();
		}

		jpaService.begin();
		try {
			EntityManager em = jpaService.get();
			TypedQuery<Long> query = em.createQuery("SELECT COUNT(e.id) FROM SomeEntity e", Long.class);
			Long count = query.getSingleResult();
			assertEquals(count, (Long) 0l);
		} catch (Exception e) {
			fail("Exception during query.");
			e.printStackTrace();
		} finally {
			jpaService.end();
		}
	}

	/**
	 * Cleanup the database to start a test
	 */
	private void cleanup() {
		jpaService.begin();
		EntityManager em = jpaService.get();
		em.getTransaction().begin();
		Query query = em.createQuery("DELETE FROM SomeEntity");
		query.executeUpdate();
		em.getTransaction().commit();
		jpaService.end();
	}

}
