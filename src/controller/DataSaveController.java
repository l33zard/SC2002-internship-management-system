package controller;

import database.*;
import java.io.IOException;
import java.nio.file.*;

public class DataSaveController extends BaseController {

    private final Path baseDir;

    public DataSaveController(StudentRepository students,
                              CompanyRepRepository reps,
                              CareerCenterStaffRepository staff,
                              InternshipRepository internships,
                              ApplicationRepository applications,
                              WithdrawalRequestRepository withdrawals) {
        this(students, reps, staff, internships, applications, withdrawals, Paths.get("src", "data"));
    }

    public DataSaveController(StudentRepository students,
                              CompanyRepRepository reps,
                              CareerCenterStaffRepository staff,
                              InternshipRepository internships,
                              ApplicationRepository applications,
                              WithdrawalRequestRepository withdrawals,
                              Path baseDir) {
        super(students, reps, staff, internships, applications, withdrawals);
        this.baseDir = baseDir == null ? Paths.get("src", "data") : baseDir;

        try {
            Files.createDirectories(this.baseDir);
        } catch (IOException e) {
            System.err.println("Warning: Could not create data directory: " + e.getMessage());
        }
    }

    public void saveAll(String studentsCsv,
                        String repsCsv,
                        String staffCsv,
                        String internshipsCsv,
                        String applicationsCsv,
                        String withdrawalsCsv) throws IOException {

        boolean hasErrors = false;
        StringBuilder errorMessages = new StringBuilder();

        try {
            Path sPath = baseDir.resolve(studentsCsv);
            studentRepo.saveToCsv(sPath.toString());
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to save students: ").append(e.getMessage()).append("\n");
        }

        try {
            Path rPath = baseDir.resolve(repsCsv);
            repRepo.saveToCsv(rPath.toString());
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to save company reps: ").append(e.getMessage()).append("\n");
        }

        try {
            Path stPath = baseDir.resolve(staffCsv);
            staffRepo.saveToCsv(stPath.toString());
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to save staff: ").append(e.getMessage()).append("\n");
        }

        try {
            Path iPath = baseDir.resolve(internshipsCsv);
            internshipRepo.saveToCsv(iPath.toString());
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to save internships: ").append(e.getMessage()).append("\n");
        }

        try {
            Path aPath = baseDir.resolve(applicationsCsv);
            applicationRepo.saveToCsv(aPath.toString());
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to save applications: ").append(e.getMessage()).append("\n");
        }

        try {
            Path wPath = baseDir.resolve(withdrawalsCsv);
            withdrawalRepo.saveToCsv(wPath.toString());
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to save withdrawal requests: ").append(e.getMessage()).append("\n");
        }

        if (hasErrors) {
            throw new IOException("Some data failed to save:\n" + errorMessages.toString());
        }
    }

    public void loadAll(String studentsCsv,
                        String repsCsv,
                        String staffCsv,
                        String internshipsCsv,
                        String applicationsCsv,
                        String withdrawalsCsv) throws IOException {

        boolean hasErrors = false;
        StringBuilder errorMessages = new StringBuilder();

        try {
            Path sPath = baseDir.resolve(studentsCsv);
            if (Files.exists(sPath)) {
                studentRepo.loadFromCsv(sPath.toString());
            } else {
                System.out.println("Students file not found: " + sPath);
            }
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to load students: ").append(e.getMessage()).append("\n");
        }

        try {
            Path rPath = baseDir.resolve(repsCsv);
            if (Files.exists(rPath)) {
                repRepo.loadFromCsv(rPath.toString());
            } else {
                System.out.println("Company reps file not found: " + rPath);
            }
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to load company reps: ").append(e.getMessage()).append("\n");
        }

        try {
            Path stPath = baseDir.resolve(staffCsv);
            if (Files.exists(stPath)) {
                staffRepo.loadFromCsv(stPath.toString());
            } else {
                System.out.println("Staff file not found: " + stPath);
            }
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to load staff: ").append(e.getMessage()).append("\n");
        }

        try {
            Path iPath = baseDir.resolve(internshipsCsv);
            if (Files.exists(iPath)) {
                internshipRepo.loadFromCsv(iPath.toString());
            } else {
                System.out.println("Internships file not found: " + iPath);
            }
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to load internships: ").append(e.getMessage()).append("\n");
        }

        try {
            Path aPath = baseDir.resolve(applicationsCsv);
            if (Files.exists(aPath)) {
                applicationRepo.loadFromCsv(aPath.toString(), studentRepo, internshipRepo);
            } else {
                System.out.println("Applications file not found: " + aPath);
            }
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to load applications: ").append(e.getMessage()).append("\n");
        }

        try {
            Path wPath = baseDir.resolve(withdrawalsCsv);
            if (Files.exists(wPath)) {
                withdrawalRepo.loadFromCsv(wPath.toString(), applicationRepo, studentRepo, staffRepo);
            } else {
                System.out.println("Withdrawal requests file not found: " + wPath);
            }
        } catch (Exception e) {
            hasErrors = true;
            errorMessages.append("Failed to load withdrawal requests: ").append(e.getMessage()).append("\n");
        }

        if (hasErrors) {
            throw new IOException("Some data failed to load:\n" + errorMessages.toString());
        }
    }

    public Path getBaseDir() {
        return baseDir;
    }

    public boolean isInitialized() {
        return studentRepo != null && repRepo != null && staffRepo != null && 
               internshipRepo != null && applicationRepo != null && withdrawalRepo != null;
    }
}