
// Repository interface that was missing
package com.prescription.repository;

import com.prescription.entity.AccessToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessTokenRepository extends JpaRepository<AccessToken, Long> {

    Optional<AccessToken> findByTokenAndIsValidTrue(String token);

    List<AccessToken> findByPatientDoctorAccess_AccessId(Long accessId);
}