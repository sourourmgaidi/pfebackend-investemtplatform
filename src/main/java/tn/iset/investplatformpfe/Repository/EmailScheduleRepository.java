package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.iset.investplatformpfe.Entity.EmailSchedule;
import java.time.LocalDateTime;
import java.util.List;

public interface EmailScheduleRepository extends JpaRepository<EmailSchedule, Long> {
    List<EmailSchedule> findByStatusAndScheduledAtBefore(String status, LocalDateTime dateTime);
    List<EmailSchedule> findAllByOrderByScheduledAtDesc();
}