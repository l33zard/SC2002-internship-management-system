package controller;

import database.CompanyRepRepository;
import entity.CompanyRep;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class CompanyRepRegistrationController extends BaseController {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public CompanyRepRegistrationController(CompanyRepRepository reps) {
        super(null, reps, null, null, null, null);
    }

    public String registerCompanyRep(String name, String companyName, String department,
                                     String position, String email) {
        validateRegistrationFields(name, companyName, department, position, email);
        
        if (repRepo.existsById(email)) {
            throw new IllegalStateException("Email already registered. Please use a different email address.");
        }

        CompanyRep rep = new CompanyRep(
            email.trim().toLowerCase(), 
            name.trim(), 
            companyName.trim(), 
            department.trim(), 
            position.trim(), 
            email.trim().toLowerCase()
        );
        repRepo.save(rep);
        return rep.getEmail();
    }
    
    public boolean emailExists(String email) {
        return repRepo.findByEmail(email).isPresent();
    }
    
    public List<CompanyRep> getPendingReps() {
        return repRepo.findAll().stream()
                .filter(rep -> !rep.isApproved() && !rep.isRejected())
                .collect(Collectors.toList());
    }
    
    public CompanyRep getRepByEmail(String email) {
        return repRepo.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Company representative not found: " + email));
    }
    
    private void validateRegistrationFields(String name, String companyName, String department,
                                          String position, String email) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name is required");
        }
        if (companyName == null || companyName.trim().isEmpty()) {
            throw new IllegalArgumentException("Company name is required");
        }
        if (department == null || department.trim().isEmpty()) {
            throw new IllegalArgumentException("Department is required");
        }
        if (position == null || position.trim().isEmpty()) {
            throw new IllegalArgumentException("Position is required");
        }
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }
        
        if (!isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
    }
    
    private boolean isValidEmail(String email) {
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}