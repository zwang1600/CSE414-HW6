package scheduler.model;

import scheduler.db.ConnectionManager;
import scheduler.util.Util;

import java.sql.*;
import java.sql.Date;
import java.util.*;

public class Caregiver {
    private final String username;
    private final byte[] salt;
    private final byte[] hash;

    private Caregiver(CaregiverBuilder builder) {
        this.username = builder.username;
        this.salt = builder.salt;
        this.hash = builder.hash;
    }

    private Caregiver(CaregiverGetter getter) {
        this.username = getter.username;
        this.salt = getter.salt;
        this.hash = getter.hash;
    }

    // Getters
    public String getUsername() {
        return username;
    }

    public byte[] getSalt() {
        return salt;
    }

    public byte[] getHash() {
        return hash;
    }

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addCaregiver = "INSERT INTO caregivers VALUES (? , ?, ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addCaregiver);
            statement.setString(1, this.username);
            statement.setBytes(2, this.salt);
            statement.setBytes(3, this.hash);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void uploadAvailability(Date d) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addAvailability = "INSERT INTO Availabilities VALUES (? , ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAvailability);
            statement.setDate(1, d);
            statement.setString(2, this.username);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void getAvailability(Date d) throws SQLException {

        List<String> caregivers = getAvailableCaregivers(d);
        System.out.println("Below are available caregivers for " + d + ":");
        for (String caregiver : caregivers) {
            System.out.println(caregiver);
        }

        System.out.println();
        System.out.println("Below are available doses for " + d + ":");
        Map<String, Integer> doses = getAvailableDoses();
        for (String k : doses.keySet()) {
            System.out.println(k + ": " + doses.get(k));
        }
    }

    public List<String> getAvailableCaregivers(Date d) throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getAvailability = "SELECT A.username FROM Availabilities A WHERE A.time = ? ORDER BY A.username";

        try {
            PreparedStatement statement = con.prepareStatement(getAvailability);
            statement.setDate(1, d);
            ResultSet res = statement.executeQuery();
            List<String> availableCaregivers = new ArrayList<>();
            while (res.next()) {
                availableCaregivers.add(res.getString("username"));
            }
            return availableCaregivers;
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public Map<String, Integer> getAvailableDoses() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getDoses = "SELECT * FROM Vaccines";

        try {
            PreparedStatement statement = con.prepareStatement(getDoses);
            ResultSet res = statement.executeQuery();
            Map<String, Integer> availableDoses = new HashMap<>();
            while (res.next()) {
                String name = res.getString("name");
                int doses = Integer.parseInt(res.getString("doses"));
                availableDoses.put(name, doses);
            }
            return availableDoses;
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void getCurrentAppointments() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String getCurrentAppointments = "SELECT * FROM Appointments WHERE Caregiver = ? ORDER BY AppointmentID";
        try {
            PreparedStatement statement = con.prepareStatement(getCurrentAppointments);
            statement.setString(1, this.username);
            ResultSet res = statement.executeQuery();
            while (res.next()) {
                System.out.println(res.getString("AppointmentID") + " "
                        + res.getString("Vaccine") + " "
                        + res.getString("Time") + " "
                        + res.getString("Patient"));
            }
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public static class CaregiverBuilder {
        private final String username;
        private final byte[] salt;
        private final byte[] hash;

        public CaregiverBuilder(String username, byte[] salt, byte[] hash) {
            this.username = username;
            this.salt = salt;
            this.hash = hash;
        }

        public Caregiver build() {
            return new Caregiver(this);
        }
    }

    public static class CaregiverGetter {
        private final String username;
        private final String password;
        private byte[] salt;
        private byte[] hash;

        public CaregiverGetter(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public Caregiver get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getCaregiver = "SELECT Salt, Hash FROM Caregivers WHERE Username = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getCaregiver);
                statement.setString(1, this.username);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    byte[] salt = resultSet.getBytes("Salt");
                    // we need to call Util.trim() to get rid of the paddings,
                    // try to remove the use of Util.trim() and you'll see :)
                    byte[] hash = Util.trim(resultSet.getBytes("Hash"));
                    // check if the password matches
                    byte[] calculatedHash = Util.generateHash(password, salt);
                    if (!Arrays.equals(hash, calculatedHash)) {
                        return null;
                    } else {
                        this.salt = salt;
                        this.hash = hash;
                        return new Caregiver(this);
                    }
                }
                return null;
            } catch (SQLException e) {
                throw new SQLException();
            } finally {
                cm.closeConnection();
            }
        }
    }
}
