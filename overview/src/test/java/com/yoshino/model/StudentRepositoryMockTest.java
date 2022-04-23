package com.yoshino.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class StudentRepositoryMockTest {

    private EntityManager manager;
    private StudentRepository repository;
    private Student john = new Student("john", "smith", "john.smith@email.com");

    @BeforeEach
    public void before() {
        manager = mock(EntityManager.class);
        repository = new StudentRepository(manager);
    }

    @Test
    public void should_generate_id_for_save_entity() {
        repository.save(john);

        verify(manager).persist(john);
    }

    @Test
    public void should_be_able_to_load_saved_by_id() {
        when(manager.find(any(), any())).thenReturn(john);

        assertEquals(john, repository.findById(john.getId()).get());
    }

    @Test
    public void should_be_able_to_load_saved_by_email() {
        TypedQuery query = mock(TypedQuery.class);

        when(manager.createQuery(anyString(), any())).thenReturn(query);
        when(query.setParameter(any(String.class), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(asList(john));

        assertEquals(john, repository.findByEmail(john.getEmail()).get());

        verify(manager).createQuery("select s from Student s where s.email=:email", Student.class);
        verify(query).setParameter("email", john.getEmail());
    }
}