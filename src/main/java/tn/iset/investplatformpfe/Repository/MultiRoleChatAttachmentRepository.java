package tn.iset.investplatformpfe.Repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.MultiRoleChatAttachment;


import java.util.List;

@Repository
public interface MultiRoleChatAttachmentRepository extends JpaRepository<MultiRoleChatAttachment, Long> {
    List<MultiRoleChatAttachment> findByChatId(Long chatId);
}
