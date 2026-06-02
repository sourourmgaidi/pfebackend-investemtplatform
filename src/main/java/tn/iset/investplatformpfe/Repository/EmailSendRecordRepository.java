package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.iset.investplatformpfe.Entity.EmailSendRecord;

import java.time.LocalDateTime;
import java.util.List;

// EmailSendRecordRepository.java

public interface EmailSendRecordRepository
        extends JpaRepository<EmailSendRecord, Long> {

    List<EmailSendRecord> findByProspectIdOrderBySentAtDesc(Long prospectId);
    List<EmailSendRecord> findAllByOrderBySentAtDesc();
    List<EmailSendRecord> findBySentAtBetweenOrderBySentAtDesc(
            LocalDateTime start,
            LocalDateTime end
    );
}