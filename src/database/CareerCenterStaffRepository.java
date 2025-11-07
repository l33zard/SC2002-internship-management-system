package database;

import entity.CareerCenterStaff;
import util.CsvUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

public class CareerCenterStaffRepository implements CrudRepository<CareerCenterStaff, String> {

    // Stable iteration/export order
    private final Map<String, CareerCenterStaff> map = new LinkedHashMap<>();

    // ---------- CrudRepository ----------

    @Override
    public Optional<CareerCenterStaff> findById(String id) {
        return Optional.ofNullable(map.get(id));
    }

    @Override
    public List<CareerCenterStaff> findAll() {
        return new ArrayList<>(map.values());
    }

    @Override
    public CareerCenterStaff save(CareerCenterStaff s) {
        map.put(s.getUserId(), s); // upsert
        return s;
    }

    @Override
    public List<CareerCenterStaff> saveAll(Iterable<CareerCenterStaff> entities) {
        List<CareerCenterStaff> out = new ArrayList<>();
        for (CareerCenterStaff e : entities) { save(e); out.add(e); }
        return out;
    }

    @Override
    public void deleteById(String id) { map.remove(id); }

    @Override
    public void deleteAllById(Iterable<String> ids) {
        for (String id : ids) map.remove(id);
    }

    @Override
    public void deleteAll() { map.clear(); }

    @Override
    public boolean existsById(String id) { return map.containsKey(id); }

    @Override
    public long count() { return map.size(); }

    // ---------- CSV I/O ----------

    private static final String[] COL_ID   = {"staffid","user id","userid","id","staff id","ntu id","ntu account"};
    private static final String[] COL_NAME = {"name","full name"};
    private static final String[] COL_ROLE = {"role","position","title"};
    private static final String[] COL_DEPT = {"department","dept"};
    private static final String[] COL_MAIL = {"email","mail"};
    private static final String[] COL_PASS = {"password","pwd"};

    public void loadFromCsv(String path) throws IOException {
        Path p = Paths.get(path);
        if (!Files.exists(p)) {
            System.out.println("Staff file not found: " + path);
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
            int lineNo = 1;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (line.isBlank()) continue;

                String[] c = CsvUtils.splitCsv(line);
                try {
                    String userId = getCsvValue(c, idx, COL_ID);
                    String name   = getCsvValue(c, idx, COL_NAME);
                    String role   = getCsvValue(c, idx, COL_ROLE);
                    String dept   = getCsvValue(c, idx, COL_DEPT);
                    String email  = getCsvValue(c, idx, COL_MAIL);
                    String password = getCsvValue(c, idx, COL_PASS);

                    if (userId == null) {
                        System.err.println("Staff CSV skip line " + lineNo + ": missing user ID");
                        continue;
                    }

                    CareerCenterStaff staff = new CareerCenterStaff(
                        userId.trim(), 
                        name != null ? name.trim() : "", 
                        role != null ? role.trim() : "Staff", 
                        dept != null ? dept.trim() : "", 
                        email != null ? email.trim() : ""
                    );
                    
                    // Set password (provided or default)
                    if (password != null && !password.isEmpty()) {
                        staff.setPassword(password);
                    } else {
                        staff.setPassword("password");
                    }

                    save(staff);
                } catch (Exception ex) {
                    System.err.println("Staff CSV skip line " + lineNo + ": " + ex.getMessage());
                }
            }
        }
    }

    public void saveToCsv(String path) throws IOException {
        Path p = Paths.get(path);
        Files.createDirectories(p.getParent() == null ? Paths.get(".") : p.getParent());
        try (BufferedWriter bw = Files.newBufferedWriter(p, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            bw.write("StaffID,Name,Role,Department,Email,Password");
            bw.newLine();
            for (CareerCenterStaff s : map.values()) {
                bw.write(String.join(",",
                    CsvUtils.esc(s.getUserId()),
                    CsvUtils.esc(s.getName()),
                    CsvUtils.esc(s.getRole()),
                    CsvUtils.esc(s.getDepartment()),
                    CsvUtils.esc(s.getEmail()),
                    CsvUtils.esc(s.getPassword())
                ));
                bw.newLine();
            }
        }
    }

    // ---------- Domain helpers ----------
    public List<CareerCenterStaff> findByDepartment(String department) {
        if (department == null) return List.of();
        String d = department.trim();
        List<CareerCenterStaff> out = new ArrayList<>();
        for (var s : map.values()) if (s.getDepartment().equalsIgnoreCase(d)) out.add(s);
        return out;
    }

    // Helper method to get CSV values
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