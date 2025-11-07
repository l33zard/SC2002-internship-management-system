package entity;

public abstract class User {
    protected String userId;
    protected String name;
    protected String password;

    public User(String userId, String name) {
        this.userId = userId;
        this.name = name;
    }

    public String getUserId() { return userId; }
    
    public String getName() { return name; }
    
    public String getPassword() { return password; }
    
    public void setPassword(String password) { 
        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty");
        }
        this.password = password.trim(); 
    }
    
    public boolean checkPassword(String attempt) {
        return password != null && password.equals(attempt);
    }
    
	public void changePassword(String currentPassword, String newPassword) {
		if (!this.password.equals(currentPassword)) {
		    throw new IllegalArgumentException("Current password is incorrect.");
		}
		if (newPassword == null || newPassword.isEmpty()) {
		    throw new IllegalArgumentException("New password cannot be empty.");
		}
		if (newPassword.equals(this.password)) {
		    throw new IllegalArgumentException("New password cannot be the same as the old one.");
		}
		this.password = newPassword;
	}

//    @Override public String toString() { return String.format("%s (%s)", name, userId); }
}