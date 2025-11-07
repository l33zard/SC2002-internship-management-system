package controller;

import database.ApplicationRepository;
import database.CompanyRepRepository;
import database.InternshipRepository;
import entity.CompanyRep;
import entity.Internship;
import entity.InternshipApplication;
import entity.InternshipLevel;
import entity.InternshipStatus;

import java.time.LocalDate;
import java.util.List;

public class CompanyRepController extends BaseController {
    private static final int MAX_POSTINGS_PER_REP = 5;
    private static final int MAX_SLOTS = 10;

    public CompanyRepController(CompanyRepRepository reps,
                                InternshipRepository internships,
                                ApplicationRepository applications) {
        super(null, reps, null, internships, applications, null);
    }

    public List<Internship> listInternships(String repEmail) {
        CompanyRep rep = validateRepExists(repEmail);
        return internshipRepo.findByCompanyName(rep.getCompanyName());
    }
    
    public Internship getInternship(String repEmail, String internshipId) {
        CompanyRep rep = validateRepExists(repEmail);
        Internship internship = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));
        
        validateOwnership(rep, internship);
        return internship;
    }

    public String createInternship(String repEmail,
                                   String title, String description, InternshipLevel level,
                                   String preferredMajor, LocalDate openDate, LocalDate closeDate,
                                   int maxSlots) {
        CompanyRep rep = validateRepExists(repEmail);
        
        validateInternshipParams(title, description, level, preferredMajor, openDate, closeDate, maxSlots);

        long activeCount = internshipRepo.findByCompanyName(rep.getCompanyName()).stream()
                .filter(i -> i.getStatus() == InternshipStatus.PENDING
                          || i.getStatus() == InternshipStatus.APPROVED)
                .count();
        if (activeCount >= MAX_POSTINGS_PER_REP) {
            throw new IllegalStateException("You have reached the limit of "
                    + MAX_POSTINGS_PER_REP + " active internship postings.");
        }

        Internship i = new Internship(
                title, description, level, preferredMajor,
                openDate, closeDate, rep.getCompanyName(), maxSlots
        );
        internshipRepo.save(i);
        return i.getInternshipId();
    }
    
    public void editInternship(String repEmail, String internshipId,
                              String title, String description, InternshipLevel level,
                              String preferredMajor, LocalDate openDate, LocalDate closeDate,
                              int maxSlots) {
        CompanyRep rep = validateRepExists(repEmail);
        Internship internship = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));
        
        validateOwnership(rep, internship);
        
        if (!internship.isEditable()) {
            throw new IllegalStateException("Cannot edit internship that has been approved or rejected.");
        }
        
        validateInternshipParams(title, description, level, preferredMajor, openDate, closeDate, maxSlots);
        
        internshipRepo.deleteById(internshipId);
        
        Internship updatedInternship = new Internship(
                title, description, level, preferredMajor,
                openDate, closeDate, rep.getCompanyName(), maxSlots
        );
        internshipRepo.save(updatedInternship);
    }
    
    public void deleteInternship(String repEmail, String internshipId) {
        CompanyRep rep = validateRepExists(repEmail);
        Internship internship = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));
        
        validateOwnership(rep, internship);
        
        if (!internship.canBeDeleted()) {
            throw new IllegalStateException("Cannot delete internship that has been approved.");
        }
        
        internshipRepo.deleteById(internshipId);
    }

    public void setVisibility(String repEmail, String internshipId, boolean visible) {
        CompanyRep rep = validateRepExists(repEmail);
        Internship i = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));

        validateOwnership(rep, i);

        i.setVisible(visible);
        internshipRepo.save(i);
    }

    public List<InternshipApplication> listApplications(String repEmail, String internshipId) {
        CompanyRep rep = validateRepExists(repEmail);
        Internship i = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));

        validateOwnership(rep, i);
        return applicationRepo.findByInternship(internshipId);
    }

    public void markApplicationSuccessful(String repEmail, String applicationId) {
        CompanyRep rep = validateRepExists(repEmail);
        InternshipApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        validateOwnership(rep, app.getInternship());

        app.markSuccessful();
        applicationRepo.save(app);
    }

    public void markApplicationUnsuccessful(String repEmail, String applicationId) {
        CompanyRep rep = validateRepExists(repEmail);
        InternshipApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        validateOwnership(rep, app.getInternship());

        app.markUnsuccessful();
        applicationRepo.save(app);
    }
    
    private CompanyRep validateRepExists(String repEmail) {
        return repRepo.findByEmail(repEmail)
                .orElseThrow(() -> new IllegalArgumentException("Company representative not found: " + repEmail));
    }
    
    private void validateOwnership(CompanyRep rep, Internship internship) {
        if (!rep.getCompanyName().equalsIgnoreCase(internship.getCompanyName())) {
            throw new SecurityException("Cannot manage another company's posting");
        }
    }
    
    private void validateInternshipParams(String title, String description, InternshipLevel level, String preferredMajor, LocalDate openDate, LocalDate closeDate,int maxSlots) {
		if (title == null || title.trim().isEmpty()) {
			throw new IllegalArgumentException("Title is required");
		}
		if (description == null || description.trim().isEmpty()) {
			throw new IllegalArgumentException("Description is required");
		}
		if (level == null) {
			throw new IllegalArgumentException("Internship level is required");
		}
		if (preferredMajor == null || preferredMajor.trim().isEmpty()) {
			throw new IllegalArgumentException("Preferred major is required");
		}
		if (openDate == null) {
			throw new IllegalArgumentException("Open date is required");
		}
		if (closeDate == null) {
			throw new IllegalArgumentException("Close date is required");
		}
		if (maxSlots < 1 || maxSlots > MAX_SLOTS) {
			throw new IllegalArgumentException("Max slots must be between 1 and " + MAX_SLOTS);
		}
		if (closeDate.isBefore(openDate)) {
			throw new IllegalArgumentException("Close date cannot be before open date");
		}
	}
}