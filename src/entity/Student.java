package entity;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * - Y1–Y2 eligible for BASIC only; Y3–Y4 eligible for BASIC/INTERMEDIATE/ADVANCED.
 * - Max 3 active applications (PENDING or SUCCESSFUL but not yet accepted).
 * - Exactly one confirmed placement per student.
 */
public class Student extends User {
    private String major;      
    private int yearOfStudy;   
    private String email;

    public static final int MAX_ACTIVE_APPLICATIONS = 3;

    public Student(String userId, String name, String major, int yearOfStudy, String email) {
        super(userId, name);
        this.yearOfStudy = yearOfStudy;
        this.major = major;
        this.email = email;
    }

    // -------------------- Queries --------------------

    public String getMajor() { return major; }
    
    public int getYearOfStudy() { return yearOfStudy; }
    
    public String getEmail() { return email; }

    /** Returns true if the student’s year permits applying to internships with the given level. */
    public boolean isEligibleFor(InternshipLevel level) {
        if (level == null) return false;
        if (yearOfStudy <= 2) {
            return level == InternshipLevel.BASIC;
        }
        return level == InternshipLevel.BASIC
            || level == InternshipLevel.INTERMEDIATE
            || level == InternshipLevel.ADVANCED;
    }

    /** Convenience filter for lists already fetched by a repo/service. */
    public List<Internship> filterEligibleVisibleOpen(List<Internship> all) {
        if (all == null) return List.of();
        return all.stream()
                  .filter(i -> i != null
                           && i.isOpenForApplications(LocalDate.now())
                           && i.isVisible()  // ← THIS FILTERS OUT NON-VISIBLE INTERNSHIPS
                           && isEligibleFor(i.getLevel()))
                  .collect(Collectors.toList());
    }

    /** Do I currently have a confirmed placement? (derived from applications service) */
    public boolean hasConfirmedPlacement(AppReadPort apps) {
        return apps.hasConfirmedPlacement(getUserId());
    }

    /** Number of active applications used toward cap. */
    public int activeApplicationsCount(AppReadPort apps) {
        return apps.countActiveApplications(getUserId());
    }

    /** Within the 3-application cap? */
    public boolean canStartAnotherApplication(AppReadPort apps) {
        return activeApplicationsCount(apps) < MAX_ACTIVE_APPLICATIONS;
    }

    // -------------------- Commands (domain validations only) --------------------

    /** Validate intent to apply for an internship (no persistence here). */
    public void assertCanApply(Internship internship, AppReadPort apps) {
        Objects.requireNonNull(internship, "Internship required");
        Objects.requireNonNull(apps, "AppReadPort required");

        if (!internship.isOpenForApplications(LocalDate.now()) || !internship.isVisible())
            throw new IllegalStateException("Internship is not open/visible");

        if (!isEligibleFor(internship.getLevel()))
            throw new IllegalStateException("Not eligible for level " + internship.getLevel());

        if (hasConfirmedPlacement(apps))
            throw new IllegalStateException("Already have a confirmed placement");

        if (!canStartAnotherApplication(apps))
            throw new IllegalStateException("Application cap reached (" + MAX_ACTIVE_APPLICATIONS + ")");
    }

    /** Validate intent to confirm a successful offer. */
    public void assertCanConfirmOffer(AppReadPort apps) {
        if (hasConfirmedPlacement(apps))
            throw new IllegalStateException("Cannot confirm: placement already confirmed");
    }

    // -------------------- Identity & debug --------------------
//
//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof Student)) return false;
//        Student that = (Student) o;
//        return Objects.equals(getUserId(), that.getUserId());
//    }
//
//    @Override
//    public int hashCode() {
//        return Objects.hash(getUserId());
//    }
//
//    @Override
//    public String toString() {
//        return "Student{id=" + getUserId() + ", name=" + getName() +
//               ", major=" + major + ", year=" + yearOfStudy + "}";
//    }

    // -------------------- Ports (read-only) --------------------
    /**
     * Minimal read-only application view the domain needs.
     * Implement this in your repository/service layer; inject into methods above.
     * Count should include PENDING and SUCCESSFUL(unaccepted), and exclude UNSUCCESSFUL and ACCEPTED.
     */
    public interface AppReadPort {
        int countActiveApplications(String studentId);
        boolean hasConfirmedPlacement(String studentId);
    }
}
