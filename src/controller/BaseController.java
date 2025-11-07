package controller;

import database.*;

public abstract class BaseController {
    protected final StudentRepository studentRepo;
    protected final CompanyRepRepository repRepo;
    protected final CareerCenterStaffRepository staffRepo;
    protected final InternshipRepository internshipRepo;
    protected final ApplicationRepository applicationRepo;
    protected final WithdrawalRequestRepository withdrawalRepo;
    
    public BaseController(StudentRepository studentRepo,
                         CompanyRepRepository repRepo,
                         CareerCenterStaffRepository staffRepo,
                         InternshipRepository internshipRepo,
                         ApplicationRepository applicationRepo,
                         WithdrawalRequestRepository withdrawalRepo) {
        this.studentRepo = studentRepo;
        this.repRepo = repRepo;
        this.staffRepo = staffRepo;
        this.internshipRepo = internshipRepo;
        this.applicationRepo = applicationRepo;
        this.withdrawalRepo = withdrawalRepo;
    }
    
    protected boolean isEmailValid(String email) {
        return email != null && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }
    
    protected boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }
    
    protected void validateNotNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
    }
}