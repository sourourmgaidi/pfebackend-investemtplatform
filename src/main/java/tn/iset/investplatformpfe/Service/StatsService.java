package tn.iset.investplatformpfe.Service;


import org.springframework.stereotype.Service;
import tn.iset.investplatformpfe.Dto.MonthlyRegistrationDTO;
import tn.iset.investplatformpfe.Dto.RegionStatsDTO;
import tn.iset.investplatformpfe.Dto.RegistrationBarDTO;
import tn.iset.investplatformpfe.Dto.StatsSummaryDTO;
import tn.iset.investplatformpfe.Entity.ServiceStatus;
import tn.iset.investplatformpfe.Repository.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


@Service
public class StatsService {
    private final RegionRepository regionRepo;
    private final InvestorRepository investorRepo;
    private final EconomicPartnerRepository economicPartnerRepo;
    private final InternationalCompanyRepository intlCompanyRepo;
    private final TouristRepository touristRepo;
    private final LocalPartnerRepository localPartnerRepo;
    private final InvestmentServiceRepository investmentServiceRepo;
    private final CollaborationServiceRepository collaborationServiceRepo;
    private final TouristServiceRepository touristServiceRepo;

    public StatsService(
            InvestorRepository investorRepo,
            EconomicPartnerRepository economicPartnerRepo,
            InternationalCompanyRepository intlCompanyRepo,
            TouristRepository touristRepo,
            LocalPartnerRepository localPartnerRepo,
            InvestmentServiceRepository investmentServiceRepo,
            CollaborationServiceRepository collaborationServiceRepo,
            TouristServiceRepository touristServiceRepo,RegionRepository regionRepo) {
        this.investorRepo = investorRepo;
        this.economicPartnerRepo = economicPartnerRepo;
        this.intlCompanyRepo = intlCompanyRepo;
        this.touristRepo = touristRepo;
        this.localPartnerRepo = localPartnerRepo;
        this.investmentServiceRepo = investmentServiceRepo;
        this.collaborationServiceRepo = collaborationServiceRepo;
        this.touristServiceRepo = touristServiceRepo;
        this.regionRepo=regionRepo;
    }

    public StatsSummaryDTO getSummary() {
        StatsSummaryDTO dto = new StatsSummaryDTO();

        dto.setTotalInvestors(investorRepo.count());
        dto.setTotalEconomicPartners(economicPartnerRepo.count());
        dto.setTotalInternationalCompanies(intlCompanyRepo.count());
        dto.setTotalTourists(touristRepo.count());
        dto.setTotalLocalPartners(localPartnerRepo.count());

        dto.setApprovedInvestmentServices(
                investmentServiceRepo.countByStatus(ServiceStatus.APPROVED));
        dto.setApprovedCollaborationServices(
                collaborationServiceRepo.countByStatus(ServiceStatus.APPROVED));
        dto.setApprovedTouristServices(
                touristServiceRepo.countByStatus(ServiceStatus.APPROVED));

        return dto;
    }


    public List<RegionStatsDTO> getRegionStats() {
        return regionRepo.findAll().stream().map(region -> {
            RegionStatsDTO dto = new RegionStatsDTO();
            dto.setId(region.getId());
            dto.setName(region.getName());
            dto.setCode(region.getCode());
            dto.setGeographicalZone(region.getGeographicalZone());
            dto.setEconomicDescription(region.getEconomicDescription());
            dto.setTaxIncentives(region.getTaxIncentives());
            dto.setInfrastructure(region.getInfrastructure());
            dto.setApprovedInvestmentServices(
                    investmentServiceRepo.countByRegionIdAndStatus(region.getId(), ServiceStatus.APPROVED));
            dto.setApprovedCollaborationServices(
                    collaborationServiceRepo.countByRegionIdAndStatus(region.getId(), ServiceStatus.APPROVED));
            dto.setApprovedTouristServices(
                    touristServiceRepo.countByRegionIdAndStatus(region.getId(), ServiceStatus.APPROVED));
            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }

    public List<RegistrationBarDTO> getDailyRegistrations(int year, int month) {
        List<RegistrationBarDTO> result = new ArrayList<>();

        // Nombre de jours dans le mois demandé
        LocalDate firstDay = LocalDate.of(year, month, 1);
        int daysInMonth = firstDay.lengthOfMonth();

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDateTime start = LocalDateTime.of(year, month, day, 0, 0, 0);
            LocalDateTime end   = start.plusDays(1);

            int total = investorRepo.countByDateRange(start, end)
                    + localPartnerRepo.countByDateRange(start, end)
                    + economicPartnerRepo.countByDateRange(start, end)
                    + intlCompanyRepo.countByDateRange(start, end)
                    + touristRepo.countByDateRange(start, end);

            result.add(new RegistrationBarDTO(
                    String.valueOf(day),                          // label "1", "2" ... "31"
                    LocalDate.of(year, month, day).format(fmt),   // "2025-03-01"
                    total
            ));
        }
        return result;
    }

    // ========================================
    // NEW METHODS FOR MONTHLY STATISTICS
    // ========================================

    /**
     * Get monthly registration statistics with month-over-month comparison
     * @param numberOfMonths Number of months to retrieve (default: 12)
     * @return List of monthly registration statistics
     */
    public List<MonthlyRegistrationDTO> getMonthlyRegistrations(int numberOfMonths) {
        List<MonthlyRegistrationDTO> result = new ArrayList<>();

        String[] monthNames = {
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
        };

        LocalDate now = LocalDate.now();

        for (int i = numberOfMonths - 1; i >= 0; i--) {
            LocalDate monthDate = now.minusMonths(i);
            int year = monthDate.getYear();
            int month = monthDate.getMonthValue();

            int total = investorRepo.countByYearAndMonth(year, month)
                    + localPartnerRepo.countByYearAndMonth(year, month)
                    + economicPartnerRepo.countByYearAndMonth(year, month)
                    + intlCompanyRepo.countByYearAndMonth(year, month)
                    + touristRepo.countByYearAndMonth(year, month);

            MonthlyRegistrationDTO dto = new MonthlyRegistrationDTO(
                    monthNames[month - 1],
                    String.format("%d-%02d", year, month),
                    total
            );

            result.add(dto);
        }

        // Calculate month-over-month comparisons
        for (int i = 0; i < result.size(); i++) {
            MonthlyRegistrationDTO current = result.get(i);
            if (i > 0) {
                MonthlyRegistrationDTO previous = result.get(i - 1);
                current.setPreviousMonthCount(previous.getCount());

                int percentageChange = 0;
                if (previous.getCount() > 0) {
                    percentageChange = ((current.getCount() - previous.getCount()) * 100) / previous.getCount();
                } else if (current.getCount() > 0) {
                    percentageChange = 100;
                }
                current.setPercentageChange(percentageChange);
            } else {
                current.setPreviousMonthCount(0);
                current.setPercentageChange(0);
            }
        }

        return result;
    }

    /**
     * Generate notification comparing current month vs previous month
     * @return Formatted notification message
     */
    public String getMonthOverMonthNotification() {
        List<MonthlyRegistrationDTO> monthlyData = getMonthlyRegistrations(2);

        if (monthlyData.size() >= 2) {
            MonthlyRegistrationDTO current = monthlyData.get(1);
            MonthlyRegistrationDTO previous = monthlyData.get(0);

            int change = current.getPercentageChange();
            String trend = change > 0 ? " increase" : (change < 0 ? " decrease" : " stable");

            return String.format("Registrations %s: %d vs %d (%s of %d%%)",
                    current.getMonthLabel(),
                    current.getCount(),
                    previous.getCount(),
                    trend,
                    Math.abs(change)
            );
        }

        return "Not enough data to compare";
    }
}