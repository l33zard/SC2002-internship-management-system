package database;

import entity.Student;
import util.CsvUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class StudentRepository implements CrudRepository<Student, String> {

    // Keep stable export order (nicer diffs)
    private final Map<String, Student> map = new LinkedHashMap<>();

    // ----- CrudRepository implementation -----

    @Override
    public Optional<Student> findById(String id) {
        return Optional.ofNullable(map.get(id));
    }

    @Override
    public List<Student> findAll() {
        return new ArrayList<>(map.values());
    }

    @Override
    public Student save(Student entity) {
        map.put(entity.getUserId(), entity); // upsert
        return entity;
    }

    @Override
    public List<Student> saveAll(Iterable<Student> entities) {
        List<Student> out = new ArrayList<>();
        for (Student s : entities) {
            save(s);
            out.add(s);
        }
        return out;
    }

    @Override
    public void deleteById(String id) {
        map.remove(id);
    }

    @Override
    public void deleteAllById(Iterable<String> ids) {
        for (String id : ids) map.remove(id);
    }

    @Override
    public void deleteAll() {
        map.clear();
    }

    @Override
    public boolean existsById(String id) {
        return map.containsKey(id);
    }

    @Override
    public long count() {
        return map.size();
    }

    // ----- Convenience domain finders -----

    public List<Student> findByMajor(String major) {
        if (major == null) return List.of();
        String m = major.trim();
        List<Student> out = new ArrayList<>();
        for (Student s : map.values()) {
            if (s.getMajor().equalsIgnoreCase(m)) out.add(s);
        }
        return out;
    }

    public List<Student> findByYear(int year) {
        List<Student> out = new ArrayList<>();
        for (Student s : map.values()) if (s.getYearOfStudy() == year) out.add(s);
        return out;
    }

    // ----- CSV I/O -----

    /** Load students from CSV. Header like: StudentID,Name,Major,Year,Email,Password */
    public void loadFromCsv(String path) throws IOException {
        Path p = Paths.get(path);
        if (!Files.exists(p)) {
            System.out.println("Students file not found: " + path);
            return;
        }

        try (BufferedReader br = Files.newBufferedReader(p, StandardCharsets.UTF_8)) {
            String headerLine = br.readLine();
            if (headerLine == null) return;

            // Create header index manually
            String[] headers = CsvUtils.splitCsv(headerLine);
            Map<String, Integer> idx = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                String header = headers[i] != null ? headers[i].trim().toLowerCase() : "";
                idx.put(header, i);
            }

            String line;
            int lineNo = 1;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;

                String[] row = CsvUtils.splitCsv(line);
                try {
                    String userId = getCsvValue(row, idx, "studentid", "userid", "id", "student id");
                    String name = getCsvValue(row, idx, "name", "full name");
                    String major = getCsvValue(row, idx, "major", "programme", "program", "course");
                    String yearS = getCsvValue(row, idx, "year", "yearofstudy", "study year");
                    String email = getCsvValue(row, idx, "email", "mail");
                    String password = getCsvValue(row, idx, "password", "pwd");

                    // Validate student ID format
                    if (userId == null || !userId.matches("^U\\d{7}[A-Z]$")) {
                        System.err.println("Invalid student ID format at line " + lineNo + ": " + userId);
                        continue;
                    }

                    int year = yearS != null ? Integer.parseInt(yearS.trim()) : 1;
                    
                    // Use provided password or default
                    if (password == null || password.isEmpty()) {
                        password = "password";
                    }

                    Student student = new Student(userId.trim(), name.trim(), 
                        major != null ? major.trim() : "CSC", year, 
                        email != null ? email.trim() : "");
                    student.setPassword(password); // SET THE PASSWORD
                    save(student);
                    
                } catch (Exception ex) {
                    System.err.println("Skipping line " + lineNo + ": " + ex.getMessage());
                }
            }
        }
    }

    /** Save students to CSV with header: StudentID,Name,Major,Year,Email,Password */
    public void saveToCsv(String path) throws IOException {
        Path p = Paths.get(path);
        Files.createDirectories(p.getParent() == null ? Paths.get(".") : p.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(p, StandardCharsets.UTF_8, 
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            bw.write("StudentID,Name,Major,Year,Email,Password");
            bw.newLine();
            for (Student s : map.values()) {
                bw.write(String.join(",",
                    CsvUtils.esc(s.getUserId()),
                    CsvUtils.esc(s.getName()),
                    CsvUtils.esc(s.getMajor()),
                    String.valueOf(s.getYearOfStudy()),
                    CsvUtils.esc(s.getEmail()),
                    CsvUtils.esc(s.getPassword())  // ADDED PASSWORD
                ));
                bw.newLine();
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