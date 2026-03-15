package tn.iset.investplatformpfe.Entity;

import java.time.LocalDateTime;

public interface Servicable {
    Long getId();
    String getName();
    LocalPartner getProvider();
    ServiceStatus getStatus();
    void setStatus(ServiceStatus status);
    LocalDateTime getEditAuthorizedUntil();
    void setEditAuthorizedUntil(LocalDateTime editAuthorizedUntil);
    Long getAuthorizedByAdminId();
    void setAuthorizedByAdminId(Long authorizedByAdminId);
    boolean isEditAuthorized();
    boolean isDeleteAuthorized();
}
