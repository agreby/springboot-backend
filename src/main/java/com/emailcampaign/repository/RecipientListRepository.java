package com.emailcampaign.repository;

import com.emailcampaign.model.RecipientList;
import com.emailcampaign.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface RecipientListRepository extends JpaRepository<RecipientList, Long> {
    
    Page<RecipientList> findByUser(User user, Pageable pageable);
    
    @Query("SELECT rl FROM RecipientList rl WHERE rl.user = :user AND rl.name LIKE %:name%")
    Page<RecipientList> findByUserAndNameContaining(User user, String name, Pageable pageable);
    
    @Query("SELECT COUNT(r) FROM Recipient r WHERE r.recipientList.id = :listId AND r.status = 'ACTIVE'")
    long countActiveRecipientsByListId(Long listId);
}
