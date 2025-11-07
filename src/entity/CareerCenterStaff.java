package entity;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * CareerCenterStaff entity.
 * 
 * Responsibilities (domain layer):
 * - Approve/reject company representative registrations.
 * - Approve/reject internship postings.
 * - Process withdrawal requests (approve/reject).
 * - Generate filtered reports of internships/applications.
 * 
 * Pure domain methods only: no persistence, no UI.
 */
public class CareerCenterStaff extends User {
    private String department;
    private String email;
    private String role;

    public CareerCenterStaff(String userId, String name, String role, String department, String email) {
        super(userId, name);
        this.role = requireText(role, "role");
        this.department = requireText(department, "department");
        this.email = requireText(email, "email");
    }

    public String getRole() { return role; }
    public String getDepartment() { return department; }
    public String getEmail() { return email; }

    // ---------------- Core Capabilities ----------------

    /** Approve a company representative account. */
    public void approveCompanyRep(CompanyRep rep) {
        Objects.requireNonNull(rep, "rep");
        rep.approve();
    }

    /** Reject a company representative account. */
    public void rejectCompanyRep(CompanyRep rep, String reason) {
        Objects.requireNonNull(rep, "rep");
        rep.reject(reason);
    }

    /** Approve an internship posting (makes it visible-ready). */
    public void approveInternship(Internship internship) {
        Objects.requireNonNull(internship, "internship");
        internship.approve();       // uses its own guard to prevent double approval
        internship.setVisible(true); // typically turned on right away
    }

    /** Reject an internship posting. */
    public void rejectInternship(Internship internship) {
        Objects.requireNonNull(internship, "internship");
        internship.reject();
    }

    /** Process a withdrawal request: approve or reject. */
    public void approveWithdrawal(WithdrawalRequest req, boolean approve, String note) {
        Objects.requireNonNull(req, "req");
        if (approve) req.approve(this, note);
        else req.reject(this, note);
    }

    /**
     * Example reporting helper:
     * filter internships by status/major/level/company.
     */
    public List<Internship> filterInternships(
            List<Internship> all,
            InternshipStatus status,
            String major,
            InternshipLevel level,
            String companyName
    ) {
        if (all == null) return List.of();
        return all.stream()
                .filter(i -> (status == null || i.getStatus() == status))
                .filter(i -> (major == null || i.getPreferredMajor().equalsIgnoreCase(major)))
                .filter(i -> (level == null || i.getLevel() == level))
                .filter(i -> (companyName == null || i.getCompanyName().equalsIgnoreCase(companyName)))
                .collect(Collectors.toList());
    }

    // ---------------- Helpers ----------------

    private static String requireText(String s, String field) {
        if (s == null || s.isBlank())
            throw new IllegalArgumentException(field + " required");
        return s.trim();
    }

//    @Override
//    public String toString() {
//        return "CareerCenterStaff{" +
//                "id='" + getUserId() + '\'' +
//                ", name='" + getName() + '\'' +
//                ", role='" + role + '\'' +
//                ", dept='" + department + '\'' +
//                '}';
//    }
}