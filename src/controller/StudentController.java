package controller;

import database.ApplicationRepository;
import database.InternshipRepository;
import database.StudentRepository;
import database.WithdrawalRequestRepository;
import entity.*;

import java.time.LocalDate;
import java.util.List;
//import java.util.Objects;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class StudentController extends BaseController {

    public StudentController(StudentRepository students,
                             InternshipRepository internships,
                             ApplicationRepository applications,
                             WithdrawalRequestRepository withdrawals) {
        super(students, null, null, internships, applications, withdrawals);
    }

    public Student getStudent(String studentId) {
        return studentRepo.findById(studentId)
                .orElseThrow(() -> new IllegalArgumentException("Student not found: " + studentId));
    }

    public List<Internship> viewEligibleInternships(String studentId) {
        Student s = getStudent(studentId);
        List<Internship> allInternships = internshipRepo.findAll();
        List<Internship> eligibleInternships = new ArrayList<>();
        
        List<String> appliedInternshipIds = applicationRepo.findByStudent(studentId).stream()
                .map(app -> app.getInternship().getInternshipId())
                .collect(Collectors.toList());
        
        for (Internship internship : allInternships) {
            if (appliedInternshipIds.contains(internship.getInternshipId())) {
                eligibleInternships.add(internship);
            } else if (internship.isOpenForApplications(LocalDate.now()) && 
                       internship.isVisible() && 
                       s.isEligibleFor(internship.getLevel())) {
                eligibleInternships.add(internship);
            }
        }
        
        return eligibleInternships;
    }

    public List<Internship> viewFilteredInternships(String studentId, String major, 
                                                   InternshipLevel level, String companyName) {
        Student s = getStudent(studentId);
        List<Internship> eligible = s.filterEligibleVisibleOpen(internshipRepo.findAll());
        
        return eligible.stream()
                .filter(internship -> major == null || internship.getPreferredMajor().equalsIgnoreCase(major))
                .filter(internship -> level == null || internship.getLevel() == level)
                .filter(internship -> companyName == null || internship.getCompanyName().equalsIgnoreCase(companyName))
                .toList();
    }

    public String applyForInternship(String studentId, String internshipId) {
        Student s = getStudent(studentId);
        Internship i = internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));

        if (applicationRepo.existsByStudentAndInternship(studentId, internshipId)) {
            throw new IllegalStateException("You have already applied to this internship.");
        }

        s.assertCanApply(i, applicationRepo);
        InternshipApplication app = new InternshipApplication(LocalDate.now(), s, i, applicationRepo);
        applicationRepo.save(app);
        return app.getApplicationId();
    }

    public void confirmAcceptance(String studentId, String applicationId) {
        var app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));

        if (app.getStudent() == null || !studentId.equals(app.getStudent().getUserId())) {
            throw new SecurityException("You can only accept your own application.");
        }
        if (app.getStatus() != ApplicationStatus.SUCCESSFUL) {
            throw new IllegalStateException("Only SUCCESSFUL applications can be accepted.");
        }
        if (app.isStudentAccepted()) {
            throw new IllegalStateException("This offer is already accepted.");
        }

        app.confirmAcceptance(applicationRepo);
        applicationRepo.save(app);

        var others = applicationRepo.findByStudent(studentId);
        for (var other : others) {
            if (other.getApplicationId().equals(applicationId)) continue;
            var st = other.getStatus();
            if (st == ApplicationStatus.PENDING || st == ApplicationStatus.SUCCESSFUL) {
                other.markWithdrawn();
                applicationRepo.save(other);
            }
        }
    }

    public String requestWithdrawal(String studentId, String applicationId, String reason) {
        InternshipApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        
        if (!app.getStudent().getUserId().equals(studentId)) {
            throw new SecurityException("Cannot withdraw another student's application");
        }
        
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Withdrawal reason is required.");
        }
        
        var existingRequest = withdrawalRepo.findByApplicationId(applicationId);
        if (existingRequest.isPresent() && existingRequest.get().isPending()) {
            throw new IllegalStateException("There is already a pending withdrawal request for this application.");
        }

        WithdrawalRequest wr = new WithdrawalRequest(app, app.getStudent(), reason);
        withdrawalRepo.save(wr);
        return wr.getRequestId();
    }

    public List<InternshipApplication> viewMyApplications(String studentId) {
        List<InternshipApplication> apps = applicationRepo.findByStudent(studentId);
        for (InternshipApplication app : apps) {
            String itId = app.getInternshipId();
            Internship it = internshipRepo.findById(itId).orElse(null);
            app.setInternship(it);
        }
        return apps;
    }

    public List<WithdrawalRequest> viewMyWithdrawalRequests(String studentId) {
        return withdrawalRepo.findByStudent(studentId);
    }

    public InternshipApplication getApplication(String studentId, String applicationId) {
        InternshipApplication app = applicationRepo.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Application not found: " + applicationId));
        
        if (!app.getStudent().getUserId().equals(studentId)) {
            throw new SecurityException("Cannot access another student's application");
        }
        
        return app;
    }

    public boolean canApplyMore(String studentId) {
        Student s = getStudent(studentId);
        return s.canStartAnotherApplication(applicationRepo);
    }

    public int getActiveApplicationCount(String studentId) {
        Student s = getStudent(studentId);
        return s.activeApplicationsCount(applicationRepo);
    }

    public boolean hasConfirmedPlacement(String studentId) {
        Student s = getStudent(studentId);
        return s.hasConfirmedPlacement(applicationRepo);
    }

    public Internship getInternship(String internshipId) {
        return internshipRepo.findById(internshipId)
                .orElseThrow(() -> new IllegalArgumentException("Internship not found: " + internshipId));
    }
}