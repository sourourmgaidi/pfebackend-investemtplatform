package tn.iset.investplatformpfe.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tn.iset.investplatformpfe.Entity.RequestType;
import tn.iset.investplatformpfe.Entity.ServiceStatus;
import tn.iset.investplatformpfe.Entity.TouristServiceRequest;

import java.util.List;

public interface TouristServiceRequestRepository extends JpaRepository<TouristServiceRequest, Long> {

    List<TouristServiceRequest> findByServiceId(Long serviceId);

    List<TouristServiceRequest> findByPartnerId(Long partnerId);

    List<TouristServiceRequest> findByRequestType(RequestType requestType);

    List<TouristServiceRequest> findByStatus(ServiceStatus status);

    List<TouristServiceRequest> findByServiceIdAndStatus(Long serviceId, ServiceStatus status);

    List<TouristServiceRequest> findByPartnerIdAndStatus(Long partnerId, ServiceStatus status);

    List<TouristServiceRequest> findByRequestTypeAndStatus(RequestType requestType, ServiceStatus status);

    boolean existsByServiceIdAndStatus(Long serviceId, ServiceStatus status);

    long countByStatus(ServiceStatus status);

    long countByRequestTypeAndStatus(RequestType requestType, ServiceStatus status);

    long countByRequestType(RequestType requestType);

    @Query("SELECT r FROM TouristServiceRequest r " +
            "JOIN FETCH r.service s " +
            "JOIN FETCH r.partner p " +
            "LEFT JOIN FETCH r.admin a " +
            "WHERE r.status = :status")
    List<TouristServiceRequest> findAllWithDetailsByStatus(@Param("status") ServiceStatus status);

    @Query("SELECT r FROM TouristServiceRequest r " +
            "JOIN FETCH r.service s " +
            "JOIN FETCH r.partner p " +
            "LEFT JOIN FETCH r.admin a " +
            "ORDER BY r.requestDate DESC")
    List<TouristServiceRequest> findAllWithDetails();
}