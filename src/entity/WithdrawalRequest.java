package entity;

import java.time.LocalDate;
import java.util.Objects;

public class WithdrawalRequest {
    private static int idCounter = 1;

    private String requestId;                 // e.g., WRQ0001
    private InternshipApplication application;  // CHANGED: InternshipApplication instead of Application
    private Student requestedBy;              // must equal application.getStudent()
    private LocalDate requestedOn;

    private String reason;                          // optional free text, stored trimmed
    private WithdrawalRequestStatus status = WithdrawalRequestStatus.PENDING;

    // audit when processed by staff
    private CareerCenterStaff processedBy;          // null until processed
    private LocalDate processedOn;                  // null until processed
    private String staffNote;                       // optional

    public WithdrawalRequest(InternshipApplication application, Student requestedBy, String reason) {
        this.requestId   = String.format("WRQ%04d", idCounter++);
        this.application = Objects.requireNonNull(application, "application");
        this.requestedBy = Objects.requireNonNull(requestedBy, "requestedBy");
        if (application.getStudent() == null || !application.getStudent().equals(requestedBy)) {
            throw new IllegalArgumentException("Requester must be the owner of the application");
        }
        this.requestedOn = LocalDate.now();
        this.reason      = sanitize(reason);
    }

    // ---------- Queries ----------

    public String getRequestId()      { return requestId; }
    public InternshipApplication getApplication() { return application; }  // CHANGED return type
    public Student getRequestedBy()   { return requestedBy; }
    public LocalDate getRequestedOn() { return requestedOn; }
    public String getReason()         { return reason; }
    public WithdrawalRequestStatus getStatus() { return status; }
    public CareerCenterStaff getProcessedBy()  { return processedBy; }
    public LocalDate getProcessedOn()          { return processedOn; }
    public String getStaffNote()               { return staffNote; }
    public boolean isPending() { return status == WithdrawalRequestStatus.PENDING; }

    // ---------- Commands ----------

    /** Update student's own reason text before processing. */
    public void setReason(String reason) {
        ensurePending();
        this.reason = sanitize(reason);
    }

    /**
     * Approve the withdrawal and update linked aggregates atomically:
     * - If student had confirmed, free a slot.
     * - Else, mark application UNSUCCESSFUL so it no longer counts toward any caps.
     */
    public void approve(CareerCenterStaff staff, String note) {
        Objects.requireNonNull(staff, "staff");
        ensurePending();

        if (application.isStudentAccepted()) {
            // frees up a slot and unsets acceptance, keeps status SUCCESSFUL (offer existed)
            application.revokeAcceptanceAfterApprovedWithdrawal();
        } else {
            // If still PENDING (or SUCCESSFUL but not confirmed), remove from the active pool
            if (application.getStatus() == ApplicationStatus.PENDING ||
                application.getStatus() == ApplicationStatus.SUCCESSFUL) {
                application.markWithdrawn();
            }
        }

        this.status = WithdrawalRequestStatus.APPROVED;
        stamp(staff, note);
    }

    /** Reject the withdrawal; application remains unchanged. */
    public void reject(CareerCenterStaff staff, String note) {
        Objects.requireNonNull(staff, "staff");
        ensurePending();
        this.status = WithdrawalRequestStatus.REJECTED;
        stamp(staff, note);
    }

    // ---------- Helpers ----------

    private void ensurePending() {
        if (!isPending()) throw new IllegalStateException("Request already processed: " + status);
    }

    private void stamp(CareerCenterStaff staff, String note) {
        this.processedBy = staff;
        this.processedOn = LocalDate.now();
        this.staffNote   = sanitize(note);
    }

    private static String sanitize(String s) {
        if (s == null) return "";
        String t = s.trim();
        // Optional: cap very long notes to avoid accidental huge payloads
        return t.length() > 2000 ? t.substring(0, 2000) : t;
    }

    // ---------- Identity & debug ----------

//    @Override
//    public boolean equals(Object o) {
//        if (this == o) return true;
//        if (!(o instanceof WithdrawalRequest)) return false;
//        WithdrawalRequest that = (WithdrawalRequest) o;
//        return requestId.equals(that.requestId);
//    }
//
//    @Override
//    public int hashCode() { return requestId.hashCode(); }
//
//    @Override
//    public String toString() {
//        return "WithdrawalRequest{" +
//                "id='" + requestId + '\'' +
//                ", app=" + application.getApplicationId() +
//                ", by=" + requestedBy.getUserId() +
//                ", status=" + status +
//                '}';
//    }
}