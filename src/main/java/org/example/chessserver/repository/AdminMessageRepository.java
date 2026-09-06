package org.example.chessserver.repository;

import org.example.chessserver.entity.AdminMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdminMessageRepository extends JpaRepository<AdminMessage, Long> {
    List<AdminMessage> findByRecipientIdOrderBySentAtDesc(Integer recipientId);

    List<AdminMessage> findAllByOrderBySentAtDesc();

    List<AdminMessage> findByRecipientIdAndIsReadFalse(Integer recipientId);

    @Modifying
    @Query("UPDATE AdminMessage m SET m.isRead = true WHERE m.recipientId = :recipientId AND m.isRead = false")
    void markAllAsRead(@Param("recipientId") Integer recipientId);

    @Modifying
    @Query("UPDATE AdminMessage m SET m.isRead = true WHERE m.id = :messageId AND m.recipientId = :recipientId")
    int markAsRead(@Param("messageId") Long messageId, @Param("recipientId") Integer recipientId);
}
