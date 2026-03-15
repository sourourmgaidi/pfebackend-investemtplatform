package tn.iset.investplatformpfe.Repository;



import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.iset.investplatformpfe.Entity.CollaborationServiceRequest;
import tn.iset.investplatformpfe.Entity.RequestType;
import tn.iset.investplatformpfe.Entity.ServiceStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface CollaborationServiceRequestRepository extends JpaRepository<CollaborationServiceRequest, Long> {

    List<CollaborationServiceRequest> findByPartnerId(Long partnerId);

    List<CollaborationServiceRequest> findByStatus(ServiceStatus status);

    List<CollaborationServiceRequest> findByRequestType(RequestType type);

    List<CollaborationServiceRequest> findByServiceId(Long serviceId);

    @Query("SELECT r FROM CollaborationServiceRequest r WHERE r.service.id = :serviceId AND r.status = :status")
    List<CollaborationServiceRequest> findByServiceIdAndStatus(@Param("serviceId") Long serviceId,
                                                               @Param("status") ServiceStatus status);

    @Query("SELECT r FROM CollaborationServiceRequest r WHERE r.requestType = :type AND r.status = :status")
    List<CollaborationServiceRequest> findByTypeAndStatus(@Param("type") RequestType type,
                                                          @Param("status") ServiceStatus status);

    long countByStatus(ServiceStatus status);

    @Query("SELECT COUNT(r) FROM CollaborationServiceRequest r WHERE r.requestType = :type")
    long countByType(@Param("type") RequestType type);

    @Query("SELECT COUNT(r) FROM CollaborationServiceRequest r WHERE r.requestType = :type AND r.status = :status")
    long countByTypeAndStatus(@Param("type") RequestType type, @Param("status") ServiceStatus status);
}
