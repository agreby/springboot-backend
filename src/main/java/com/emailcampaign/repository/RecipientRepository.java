package com.emailcampaign.repository;

import com.emailcampaign.model.Recipient;
import com.emailcampaign.model.RecipientList;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecipientRepository extends JpaRepository<Recipient, Long> {
    
    Page<Recipient> findByRecipientList(RecipientList recipientList, Pageable pageable);
    
    Optional<Recipient> findByEmailAndRecipientList(String email, RecipientList recipientList);
    
    @Query("SELECT r FROM Recipient r WHERE r.recipientList = :recipientList AND r.status = :status")
    Page<Recipient> findByRecipientListAndStatus(RecipientList recipientList, Recipient.RecipientStatus status, Pageable pageable);
    
    @Query("SELECT COUNT(r) FROM Recipient r WHERE r.recipientList = :recipientList AND r.status = 'ACTIVE'")
    long countActiveByRecipientList(RecipientList recipientList);
    
    boolean existsByEmailAndRecipientList(String email, RecipientList recipientList);
}
