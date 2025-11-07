package controller;

import database.CareerCenterStaffRepository;
import database.CompanyRepRepository;
import database.StudentRepository;
import entity.CareerCenterStaff;
import entity.CompanyRep;
import entity.Student;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AuthController extends BaseController {

    public enum Role { STUDENT, COMPANY_REP, STAFF }

    private final Map<String, String> pw = new ConcurrentHashMap<>();

    public AuthController() {
        super(null, null, null, null, null, null);
    }

    public AuthController(StudentRepository students,
                         CompanyRepRepository reps,
                         CareerCenterStaffRepository staff) {
        super(students, reps, staff, null, null, null);
        // Call seedDefaults after super() to ensure repositories are set
        seedDefaults(students, reps, staff);
    }

    public void seedDefaults(StudentRepository students,
                             CompanyRepRepository reps,
                             CareerCenterStaffRepository staff) {
        // These repositories are now available through the base class fields
        if (studentRepo != null) {
            for (Student s : studentRepo.findAll()) {
                String key = key(Role.STUDENT, s.getUserId());
                pw.putIfAbsent(key, "password");
            }
        }
        if (repRepo != null) {
            for (CompanyRep r : repRepo.findAll()) {
                String key = key(Role.COMPANY_REP, normEmail(r.getEmail()));
                pw.putIfAbsent(key, "password");
            }
        }
        if (staffRepo != null) {
            for (CareerCenterStaff st : staffRepo.findAll()) {
                String key = key(Role.STAFF, st.getUserId());
                pw.putIfAbsent(key, "password");
            }
        }
    }

    public String login(String loginKey, String password) {
        requireRepos();
        
        if (loginKey == null || loginKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Login ID/email cannot be empty.");
        }
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        String keyRaw = loginKey.trim();
        String pass   = password;

        if (keyRaw.contains("@")) {
            String emailNorm = normEmail(keyRaw);
            CompanyRep rep = repRepo.findByEmail(emailNorm)
                    .orElseThrow(() -> new IllegalArgumentException("Company representative not found with email: " + keyRaw));
            if (!rep.isApproved()) {
                throw new IllegalArgumentException("Account pending approval by Career Center staff.");
            }
            String authKey = key(Role.COMPANY_REP, emailNorm);
            String expected = pw.getOrDefault(authKey, "password");
            if (!Objects.equals(expected, pass)) throw new IllegalArgumentException("Incorrect password.");
            return Role.COMPANY_REP + ":" + rep.getEmail();
        }

        Optional<Student> os = studentRepo.findById(keyRaw);
        if (os.isPresent()) {
            String authKey = key(Role.STUDENT, os.get().getUserId());
            String expected = pw.getOrDefault(authKey, "password");
            if (!Objects.equals(expected, pass)) throw new IllegalArgumentException("Incorrect password.");
            return Role.STUDENT + ":" + os.get().getUserId();
        }

        Optional<CareerCenterStaff> ost = staffRepo.findById(keyRaw);
        if (ost.isPresent()) {
            String authKey = key(Role.STAFF, ost.get().getUserId());
            String expected = pw.getOrDefault(authKey, "password");
            if (!Objects.equals(expected, pass)) throw new IllegalArgumentException("Incorrect password.");
            return Role.STAFF + ":" + ost.get().getUserId();
        }

        throw new IllegalArgumentException("Invalid login credentials. Please check your ID/email and password.");
    }

    public void changePassword(String loginKey, String oldPw, String newPw) {
        requireRepos();
        
        if (loginKey == null || loginKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Login ID/email cannot be empty.");
        }
        if (oldPw == null || oldPw.isEmpty()) {
            throw new IllegalArgumentException("Current password cannot be empty.");
        }
        if (newPw == null || newPw.isEmpty()) {
            throw new IllegalArgumentException("New password cannot be empty.");
        }
        if (newPw.equals("password")) {
            throw new IllegalArgumentException("New password cannot be the default password.");
        }

        String keyRaw = loginKey.trim();

        if (keyRaw.contains("@")) {
            String emailNorm = normEmail(keyRaw);
            repRepo.findByEmail(emailNorm).orElseThrow(() -> new IllegalArgumentException("Company representative not found: " + keyRaw));
            String authKey = key(Role.COMPANY_REP, emailNorm);
            verifyAndSet(authKey, oldPw, newPw);
            return;
        }

        if (studentRepo.findById(keyRaw).isPresent()) {
            String authKey = key(Role.STUDENT, keyRaw);
            verifyAndSet(authKey, oldPw, newPw);
            return;
        }

        if (staffRepo.findById(keyRaw).isPresent()) {
            String authKey = key(Role.STAFF, keyRaw);
            verifyAndSet(authKey, oldPw, newPw);
            return;
        }

        throw new IllegalArgumentException("User not found: " + keyRaw);
    }
    
    public boolean verify(String loginKey, String password) {
        requireRepos();
        
        if (loginKey == null || loginKey.trim().isEmpty() || password == null) {
            return false;
        }

        String keyRaw = loginKey.trim();
        String pass   = password;

        if (keyRaw.contains("@")) {
            String emailNorm = normEmail(keyRaw);
            if (repRepo.findByEmail(emailNorm).isEmpty()) {
                return false;
            }
            String authKey = key(Role.COMPANY_REP, emailNorm);
            String expected = pw.getOrDefault(authKey, "password");
            return Objects.equals(expected, pass);
        }

        if (studentRepo.findById(keyRaw).isPresent()) {
            String authKey = key(Role.STUDENT, keyRaw);
            String expected = pw.getOrDefault(authKey, "password");
            return Objects.equals(expected, pass);
        }

        if (staffRepo.findById(keyRaw).isPresent()) {
            String authKey = key(Role.STAFF, keyRaw);
            String expected = pw.getOrDefault(authKey, "password");
            return Objects.equals(expected, pass);
        }

        return false;
    }

    private void verifyAndSet(String authKey, String oldPw, String newPw) {
        String current = pw.getOrDefault(authKey, "password");
        if (!Objects.equals(current, oldPw)) throw new IllegalArgumentException("Current password is incorrect.");
        pw.put(authKey, newPw);
    }

    private void requireRepos() {
        if (studentRepo == null || repRepo == null || staffRepo == null) {
            throw new IllegalStateException("Authentication system not initialized. Please contact administrator.");
        }
    }

    private String key(Role r, String id) {
        return r.name() + ":" + ((id == null) ? "" : id.trim());
    }

    private String normEmail(String email) {
        return (email == null) ? "" : email.trim().toLowerCase();
    }
    
    public Role getUserRole(String loginKey) {
        requireRepos();
        
        if (loginKey == null || loginKey.trim().isEmpty()) {
            return null;
        }

        String keyRaw = loginKey.trim();

        if (keyRaw.contains("@")) {
            String emailNorm = normEmail(keyRaw);
            if (repRepo.findByEmail(emailNorm).isPresent()) {
                return Role.COMPANY_REP;
            }
        }

        if (studentRepo.findById(keyRaw).isPresent()) {
            return Role.STUDENT;
        }

        if (staffRepo.findById(keyRaw).isPresent()) {
            return Role.STAFF;
        }

        return null;
    }
}