package scheduler;

import scheduler.db.ConnectionManager;
import scheduler.model.Appointment;
import scheduler.model.Caregiver;
import scheduler.model.Patient;
import scheduler.model.Vaccine;
import scheduler.util.Util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Scheduler {

    // objects to keep track of the currently logged-in user
    // Note: it is always true that at most one of currentCaregiver and currentPatient is not null
    //       since only one user can be logged-in at a time
    private static Caregiver currentCaregiver = null;
    private static Patient currentPatient = null;

    public static void main(String[] args) {
        // printing greetings text
        System.out.println();
        System.out.println("Welcome to the COVID-19 Vaccine Reservation Scheduling Application!");
        System.out.println("*** Please enter one of the following commands ***");
        System.out.println("> create_patient <username> <password>");
        System.out.println("> create_caregiver <username> <password>");
        System.out.println("> login_patient <username> <password>");
        System.out.println("> login_caregiver <username> <password>");
        System.out.println("> search_caregiver_schedule <date>");
        System.out.println("> reserve <date> <vaccine>");
        System.out.println("> upload_availability <date>");
        System.out.println("> cancel <appointment_id>");
        System.out.println("> add_doses <vaccine> <number>");
        System.out.println("> show_appointments");
        System.out.println("> logout");
        System.out.println("> quit");
        System.out.println();

        // read input from user
        BufferedReader r = new BufferedReader(new InputStreamReader(System.in));
        while (true) {
            System.out.print("> ");
            String response = "";
            try {
                response = r.readLine();
            } catch (IOException e) {
                System.out.println("Please try again!");
            }
            // split the user input by spaces
            String[] tokens = response.split(" ");
            // check if input exists
            if (tokens.length == 0) {
                System.out.println("Please try again!");
                continue;
            }
            // determine which operation to perform
            String operation = tokens[0];
            if (operation.equals("create_patient")) {
                createPatient(tokens);
            } else if (operation.equals("create_caregiver")) {
                createCaregiver(tokens);
            } else if (operation.equals("login_patient")) {
                loginPatient(tokens);
            } else if (operation.equals("login_caregiver")) {
                loginCaregiver(tokens);
            } else if (operation.equals("search_caregiver_schedule")) {
                searchCaregiverSchedule(tokens);
            } else if (operation.equals("reserve")) {
                reserve(tokens);
            } else if (operation.equals("upload_availability")) {
                uploadAvailability(tokens);
            } else if (operation.equals("cancel")) {
                cancel(tokens);
            } else if (operation.equals("add_doses")) {
                addDoses(tokens);
            } else if (operation.equals("show_appointments")) {
                showAppointments(tokens);
            } else if (operation.equals("logout")) {
                logout(tokens);
            } else if (operation.equals("quit")) {
                System.out.println("Bye!");
                return;
            } else {
                System.out.println("Invalid operation name!");
            }
        }
    }

    private static void createPatient(String[] tokens) {
        // create_patient <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsPatient(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the patient
        try {
            currentPatient = new Patient.PatientBuilder(username, salt, hash).build();
            // save to patient information to our database
            currentPatient.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsPatient(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Patients WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void createCaregiver(String[] tokens) {
        // create_caregiver <username> <password>
        // check 1: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Failed to create user.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];
        // check 2: check if the username has been taken already
        if (usernameExistsCaregiver(username)) {
            System.out.println("Username taken, try again!");
            return;
        }
        byte[] salt = Util.generateSalt();
        byte[] hash = Util.generateHash(password, salt);
        // create the caregiver
        try {
            currentCaregiver = new Caregiver.CaregiverBuilder(username, salt, hash).build();
            // save to caregiver information to our database
            currentCaregiver.saveToDB();
            System.out.println("Created user " + username);
        } catch (SQLException e) {
            System.out.println("Failed to create user.");
            e.printStackTrace();
        }
    }

    private static boolean usernameExistsCaregiver(String username) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectUsername = "SELECT * FROM Caregivers WHERE Username = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectUsername);
            statement.setString(1, username);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void loginPatient(String[] tokens) {
        // login_patient <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Patient patient = null;
        try {
            patient = new Patient.PatientGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (patient == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentPatient = patient;
        }
    }

    private static void loginCaregiver(String[] tokens) {
        // login_caregiver <username> <password>
        // check 1: if someone's already logged-in, they need to log out first
        if (currentCaregiver != null || currentPatient != null) {
            System.out.println("User already logged in.");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Login failed.");
            return;
        }
        String username = tokens[1];
        String password = tokens[2];

        Caregiver caregiver = null;
        try {
            caregiver = new Caregiver.CaregiverGetter(username, password).get();
        } catch (SQLException e) {
            System.out.println("Login failed.");
            e.printStackTrace();
        }
        // check if the login was successful
        if (caregiver == null) {
            System.out.println("Login failed.");
        } else {
            System.out.println("Logged in as: " + username);
            currentCaregiver = caregiver;
        }
    }

    private static void searchCaregiverSchedule(String[] tokens) {
        // search_caregiver_schedule <date>
        // check 1: if not logged-in, prompt the user to log in first
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        try {
            Date d = Date.valueOf(date);
            if (currentCaregiver != null) {
                currentCaregiver.getAvailability(d);
            } else {
                currentPatient.getAvailability(d);
            }
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when searching for availability");
            e.printStackTrace();
        }
    }

    private static void reserve(String[] tokens) {
        // reserve <date> <vaccine>
        // check 1: if a caregiver is logged-in, prompt them to log in as patient
        if (currentCaregiver != null) {
            System.out.println("Please login as a patient!");
            return;
        }
        // check 2: if not logged-in, prompt the user to log in first
        if (currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        // check 3: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }

        String date = tokens[1];
        String vaccine = tokens[2];

        try {
            Date d = Date.valueOf(date);
            List<String> availableCaregivers = currentPatient.getAvailableCaregivers(d);
            Map<String, Integer> availableDoses = currentPatient.getAvailableDoses();
            if (availableCaregivers.size() == 0) {
                System.out.println("No Caregiver is available!");
            } else if (!availableDoses.containsKey(vaccine) || Objects.equals(availableDoses.get(vaccine), 0)) {
                System.out.println("Not enough available doses!");
            } else {
                // create a new appointment
                String caregiverUsername = availableCaregivers.get(0);
                int appointmentID = currentPatient.getMaxAppointmentID() + 1;
                Appointment appointment = new Appointment.AppointmentBuilder(appointmentID, caregiverUsername, currentPatient.getUsername(), vaccine, d).build();

                // save to appointment information to our database
                appointment.saveToDB();

                // decrease the dose by 1
                Vaccine vac = new Vaccine.VaccineGetter(vaccine).get();
                vac.decreaseAvailableDoses(1);

                // change availability of the caregiver
                currentPatient.updateAvailability(d, caregiverUsername);
            }
            System.out.println("Reservation made with " + availableCaregivers.get(0) + " on " + date);
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when reserving");
            e.printStackTrace();
        }
    }

    private static void uploadAvailability(String[] tokens) {
        // upload_availability <date>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }
        String date = tokens[1];
        Date d = Date.valueOf(date);
        // check 3: check if the availability already exists
        if (availabilityExist(d)) {
            System.out.println("You're already available on this date, try again!");
            return;
        }

        try {
            currentCaregiver.uploadAvailability(d);
            System.out.println("Availability uploaded!");
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid date!");
        } catch (SQLException e) {
            System.out.println("Error occurred when uploading availability");
            e.printStackTrace();
        }
    }

    private static boolean availabilityExist(Date date) {
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();

        String selectAvailability = "SELECT * FROM Availabilities WHERE Username = ? AND Time = ?";
        try {
            PreparedStatement statement = con.prepareStatement(selectAvailability);
            statement.setString(1, currentCaregiver.getUsername());
            statement.setDate(2, date);
            ResultSet resultSet = statement.executeQuery();
            // returns false if the cursor is not before the first record or if there are no rows in the ResultSet.
            return resultSet.isBeforeFirst();
        } catch (SQLException e) {
            System.out.println("Error occurred when checking username");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
        return true;
    }

    private static void cancel(String[] tokens) {
        // cancel <appointment_id>
        // check 1: if not logged-in, prompt the user to log in first
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 2 to include all information (with the operation name)
        if (tokens.length != 2) {
            System.out.println("Please try again!");
            return;
        }

        int appointmentID = Integer.parseInt(tokens[1]);
        ConnectionManager cm = new ConnectionManager();
        Connection con = cm.createConnection();
        String addAvailability = "INSERT INTO Availabilities VALUES (? , ?)";
        try {
            // remove the appointment only when it exists and it is the right user
            Appointment appointment = new Appointment.AppointmentGetter(appointmentID).get();
            if (appointment == null) {
                System.out.println("This appointment doesn't exist!");
                return;
            }
            if ((currentCaregiver != null && !Objects.equals(currentCaregiver.getUsername(), appointment.getCaregiverUsername())) ||
            (!Objects.equals(currentPatient.getUsername(), appointment.getPatientUsername()))){
                System.out.println("You don't have access to cancel this appointment");
                return;
            }
            appointment.removeFromDB();

            // increase the dose by 1
            String vaccine = appointment.getVaccineName();
            Vaccine vac = new Vaccine.VaccineGetter(vaccine).get();
            vac.increaseAvailableDoses(1);

            // change the availability of the caregiver
            String caregiver = appointment.getCaregiverUsername();
            Date d = appointment.getDate();

            PreparedStatement statement = con.prepareStatement(addAvailability);
            statement.setDate(1, d);
            statement.setString(2, caregiver);
            statement.executeUpdate();

            System.out.println("Reservation cancelled successfully with " + caregiver + " on " + d);
        } catch (IllegalArgumentException e) {
            System.out.println("Please enter a valid appointment ID!");
        } catch (SQLException e) {
            System.out.println("Error occurred when searching for availability");
            e.printStackTrace();
        } finally {
            cm.closeConnection();
        }
    }

    private static void addDoses(String[] tokens) {
        // add_doses <vaccine> <number>
        // check 1: check if the current logged-in user is a caregiver
        if (currentCaregiver == null) {
            System.out.println("Please login as a caregiver first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 3 to include all information (with the operation name)
        if (tokens.length != 3) {
            System.out.println("Please try again!");
            return;
        }
        String vaccineName = tokens[1];
        int doses = Integer.parseInt(tokens[2]);
        Vaccine vaccine = null;
        try {
            vaccine = new Vaccine.VaccineGetter(vaccineName).get();
        } catch (SQLException e) {
            System.out.println("Error occurred when adding doses");
            e.printStackTrace();
        }
        // check 3: if getter returns null, it means that we need to create the vaccine and insert it into the Vaccines
        //          table
        if (vaccine == null) {
            try {
                vaccine = new Vaccine.VaccineBuilder(vaccineName, doses).build();
                vaccine.saveToDB();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            // if the vaccine is not null, meaning that the vaccine already exists in our table
            try {
                vaccine.increaseAvailableDoses(doses);
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
        System.out.println("Doses updated!");
    }

    private static void showAppointments(String[] tokens) {
        // show_appointments
        // check 1: if not logged-in, prompt the user to log in first
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first!");
            return;
        }
        // check 2: the length for tokens need to be exactly 1 to include all information
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        if (currentPatient != null) {
            try {
                currentPatient.getCurrentAppointments();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        } else {
            try {
                currentCaregiver.getCurrentAppointments();
            } catch (SQLException e) {
                System.out.println("Error occurred when adding doses");
                e.printStackTrace();
            }
        }
    }

    private static void logout(String[] tokens) {
        // logout
        // check 1: if no one is already logged-in, no logout can be done
        if (currentCaregiver == null && currentPatient == null) {
            System.out.println("Please login first.");
            return;
        }
        // check 2: the length for tokens need to be exactly 1 to include all information (only operation name is needed)
        if (tokens.length != 1) {
            System.out.println("Please try again!");
            return;
        }
        currentPatient = null;
        currentCaregiver = null;
        System.out.println("Successfully logged out!");
    }
}
