package com.prescription.repository;

import com.prescription.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByCnic(String cnic);

    Optional<User> findByCnic(String cnic);

    Optional<User> findByRegistrationToken(String token);

    @Query("SELECT u FROM User u WHERE u.accountStatus = 'PENDING' ORDER BY u.createdAt DESC")
    List<User> findPendingRegistrations();

    @Query("SELECT u FROM User u WHERE u.accountStatus = 'APPROVED' AND u.passwordSet = false")
    List<User> findApprovedButNotActivated();

    @Query("""
        SELECT u FROM User u 
        LEFT JOIN FETCH u.patient 
        LEFT JOIN FETCH u.doctor 
        WHERE u.userId = :userId
    """)
    Optional<User> findByIdWithDetails(Long userId);

    @Query("""
        SELECT u FROM User u
        LEFT JOIN FETCH u.patient
        LEFT JOIN FETCH u.doctor
        WHERE u.accountStatus = 'PENDING'
        ORDER BY u.createdAt DESC
    """)
    List<User> findPendingRegistrationsWithDetails();
}