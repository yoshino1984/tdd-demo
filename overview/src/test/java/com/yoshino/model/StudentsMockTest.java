package com.yoshino.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author xiaoyi
 * 2022/4/23 11:16
 * @since
 **/
public class StudentsMockTest {

    private EntityManager manager;
    private Students repository;
    private Student john = new Student("john", "smith", "john.smith@email.com");

    @BeforeEach
    public void before() {
        manager = mock(EntityManager.class);
        repository = new Students(manager);
    }

    @Test
    public void should_generate_id_for_saved_entity() {
        repository.save(john);

        verify(manager).persist(john);
    }



    static class Students {
        private EntityManager manager;


        Students(EntityManager manager) {
            this.manager = manager;
        }


        public void save(Student student) {
            manager.persist(student);
        }
    }
}
