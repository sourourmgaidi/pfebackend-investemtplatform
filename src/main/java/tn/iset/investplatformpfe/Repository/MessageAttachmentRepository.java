package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.MessageAttachment;

import java.util.List;

@Repository
public interface MessageAttachmentRepository extends JpaRepository<MessageAttachment, Long> {
    List<MessageAttachment> findByMessageId(Long messageId);
}
