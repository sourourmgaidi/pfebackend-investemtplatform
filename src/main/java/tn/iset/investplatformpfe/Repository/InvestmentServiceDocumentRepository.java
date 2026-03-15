package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tn.iset.investplatformpfe.Entity.InvestmentServiceDocument;
import java.util.List;

public interface InvestmentServiceDocumentRepository extends JpaRepository<InvestmentServiceDocument, Long> {
    List<InvestmentServiceDocument> findByInvestmentServiceId(Long serviceId);
}
