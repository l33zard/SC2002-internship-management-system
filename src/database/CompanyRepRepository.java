package database;

import entity.CompanyRep;
import util.CsvUtils;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * In-memory repository for CompanyRep.
 * Works with CSV schema:
 * CompanyRepID,Name,CompanyName,Department,Position,Email,Status,Password,RejectionReason
 */
public class CompanyRepRepository implements CrudRepository<CompanyRep, String> {

    private final Map<String, CompanyRep> map = new LinkedHashMap<>();

    // ---------- CRUD ----------
    @Override
    public Optional<CompanyRep> findById(String email) {
        if (email == null) return Optional.empty();
        return Optional.ofNullable(map.get(email.trim().toLowerCase()));
    }

    @Override
    public List<CompanyRep> findAll() {
        return new ArrayList<>(map.values());
    }

    @Override
    public CompanyRep save(CompanyRep rep) {
        map.put(rep.getEmail().trim().toLowerCase(), rep);
        return rep;
    }

    @Override
    public List<CompanyRep> saveAll(Iterable<CompanyRep> entities) {
        List<CompanyRep> out = new ArrayList<>();
        for (CompanyRep e : entities) { save(e); out.add(e); }
        return out;
    }

    @Override
    public void deleteById(String email) {
        if (email != null) map.remove(email.trim().toLowerCase());
    }

    @Override
    public void deleteAllById(Iterable<String> emails) {
        for (String e : emails) deleteById(e);
    }

    @Override
    public void deleteAll() {
        map.clear();
    }

    @Override
    public boolean existsById(String email) {
        return email != null && map.containsKey(email.trim().toLowerCase());
    }

    @Override
    public long count() {
        return map.size();
    }

    // ---------- Helper ----------
    public Optional<CompanyRep> findByEmail(String email) {
        return findById(email);
    }

    // ---------- CSV PERSISTENCE ----------
    /** Save ALL reps (approved, pending, rejected). */
    public void saveToCsv(String path) throws java.io.IOException {
        Path p = Paths.get(path);
        Files.createDirectories(p.getParent() == null ? Paths.get(".") : p.getParent());
        try (var bw = Files.newBufferedWriter(p, StandardCharsets.UTF_8, 
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            bw.write("CompanyRepID,Name,CompanyName,Department,Position,Email,Status,Password,RejectionReason");
            bw.newLine();
            for (var r : map.values()) {
                String status;
                if (r.isApproved()) status = "APPROVED";
                else if (r.isRejected()) status = "REJECTED";
                else status = "PENDING";

                bw.write(String.join(",",
                    CsvUtils.esc(r.getEmail()),          // CompanyRepID
                    CsvUtils.esc(r.getName()),
                    CsvUtils.esc(r.getCompanyName()),
                    CsvUtils.esc(r.getDepartment()),
                    CsvUtils.esc(r.getPosition()),
                    CsvUtils.esc(r.getEmail()),
                    CsvUtils.esc(status),
                    CsvUtils.esc(r.getPassword()),
                    CsvUtils.esc(r.getRejectionReason() != null ? r.getRejectionReason() : "")
                ));
                bw.newLine();
            }
        }
    }

    /** Load ALL reps from CSV (any status). */
    public void loadFromCsv(String path) throws java.io.IOException {
        Path p = Paths.get(path);
        if (!Files.exists(p)) {
            System.out.println("Company reps file not found: " + path);
            return;
        }

        try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String headerLine = br.readLine();
            if (headerLine == null) return;
            String[] headers = CsvUtils.splitCsv(headerLine);

            Map<String,Integer> idx = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i] != null ? headers[i].trim().toLowerCase() : "";
                idx.put(header, i);
            }

            String line;
            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] row = CsvUtils.splitCsv(line);

                try {
                    String email = getCsvValue(row, idx, "companyrepid", "email");
                    String name = getCsvValue(row, idx, "name");
                    String company = getCsvValue(row, idx, "companyname");
                    String dept = getCsvValue(row, idx, "department");
                    String pos = getCsvValue(row, idx, "position");
                    String statusStr = getCsvValue(row, idx, "status");
                    String password = getCsvValue(row, idx, "password");
                    String rejectionReason = getCsvValue(row, idx, "rejectionreason");

                    if (email == null || email.isBlank()) continue;

                    var rep = new CompanyRep(email, name, company, dept, pos, email);
                    
                    // Set password (default if not provided)
                    if (password != null && !password.isEmpty()) {
                        rep.setPassword(password);
                    } else {
                        rep.setPassword("password");
                    }

                    // Set status
                    if (statusStr != null) {
                        switch (statusStr.trim().toUpperCase()) {
                            case "APPROVED" -> rep.approve();
                            case "REJECTED" -> rep.reject(rejectionReason != null ? rejectionReason : "Rejected by staff");
                            default -> { /* keep as pending */ }
                        }
                    }

                    save(rep);
                } catch (Exception e) {
                    System.err.println("Error parsing company rep line: " + e.getMessage());
                }
            }
        }
    }

    // Helper method to get CSV values with multiple possible header names
    private String getCsvValue(String[] row, Map<String, Integer> idx, String... possibleHeaders) {
        for (String header : possibleHeaders) {
            Integer index = idx.get(header.toLowerCase());
            if (index != null && index < row.length && row[index] != null) {
                String value = row[index].trim();
                return value.isEmpty() ? null : value;
            }
        }
        return null;
    }
}