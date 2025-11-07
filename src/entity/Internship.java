package entity;

import java.time.LocalDate;

/**
 * Internship entity representing a posting created by a company.
 */
public class Internship {

    private static int idCounter = 1;  // ADD THIS

    private final String internshipId;
    private String title;
    private String description;
    private InternshipLevel level;
    private String preferredMajor;
    private LocalDate openDate;
    private LocalDate closeDate;
    private String companyName;
    private int maxSlots;
    private int confirmedSlots = 0;
    private boolean visible = false;
    private InternshipStatus status = InternshipStatus.PENDING;

    /* ===================== Constructors ===================== */

    public Internship(String title,
                      String description,
                      InternshipLevel level,
                      String preferredMajor,
                      LocalDate openDate,
                      LocalDate closeDate,
                      String companyName,
                      int maxSlots) {

        this.internshipId = "INT" + String.format("%04d", idCounter++);  // CHANGED THIS
        this.title = title;
        this.description = description;
        this.level = level;
        this.preferredMajor = preferredMajor;
        this.openDate = openDate;
        this.closeDate = closeDate;
        this.companyName = companyName;
        this.maxSlots = maxSlots;
    }

    // REST OF YOUR EXISTING METHODS REMAIN EXACTLY THE SAME...
    public String getInternshipId() { return internshipId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public InternshipLevel getLevel() { return level; }
    public String getPreferredMajor() { return preferredMajor; }
    public LocalDate getOpenDate() { return openDate; }
    public LocalDate getCloseDate() { return closeDate; }
    public String getCompanyName() { return companyName; }
    public int getMaxSlots() { return maxSlots; }
    public int getConfirmedSlots() { return confirmedSlots; }
    public boolean isVisible() { return visible; }
    public InternshipStatus getStatus() { return status; }

    public void approve() {
        if (status == InternshipStatus.APPROVED) return;
        status = InternshipStatus.APPROVED;
    }

    public void reject() {
        status = InternshipStatus.REJECTED;
        visible = false;
    }

    public void setVisible(boolean visible) {
        if (status != InternshipStatus.APPROVED && visible) {
            throw new IllegalStateException("Only approved internships can be made visible.");
        }
        this.visible = visible;
    }

    public boolean isOpenForApplications(LocalDate now) {
        if (status != InternshipStatus.APPROVED || !visible) return false;
        return (now != null && !now.isBefore(openDate) && !now.isAfter(closeDate) && confirmedSlots < maxSlots);
    }

    public void incrementConfirmedSlots() {
        if (confirmedSlots >= maxSlots) {
            status = InternshipStatus.FILLED;
            throw new IllegalStateException("No remaining slots.");
        }
        confirmedSlots++;
        if (confirmedSlots >= maxSlots) {
            status = InternshipStatus.FILLED;
        }
    }

    public void decrementConfirmedSlots() {
        if (confirmedSlots > 0) confirmedSlots--;
        if (status == InternshipStatus.FILLED && confirmedSlots < maxSlots) {
            status = InternshipStatus.APPROVED;
        }
    }

    public boolean isEditable() {
        return status == InternshipStatus.PENDING;
    }

    public boolean canBeDeleted() {
        return status == InternshipStatus.PENDING || status == InternshipStatus.REJECTED;
    }

//    @Override
//    public String toString() {
//        return String.format("%s [%s] %s (%s → %s) status=%s visible=%s slots=%d/%d",
//                title, level, companyName, openDate, closeDate, status, visible, confirmedSlots, maxSlots);
//    }
}