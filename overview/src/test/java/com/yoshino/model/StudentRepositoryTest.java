package com.yoshino.model;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class StudentRepositoryTest {

    private EntityManagerFactory factory;
    private EntityManager manager;
    private StudentRepository studentRepository;
    private Student saved;

    @BeforeEach
    public void before() {
        factory = Persistence.createEntityManagerFactory("student");
        manager = factory.createEntityManager();
        studentRepository = new StudentRepository(manager);


        manager.getTransaction().begin();
        saved = studentRepository.save(new Student("john", "smith", "john.smith@email.com"));
        manager.getTransaction().commit();

    }

    @AfterEach
    public void after() {
        manager.clear();
        manager.close();
        factory.close();
    }

    @Test
    public void should_save_student_to_db() {
        List result = manager.createNativeQuery("select id, first_name, last_name, email from students").getResultList();

        assertEquals(1, result.size());

        Object[] john = (Object[]) result.get(0);

        assertEquals(BigInteger.valueOf(saved.getId()), john[0]);
        assertEquals(saved.getFirstName(), john[1]);
        assertEquals(saved.getLastName(), john[2]);
        assertEquals(saved.getEmail(), john[3]);
    }

    @Test
    public void should_generate_id_for_save_entity() {
        assertNotEquals(0, saved.getId());
    }

    @Test
    public void should_be_able_to_load_saved_by_id() {
        Optional<Student> loaded = studentRepository.findById(saved.getId());

        assertTrue(loaded.isPresent());
        assertEquals(saved.getId(), loaded.get().getId());
        assertEquals(saved.getFirstName(), loaded.get().getFirstName());
        assertEquals(saved.getLastName(), loaded.get().getLastName());
        assertEquals(saved.getEmail(), loaded.get().getEmail());

    }

    @Test
    public void should_be_able_to_load_saved_by_email() {
        Optional<Student> loaded = studentRepository.findByEmail(saved.getEmail());

        assertTrue(loaded.isPresent());
        assertEquals(saved.getId(), loaded.get().getId());
        assertEquals(saved.getFirstName(), loaded.get().getFirstName());
        assertEquals(saved.getLastName(), loaded.get().getLastName());
        assertEquals(saved.getEmail(), loaded.get().getEmail());
    }
}