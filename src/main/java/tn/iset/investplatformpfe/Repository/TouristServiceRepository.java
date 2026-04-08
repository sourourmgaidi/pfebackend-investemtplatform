package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.iset.investplatformpfe.Entity.CollaborationService;
import tn.iset.investplatformpfe.Entity.ServiceStatus;
import tn.iset.investplatformpfe.Entity.TouristService;

public interface TouristServiceRepository extends JpaRepository<TouristService, Long> {

    List<TouristService> findByProviderId(Long providerId);
    List<TouristService> findByStatus(ServiceStatus status);
    // Dans TouristServiceRepository.java
    List<TouristService> findByProviderIdAndStatus(Long providerId, ServiceStatus status);
    long countByStatus(ServiceStatus status);
    long countByRegionIdAndStatus(Long regionId, ServiceStatus status);
    List<TouristService> findByRegionIdAndStatus(Long regionId, ServiceStatus status);
    @Query("SELECT t FROM TouristService t WHERE t.region.id = :regionId AND t.status IN :statuses")
    List<TouristService> findByRegionIdAndStatusIn(@Param("regionId") Long regionId, @Param("statuses") List<ServiceStatus> statuses);

    // ✅ NOUVELLE MÉTHODE POUR COMPTER AVEC PLUSIEURS STATUTS
    @Query("SELECT COUNT(t) FROM TouristService t WHERE t.region.id = :regionId AND t.status IN :statuses")
    long countByRegionIdAndStatusIn(@Param("regionId") Long regionId, @Param("statuses") List<ServiceStatus> statuses);
}