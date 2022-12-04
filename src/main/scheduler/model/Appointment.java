package scheduler.model;

import scheduler.db.ConnectionManager;

import java.sql.*;

public class Appointment {
    private final int appointmentID;
    private final String caregiverUsername;
    private final String patientUsername;
    private final String vaccineName;
    private final Date date;

    private Appointment(AppointmentBuilder builder) {
        this.appointmentID = builder.appointmentID;
        this.caregiverUsername = builder.caregiverUsername;
        this.patientUsername = builder.patientUsername;
        this.vaccineName = builder.vaccineName;
        this.date = builder.date;
    }

    private Appointment(AppointmentGetter getter) {
        this.appointmentID = getter.appointmentID;
        this.caregiverUsername = getter.caregiverUsername;
        this.patientUsername = getter.patientUsername;
        this.vaccineName = getter.vaccineName;
        this.date = getter.date;
    }

    // Getters
    public int getAppointmentID() {
        return appointmentID;
    }

    public String getCaregiverUsername() {
        return caregiverUsername;
    }

    public String getPatientUsername() {
        return patientUsername;
    }

    public String getVaccineName() {
        return vaccineName;
    }

    public Date getDate() {
        return date;
    }

    public void saveToDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String addAppointment = "INSERT INTO appointments VALUES (? , ?, ? , ? , ?)";
        try {
            PreparedStatement statement = con.prepareStatement(addAppointment);
            statement.setInt(1, this.appointmentID);
            statement.setString(2, this.caregiverUsername);
            statement.setString(3, this.patientUsername);
            statement.setString(4, this.vaccineName);
            statement.setDate(5, this.date);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public void removeFromDB() throws SQLException {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String removeAppointment = "DELETE FROM Appointments WHERE AppointmentID = ?";
        try {
            PreparedStatement statement = con.prepareStatement(removeAppointment);
            statement.setInt(1, this.appointmentID);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new SQLException();
        } finally {
            cm.closeConnection();
        }
    }

    public static class AppointmentBuilder {
        private final int appointmentID;
        private final String caregiverUsername;
        private final String patientUsername;
        private final String vaccineName;
        private final Date date;

        public AppointmentBuilder(int appointmentID,
                                  String caregiverUsername,
                                  String patientUsername,
                                  String vaccineName,
                                  Date date) {
            this.appointmentID = appointmentID;
            this.caregiverUsername = caregiverUsername;
            this.patientUsername = patientUsername;
            this.vaccineName = vaccineName;
            this.date = date;
        }

        public Appointment build() {
            return new Appointment(this);
        }
    }

    public static class AppointmentGetter {
        private final int appointmentID;
        private String caregiverUsername;
        private String patientUsername;
        private String vaccineName;
        private Date date;

        public AppointmentGetter(int appointmentID) {
            this.appointmentID = appointmentID;
        }

        public Appointment get() throws SQLException {
            ConnectionManager cm = new ConnectionManager();
            Connection con = cm.createConnection();

            String getAppointment = "SELECT * FROM Appointments WHERE AppointmentID = ?";
            try {
                PreparedStatement statement = con.prepareStatement(getAppointment);
                statement.setInt(1, this.appointmentID);
                ResultSet resultSet = statement.executeQuery();
                while (resultSet.next()) {
                    this.caregiverUsername = resultSet.getString("Caregiver");
                    this.patientUsername = resultSet.getString("Patient");
                    this.vaccineName = resultSet.getString("Vaccine");
                    this.date = resultSet.getDate("Time");
                    return new Appointment(this);
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
