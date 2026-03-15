package tn.iset.investplatformpfe.Repository;


import org.springframework.data.jpa.repository.JpaRepository;
import tn.iset.investplatformpfe.Entity.CollaborationServiceDocument;
import java.util.List;

public interface CollaborationServiceDocumentRepository extends JpaRepository<CollaborationServiceDocument, Long> {
    List<CollaborationServiceDocument> findByCollaborationServiceId(Long serviceId);
}
