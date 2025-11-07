package controller;

import database.CompanyRepRepository;
import database.InternshipRepository;
import database.ApplicationRepository;
import database.WithdrawalRequestRepository;
import database.CareerCenterStaffRepository;
import entity.CompanyRep;
import entity.Internship;
import entity.InternshipStatus;
import entity.InternshipLevel;
import entity.WithdrawalRequest;
import entity.WithdrawalRequestStatus;
import entity.CareerCenterStaff;

import java.util.ArrayList;
import java.util.List;

public class CareerCenterStaffController extends BaseController {

    public CareerCenterStaffController(CompanyRepRepository companyRepRepo,
                                       InternshipRepository internshipRepo,
                                       ApplicationRepository applicationRepo,
                                       WithdrawalRequestRepository withdrawalsRepo,
                                       CareerCenterStaffRepository staffRepo) {
        super(null, companyRepRepo, staffRepo, internshipRepo, applicationRepo, withdrawalsRepo);
    }

    public List<Internship> listPendingInternships() {
        return internshipRepo.findByStatus(InternshipStatus.PENDING);
    }

    public List<CompanyRep> listUnapprovedReps() {
        List<CompanyRep> out = new ArrayList<>();
        for (CompanyRep r : repRepo.findAll()) {
            if (!r.isApproved() && !r.isRejected()) {
                out.add(r);
            }
        }
        return out;
    }
    
    public List<CompanyRep> listAllReps() {
        return repRepo.findAll();
    }

    public List<WithdrawalRequest> listPendingWithdrawals() {
        return withdrawalRepo.findPending();
    }
    
    public List<Internship> listAllInternships() {
        return internshipRepo.findAll();
    }

    public void approveInternship(String staffId, String internshipId, boolean makeVisible) {
        
        Internship i = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));
        
        if (i.getStatus() != InternshipStatus.PENDING) {
            throw new IllegalStateException("Only pending internships can be approved.");
        }
        
        i.approve();
        i.setVisible(makeVisible);
        internshipRepo.save(i);
    }

    public void rejectInternship(String staffId, String internshipId) {
        
        Internship i = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));
        
        if (i.getStatus() != InternshipStatus.PENDING) {
            throw new IllegalStateException("Only pending internships can be rejected.");
        }
        
        i.reject();
        internshipRepo.save(i);
    }

    public void approveCompanyRep(String staffId, String repEmail) {
        
        CompanyRep rep = repRepo.findByEmail(repEmail)
                .orElseThrow(() -> new IllegalArgumentException("Company representative not found: " + repEmail));
        
        if (rep.isApproved()) {
            throw new IllegalStateException("Company representative is already approved.");
        }
        
        rep.approve();
        repRepo.save(rep);
    }

    public void rejectCompanyRep(String staffId, String repEmail, String reason) {
        
        CompanyRep rep = repRepo.findByEmail(repEmail)
                .orElseThrow(() -> new IllegalArgumentException("Company representative not found: " + repEmail));
        
        if (rep.isRejected()) {
            throw new IllegalStateException("Company representative is already rejected.");
        }
        
        rep.reject(reason != null ? reason : "Rejected by Career Center staff");
        repRepo.save(rep);
    }

    public void approveWithdrawal(String staffId, String wrId, String note) {
        CareerCenterStaff staff = validateStaffExists(staffId);
        WithdrawalRequest wr = withdrawalRepo.findById(wrId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal request not found: " + wrId));

        if (wr.getStatus() != WithdrawalRequestStatus.PENDING) {
            throw new IllegalStateException("Withdrawal request is already processed.");
        }

        wr.approve(staff, note != null ? note : "Approved by Career Center staff");
        withdrawalRepo.save(wr);
        applicationRepo.save(wr.getApplication());
    }

    public void rejectWithdrawal(String staffId, String wrId, String note) {
        CareerCenterStaff staff = validateStaffExists(staffId);
        WithdrawalRequest wr = withdrawalRepo.findById(wrId)
                .orElseThrow(() -> new IllegalArgumentException("Withdrawal request not found: " + wrId));

        if (wr.getStatus() != WithdrawalRequestStatus.PENDING) {
            throw new IllegalStateException("Withdrawal request is already processed.");
        }

        wr.reject(staff, note != null ? note : "Rejected by Career Center staff");
        withdrawalRepo.save(wr);
    }
    
    public List<Internship> filterInternships(InternshipStatus status, String major, 
                                             String companyName, String level) {
        List<Internship> allInternships = internshipRepo.findAll();
        List<Internship> filtered = new ArrayList<>();
        
        for (Internship internship : allInternships) {
            boolean matches = true;
            
            if (status != null && internship.getStatus() != status) {
                matches = false;
            }
            if (major != null && !major.isEmpty() && 
                !internship.getPreferredMajor().equalsIgnoreCase(major)) {
                matches = false;
            }
            if (companyName != null && !companyName.isEmpty() &&
                !internship.getCompanyName().equalsIgnoreCase(companyName)) {
                matches = false;
            }
            if (level != null && !level.isEmpty() &&
                internship.getLevel() != InternshipLevel.valueOf(level.toUpperCase())) {
                matches = false;
            }
            
            if (matches) {
                filtered.add(internship);
            }
        }
        
        return filtered;
    }
    
    private CareerCenterStaff validateStaffExists(String staffId) {
        return staffRepo.findById(staffId)
                .orElseThrow(() -> new IllegalArgumentException("Staff member not found: " + staffId));
    }
}