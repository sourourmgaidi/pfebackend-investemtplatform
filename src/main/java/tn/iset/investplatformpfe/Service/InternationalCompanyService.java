package tn.iset.investplatformpfe.Service;

import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Entity.CollaborationService;
import tn.iset.investplatformpfe.Entity.InvestmentService;
import tn.iset.investplatformpfe.Entity.ServiceStatus;
import tn.iset.investplatformpfe.Repository.CollaborationServiceRepository;
import tn.iset.investplatformpfe.Repository.InvestmentServiceRepository;

import java.util.List;

@Service
public class InternationalCompanyService {

    private final CollaborationServiceRepository collaborationRepo;
    private final InvestmentServiceRepository investmentRepo;

    public InternationalCompanyService(CollaborationServiceRepository collaborationRepo,
                                       InvestmentServiceRepository investmentRepo) {
        this.collaborationRepo = collaborationRepo;
        this.investmentRepo = investmentRepo;
    }

    // Récupérer tous les services de collaboration approuvés
    public List<CollaborationService> getApprovedCollaborationServices() {
        return collaborationRepo.findByStatus(ServiceStatus.APPROVED);
    }

    // Récupérer tous les services d'investissement approuvés
    public List<InvestmentService> getApprovedInvestmentServices() {
        return investmentRepo.findByStatus(ServiceStatus.APPROVED);
    }

    // Récupérer un service de collaboration par ID, uniquement s'il est approuvé
    public CollaborationService getApprovedCollaborationServiceById(Long id) {
        CollaborationService service = collaborationRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Service de collaboration non trouvé"));
        if (service.getStatus() != ServiceStatus.APPROVED) {
            throw new RuntimeException("Ce service n'est pas encore approuvé");
        }
        return service;
    }

    // Récupérer un service d'investissement par ID, uniquement s'il est approuvé
    public InvestmentService getApprovedInvestmentServiceById(Long id) {
        InvestmentService service = investmentRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Service d'investissement non trouvé"));
        if (service.getStatus() != ServiceStatus.APPROVED) {
            throw new RuntimeException("Ce service n'est pas encore approuvé");
        }
        return service;
    }
}