package tn.iset.investplatformpfe.Repository;

import tn.iset.investplatformpfe.Entity.ServiceRequest;
import tn.iset.investplatformpfe.Entity.ServiceStatus;
import tn.iset.investplatformpfe.Entity.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    List<ServiceRequest> findByStatus(ServiceStatus status);

    List<ServiceRequest> findByRequestType(RequestType type);

    List<ServiceRequest> findByPartnerId(Long partnerId);

    List<ServiceRequest> findByServiceId(Long serviceId);

    @Query("SELECT sr FROM ServiceRequest sr WHERE sr.service.id = :serviceId AND sr.status = :status")
    List<ServiceRequest> findByServiceIdAndStatus(@Param("serviceId") Long serviceId,
                                                  @Param("status") ServiceStatus status);

    @Query("SELECT sr FROM ServiceRequest sr WHERE sr.requestType = :type AND sr.status = :status")
    List<ServiceRequest> findByTypeAndStatus(@Param("type") RequestType type,
                                             @Param("status") ServiceStatus status);

    long countByStatus(ServiceStatus status);

    // ✅ AJOUTER CES MÉTHODES
    @Query("SELECT COUNT(sr) FROM ServiceRequest sr WHERE sr.requestType = :type")
    long countByType(@Param("type") RequestType type);

    @Query("SELECT COUNT(sr) FROM ServiceRequest sr WHERE sr.requestType = :type AND sr.status = :status")
    long countByTypeAndStatus(@Param("type") RequestType type, @Param("status") ServiceStatus status);

    @Query("SELECT sr FROM ServiceRequest sr WHERE sr.requestType = 'DELETE' AND sr.status = 'APPROVED' AND sr.executionDate < :now")
    List<ServiceRequest> findExpiredDeleteApprovals(@Param("now") LocalDateTime now);
}