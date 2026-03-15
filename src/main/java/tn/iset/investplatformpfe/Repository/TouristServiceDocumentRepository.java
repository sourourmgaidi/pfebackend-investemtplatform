package tn.iset.investplatformpfe.Repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import tn.iset.investplatformpfe.Entity.TouristServiceDocument;

import java.util.List;

@Repository
public interface TouristServiceDocumentRepository extends JpaRepository<TouristServiceDocument, Long> {
    List<TouristServiceDocument> findByTouristServiceId(Long touristServiceId);
}
