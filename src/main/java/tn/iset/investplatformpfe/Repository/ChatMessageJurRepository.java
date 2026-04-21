package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.iset.investplatformpfe.Entity.ChatMessageJur;

import java.util.List;

public interface ChatMessageJurRepository extends JpaRepository<ChatMessageJur, Long> {
    List<ChatMessageJur> findByUserIdOrderByTimestampAsc(String userId);
}

