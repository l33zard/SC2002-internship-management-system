package entity;

import java.time.LocalDate;
import java.util.Objects;

/**
 * InternshipApplication aggregate (domain-tier).
 */
public class InternshipApplication {
    private static int idCounter = 1;

    private String applicationId;          // (mutable for repo loading)
    private final LocalDate appliedOn;
    private final Student student;

    // CHANGED: store durable internshipId for persistence/re-attachment
    private String internshipId;

    // CHANGED: make internship non-final so we can re-attach after load
    private Internship internship;         // may be null right after CSV load

    private boolean studentAccepted;
    private ApplicationStatus status;

    // ---------- Construction ----------

    public InternshipApplication(LocalDate appliedOn,
                                 Student student,
                                 Internship internship,
                                 Student.AppReadPort appReadPort) {
        this.applicationId = String.format("APP%04d", idCounter++);
        this.appliedOn = Objects.requireNonNull(appliedOn, "appliedOn");
        this.student = Objects.requireNonNull(student, "student");
        this.internship = Objects.requireNonNull(internship, "internship");
        this.internshipId = internship.getInternshipId();  // CHANGED: capture ID
        this.status = ApplicationStatus.PENDING;
        this.studentAccepted = false;

        // Creation-time domain checks (require internship object present at creation)
        if (!internship.isOpenForApplications(appliedOn)) {
            throw new IllegalStateException("Internship is not open/visible for applications");
        }
        if (!student.isEligibleFor(internship.getLevel())) {
            throw new IllegalStateException("Student not eligible for level " + internship.getLevel());
        }
        if (student.hasConfirmedPlacement(appReadPort)) {
            throw new IllegalStateException("Student already has a confirmed placement");
        }
        if (!student.canStartAnotherApplication(appReadPort)) {
            throw new IllegalStateException("Student reached application cap (" + Student.MAX_ACTIVE_APPLICATIONS + ")");
        }
    }

    public InternshipApplication(Student student,
                                 Internship internship,
                                 Student.AppReadPort appReadPort) {
        this(LocalDate.now(), student, internship, appReadPort);
    }

    // ---------- Queries ----------

    public String getApplicationId() { return applicationId; }
    public LocalDate getAppliedOn() { return appliedOn; }
    public Student getStudent() { return student; }

    /**
     * Durable link by ID (available even if internship object not attached).
     */
    public String getInternshipId() {
        if (internshipId != null) return internshipId;
        return (internship != null ? internship.getInternshipId() : null);
    }

    /**
     * May be null if not yet re-attached after loading from CSV.
     */
    public Internship getInternship() { return internship; }

    public ApplicationStatus getStatus() { return status; }
    public boolean isStudentAccepted() { return studentAccepted; }

    public boolean canAccept() {
        return status == ApplicationStatus.SUCCESSFUL && !studentAccepted;
    }

    public boolean isActiveTowardCap() {
        return (status == ApplicationStatus.PENDING) ||
               (status == ApplicationStatus.SUCCESSFUL && !studentAccepted);
    }

    // ---------- Transitions (rep/staff actions) ----------

    public void markSuccessful() {
        ensureStatus(ApplicationStatus.PENDING, "Only PENDING can be marked SUCCESSFUL");
        this.status = ApplicationStatus.SUCCESSFUL;
    }

    public void markUnsuccessful() {
        if (status == ApplicationStatus.SUCCESSFUL && studentAccepted) {
            throw new IllegalStateException("Cannot reject after student accepted");
        }
        ensureStatus(ApplicationStatus.PENDING, "Only PENDING can be marked UNSUCCESSFUL");
        this.status = ApplicationStatus.UNSUCCESSFUL;
    }

    public void markWithdrawn() {
        // Keeps semantics the same but simpler
        this.status = ApplicationStatus.WITHDRAWN;
    }

    // ---------- Transitions (student actions) ----------

    public void confirmAcceptance(Student.AppReadPort appReadPort) {
        if (!canAccept()) throw new IllegalStateException("Cannot accept in status " + status);
        if (student.hasConfirmedPlacement(appReadPort)) {
            throw new IllegalStateException("Student already has a confirmed placement");
        }
        // CHANGED: guard internship presence (must be attached by controller)
        if (internship == null) {
            throw new IllegalStateException("Internship details not attached; cannot confirm acceptance");
        }
        internship.incrementConfirmedSlots();
        this.studentAccepted = true;
    }

    public void revokeAcceptanceAfterApprovedWithdrawal() {
        if (!studentAccepted) throw new IllegalStateException("No accepted placement to revoke");
        if (internship == null) {
            throw new IllegalStateException("Internship details not attached; cannot revoke acceptance");
        }
        internship.decrementConfirmedSlots();
        this.studentAccepted = false;
    }

    // ---------- ID & Re-attachment for Persistence ----------

    /**
     * Set application ID (for repository loading only)
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * Set internshipId when loading from CSV (if needed).
     */
    public void setInternshipId(String internshipId) {
        this.internshipId = internshipId;
    }

    /**
     * Re-attach the current Internship object by controller after repositories load.
     * Safe to call with null (detaches).
     */
    public void setInternship(Internship internship) {
        this.internship = internship;
        if (internship != null) {
            this.internshipId = internship.getInternshipId(); // keep in sync
        }
    }

    // ---------- Helpers ----------

    private void ensureStatus(ApplicationStatus expected, String msg) {
        if (this.status != expected) throw new IllegalStateException(msg + " (current=" + this.status + ")");
    }
//
//    // ---------- Identity & debug ----------
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof InternshipApplication)) return false;
//        return applicationId.equals(((InternshipApplication) o).applicationId);
//    }
//
//    @Override
//    public int hashCode() { return applicationId.hashCode(); }
//
//    @Override
//    public String toString() {
//        return "Application{" +
//               "id='" + applicationId + '\'' +
//               ", student=" + student.getUserId() +
//               ", internshipId=" + getInternshipId() +
//               ", status=" + status +
//               ", accepted=" + studentAccepted +
//               '}';
//    }
}
