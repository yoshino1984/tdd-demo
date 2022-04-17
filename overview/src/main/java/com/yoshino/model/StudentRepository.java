package com.yoshino.model;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import java.util.Optional;

/**
 * @author xiaoyi
 */
public class StudentRepository {

    private final EntityManager manager;

    public StudentRepository(EntityManager manager) {
        this.manager = manager;
    }

    public Student save(Student student) {
        manager.persist(student);
        return student;
    }

    public Optional<Student> findById(long id) {
        return Optional.ofNullable(manager.find(Student.class, id));
    }

    public Optional<Student> findByEmail(String email) {
        TypedQuery<Student> typedQuery = manager.createQuery("select s from Student s where s.email=:email", Student.class);
        return typedQuery.setParameter("email", email).getResultList().stream().findFirst();
    }
}
