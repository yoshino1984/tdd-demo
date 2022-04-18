package com.yoshino.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class StudentRepositoryTest {

    private EntityManagerFactory factory;
    private EntityManager entityManager;
    private StudentRepository studentRepository;
    private Student john;

    @BeforeEach
    public void before() {
        factory = Persistence.createEntityManagerFactory("student");
        entityManager = factory.createEntityManager();
        studentRepository = new StudentRepository(entityManager);


        entityManager.getTransaction().begin();
        john = studentRepository.save(new Student("john", "smith", "john.smith@email.com"));
        entityManager.getTransaction().commit();

    }

    @AfterEach
    public void after() {
        entityManager.clear();
        entityManager.close();
        factory.close();
    }


    @Test
    public void should_generate_id_for_save_entity() {

        assertNotEquals(0, john.getId());
    }

    @Test
    public void should_be_able_to_load_saved_by_id() {
        Optional<Student> loaded = studentRepository.findById(john.getId());

        assertTrue(loaded.isPresent());
        assertEquals(john.getId(), loaded.get().getId());
        assertEquals(john.getFirstName(), loaded.get().getFirstName());
        assertEquals(john.getLastName(), loaded.get().getLastName());
        assertEquals(john.getEmail(), loaded.get().getEmail());

    }

    @Test
    public void should_be_able_to_load_saved_by_email() {
        Optional<Student> loaded = studentRepository.findByEmail(john.getEmail());

        assertTrue(loaded.isPresent());
        assertEquals(john.getId(), loaded.get().getId());
        assertEquals(john.getFirstName(), loaded.get().getFirstName());
        assertEquals(john.getLastName(), loaded.get().getLastName());
        assertEquals(john.getEmail(), loaded.get().getEmail());
    }
}