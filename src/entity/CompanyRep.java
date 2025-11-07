package entity;

import java.time.LocalDate;
import java.util.Objects;

/**
 * CompanyRep entity (domain-tier).
 */
public class CompanyRep extends User {

    public static final int MAX_POSTINGS = 5;

    private String companyName;
    private String department;
    private String position;
    private String email;

    // Registration status (controlled by staff actions)
    private boolean approved;             // must be true before using platform
    private String rejectionReason;       // last rejection reason (optional)

    public CompanyRep(String userId, String name,
                      String companyName, String department, String position, String email) {
        super(userId, name);
        this.companyName = requireText(companyName, "companyName");
        this.department  = requireText(department, "department");
        this.position    = requireText(position, "position");
        this.email       = requireText(email, "email");
        this.approved    = false;
        this.rejectionReason = "";
    }

    // ---------- Queries ----------
    public String getCompanyName() { return companyName; }
    public String getDepartment()  { return department; }
    public String getPosition()    { return position; }
    public String getEmail()       { return email; }
    public boolean isApproved()    { return approved; }
    public boolean isRejected()    { return !approved && rejectionReason != null && !rejectionReason.isEmpty(); }  // ADDED
    public String getRejectionReason() { return rejectionReason; }

    // ---------- Registration (called by staff service/controller) ----------
    public void approve() {
        this.approved = true;
        this.rejectionReason = "";
    }

    public void reject(String reason) {
        this.approved = false;
        this.rejectionReason = reason == null ? "" : reason.trim();
    }

    // ---------- Posting management ----------
    /** Ensure this rep has capacity to create another posting (<= MAX_POSTINGS). */
    public boolean canCreateAnotherPosting(RepPostingReadPort port) {
        Objects.requireNonNull(port, "port");
        return port.countActivePostingsForRep(getUserId()) < MAX_POSTINGS;
    }

    /** Create a new internship owned by this rep's company (throws if over cap or not approved). */
    public Internship createInternship(
            String title, String description, InternshipLevel level,
            String preferredMajor, LocalDate openDate, LocalDate closeDate,
            int maxSlots, RepPostingReadPort port) {

        if (!isApproved()) throw new IllegalStateException("Rep not approved");
        if (!canCreateAnotherPosting(port))
            throw new IllegalStateException("Posting cap reached (" + MAX_POSTINGS + ")");

        return new Internship(
                title, description, level, preferredMajor,
                openDate, closeDate, this.companyName, maxSlots
        );
    }

    /** Turn visibility on/off for OWN internship (only valid when APPROVED per Internship rules). */
    public void setInternshipVisibility(Internship internship, boolean visible) {
        ensureOwns(internship);
        internship.setVisible(visible);
    }

    /** Optional helper to hide/close a posting (without changing status). */
    public void closePosting(Internship internship) {
        ensureOwns(internship);
        if (internship.getStatus() == InternshipStatus.APPROVED) {
            internship.setVisible(false);
        }
    }
    

    // ---------- Application decisions (for OWN postings) ----------
    /** Mark a PENDING application SUCCESSFUL (offer). */
    public void approveApplication(InternshipApplication app) {
        ensureOwns(app.getInternship());
        app.markSuccessful();
    }

    /** Mark a PENDING application UNSUCCESSFUL (reject). */
    public void rejectApplication(InternshipApplication app) {
        ensureOwns(app.getInternship());
        app.markUnsuccessful();
    }
    

    // ---------- Ownership & validation ----------
    private void ensureOwns(Internship internship) {
        Objects.requireNonNull(internship, "internship");
        if (!this.companyName.equalsIgnoreCase(internship.getCompanyName())) {
            throw new IllegalArgumentException("Rep can only manage their own company's postings");
        }
    }

    private static String requireText(String s, String field) {
        if (s == null || s.isBlank()) throw new IllegalArgumentException(field + " required");
        return s.trim();
    }

//    // ---------- Identity & debug ----------
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof CompanyRep)) return false;
//        CompanyRep that = (CompanyRep) o;
//        return getUserId().equals(that.getUserId());
//    }
//
//    @Override
//    public int hashCode() { return getUserId().hashCode(); }
//
//    @Override
//    public String toString() {
//        return "CompanyRep{" +
//                "id='" + getUserId() + '\'' +
//                ", name='" + getName() + '\'' +
//                ", company='" + companyName + '\'' +
//                ", approved=" + approved +
//                '}';
//    }

    // ---------- Ports ----------
    /** Minimal read-port so the domain can ask "how many active postings do I have?" */
    public interface RepPostingReadPort {
        int countActivePostingsForRep(String repId);
    }
}