package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import tn.iset.investplatformpfe.Entity.CollaborationService;
import tn.iset.investplatformpfe.Entity.ServiceStatus;
import tn.iset.investplatformpfe.Entity.TouristService;

public interface TouristServiceRepository extends JpaRepository<TouristService, Long> {

    List<TouristService> findByProviderId(Long providerId);
    List<TouristService> findByStatus(ServiceStatus status);
    // Dans TouristServiceRepository.java
    List<TouristService> findByProviderIdAndStatus(Long providerId, ServiceStatus status);

}