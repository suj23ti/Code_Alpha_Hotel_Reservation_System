import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.*;


public class HotelReservationSystem {

    enum Category {
        STANDARD, DELUXE, SUITE;

        static Category fromString(String s) {
            try {
                return Category.valueOf(s.trim().toUpperCase());
            } catch (Exception e) {
                return null;
            }
        }
    }

    static class Room {
        int roomNumber;
        Category category;
        double pricePerNight;
        boolean available;

        Room(int roomNumber, Category category, double pricePerNight, boolean available) {
            this.roomNumber = roomNumber;
            this.category = category;
            this.pricePerNight = pricePerNight;
            this.available = available;
        }

        String toFileString() {
            return roomNumber + "|" + category + "|" + pricePerNight + "|" + available;
        }

        static Room fromFileString(String line) {
            String[] a = line.split("\\|");
            return new Room(
                    Integer.parseInt(a[0]),
                    Category.valueOf(a[1]),
                    Double.parseDouble(a[2]),
                    Boolean.parseBoolean(a[3])
            );
        }

        @Override
        public String toString() {
            return String.format("Room %-4d | %-8s | $%.2f/night | %s",
                    roomNumber, category, pricePerNight, available ? "Available" : "Booked");
        }
    }

    // ================= RESERVATION CLASS =================
    enum Status { PAID, CANCELLED }

    static class Reservation {
        int id;
        String guestName;
        int roomNumber;
        Category category;
        LocalDate checkIn;
        LocalDate checkOut;
        double amountPaid;
        String paymentMethod;
        Status status;

        Reservation(int id, String guestName, int roomNumber, Category category,
                    LocalDate checkIn, LocalDate checkOut, double amountPaid,
                    String paymentMethod, Status status) {
            this.id = id;
            this.guestName = guestName;
            this.roomNumber = roomNumber;
            this.category = category;
            this.checkIn = checkIn;
            this.checkOut = checkOut;
            this.amountPaid = amountPaid;
            this.paymentMethod = paymentMethod;
            this.status = status;
        }

        long nights() {
            return ChronoUnit.DAYS.between(checkIn, checkOut);
        }

        String toFileString() {
            return id + "|" + guestName + "|" + roomNumber + "|" + category + "|" +
                    checkIn + "|" + checkOut + "|" + amountPaid + "|" + paymentMethod + "|" + status;
        }

        static Reservation fromFileString(String line) {
            String[] a = line.split("\\|");
            return new Reservation(
                    Integer.parseInt(a[0]),
                    a[1],
                    Integer.parseInt(a[2]),
                    Category.valueOf(a[3]),
                    LocalDate.parse(a[4]),
                    LocalDate.parse(a[5]),
                    Double.parseDouble(a[6]),
                    a[7],
                    Status.valueOf(a[8])
            );
        }

        @Override
        public String toString() {
            return String.format("ID %-5d | %-15s | Room %-4d (%s) | %s -> %s | %d night(s) | $%.2f | %s | %s",
                    id, guestName, roomNumber, category, checkIn, checkOut, nights(),
                    amountPaid, paymentMethod, status);
        }
    }

    // ================= ROOM MANAGER =================
    static class RoomManager {
        private final List<Room> rooms = new ArrayList<>();
        private final String fileName;

        RoomManager(String fileName) {
            this.fileName = fileName;
            load();
        }

        void load() {
            File f = new File(fileName);
            if (!f.exists()) return;
            try (Scanner s = new Scanner(f)) {
                while (s.hasNextLine()) {
                    String line = s.nextLine().trim();
                    if (!line.isEmpty()) rooms.add(Room.fromFileString(line));
                }
            } catch (Exception e) {
                System.out.println("Error loading rooms: " + e.getMessage());
            }
        }

        void save() {
            try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
                for (Room r : rooms) pw.println(r.toFileString());
            } catch (Exception e) {
                System.out.println("Error saving rooms: " + e.getMessage());
            }
        }

        boolean roomNumberExists(int roomNumber) {
            return findByNumber(roomNumber) != null;
        }

        boolean addRoom(int roomNumber, Category category, double price) {
            if (roomNumberExists(roomNumber)) return false;
            rooms.add(new Room(roomNumber, category, price, true));
            save();
            return true;
        }

        Room findByNumber(int roomNumber) {
            for (Room r : rooms) if (r.roomNumber == roomNumber) return r;
            return null;
        }

        List<Room> all() {
            return rooms;
        }

        List<Room> searchByCategory(Category c) {
            List<Room> result = new ArrayList<>();
            for (Room r : rooms) if (r.category == c && r.available) result.add(r);
            return result;
        }

        List<Room> searchByPriceRange(double min, double max) {
            List<Room> result = new ArrayList<>();
            for (Room r : rooms) if (r.available && r.pricePerNight >= min && r.pricePerNight <= max) result.add(r);
            return result;
        }

        List<Room> availableRooms() {
            List<Room> result = new ArrayList<>();
            for (Room r : rooms) if (r.available) result.add(r);
            return result;
        }
    }

    // ================= RESERVATION MANAGER =================
    static class ReservationManager {
        private final List<Reservation> reservations = new ArrayList<>();
        private final String fileName;

        ReservationManager(String fileName) {
            this.fileName = fileName;
            load();
        }

        void load() {
            File f = new File(fileName);
            if (!f.exists()) return;
            try (Scanner s = new Scanner(f)) {
                while (s.hasNextLine()) {
                    String line = s.nextLine().trim();
                    if (!line.isEmpty()) reservations.add(Reservation.fromFileString(line));
                }
            } catch (Exception e) {
                System.out.println("Error loading bookings: " + e.getMessage());
            }
        }

        void save() {
            try (PrintWriter pw = new PrintWriter(new FileWriter(fileName))) {
                for (Reservation r : reservations) pw.println(r.toFileString());
            } catch (Exception e) {
                System.out.println("Error saving bookings: " + e.getMessage());
            }
        }

        int nextId() {
            int max = 1000;
            for (Reservation r : reservations) if (r.id > max) max = r.id;
            return max + 1;
        }

        Reservation findById(int id) {
            for (Reservation r : reservations) if (r.id == id) return r;
            return null;
        }

        void add(Reservation r) {
            reservations.add(r);
            save();
        }

        List<Reservation> all() {
            return reservations;
        }
    }

    // ================= MAIN APP =================
    static RoomManager roomManager = new RoomManager("rooms.txt");
    static ReservationManager reservationManager = new ReservationManager("bookings.txt");
    static Scanner sc = new Scanner(System.in);
    static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ISO_LOCAL_DATE; // yyyy-MM-dd

    public static void main(String[] args) {

        // Seed default rooms on first run so the system is usable out of the box
        if (roomManager.all().isEmpty()) {
            roomManager.addRoom(101, Category.STANDARD, 50.0);
            roomManager.addRoom(102, Category.STANDARD, 50.0);
            roomManager.addRoom(201, Category.DELUXE, 90.0);
            roomManager.addRoom(202, Category.DELUXE, 90.0);
            roomManager.addRoom(301, Category.SUITE, 150.0);
        }

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Choice: ");

            switch (choice) {
                case 1 -> showAllRooms();
                case 2 -> searchRooms();
                case 3 -> bookRoom();
                case 4 -> cancelBooking();
                case 5 -> showBookings();
                case 6 -> addRoom();
                case 7 -> {
                    System.out.println("Goodbye!");
                    running = false;
                }
                default -> System.out.println("Invalid choice, try again.");
            }
        }
    }

    static void printMenu() {
        System.out.println("\n===== HOTEL RESERVATION SYSTEM =====");
        System.out.println("1. View All Rooms");
        System.out.println("2. Search Rooms (category / price range)");
        System.out.println("3. Book a Room");
        System.out.println("4. Cancel a Booking");
        System.out.println("5. View Bookings");
        System.out.println("6. Add Room (admin)");
        System.out.println("7. Exit");
    }

    // ================= ROOM OPERATIONS =================
    static void addRoom() {
        int roomNumber = readInt("Room number: ");

        if (roomManager.roomNumberExists(roomNumber)) {
            System.out.println("A room with that number already exists.");
            return;
        }

        Category category = readCategory();
        double price = readPositiveDouble("Price per night: ");

        roomManager.addRoom(roomNumber, category, price);
        System.out.println("Room added successfully.");
    }

    static void showAllRooms() {
        if (roomManager.all().isEmpty()) {
            System.out.println("No rooms in the system.");
            return;
        }
        for (Room r : roomManager.all()) System.out.println(r);
    }

    static void searchRooms() {
        System.out.println("Search by: 1. Category   2. Price range");
        int opt = readInt("Choice: ");

        List<Room> result;
        if (opt == 1) {
            Category c = readCategory();
            result = roomManager.searchByCategory(c);
        } else if (opt == 2) {
            double min = readPositiveDouble("Min price: ");
            double max = readPositiveDouble("Max price: ");
            result = roomManager.searchByPriceRange(min, max);
        } else {
            System.out.println("Invalid option.");
            return;
        }

        if (result.isEmpty()) {
            System.out.println("No matching available rooms found.");
        } else {
            for (Room r : result) System.out.println(r);
        }
    }

    // ================= BOOKING =================
    static void bookRoom() {
        List<Room> available = roomManager.availableRooms();
        if (available.isEmpty()) {
            System.out.println("No rooms currently available.");
            return;
        }

        System.out.println("\nAvailable rooms:");
        for (Room r : available) System.out.println(r);

        int roomNumber = readInt("\nEnter room number to book: ");
        Room room = roomManager.findByNumber(roomNumber);

        if (room == null || !room.available) {
            System.out.println("Invalid or unavailable room.");
            return;
        }

        sc.nextLine(); // consume leftover newline
        System.out.print("Guest name: ");
        String name = sc.nextLine().trim();
        if (name.isEmpty()) name = "Guest";

        LocalDate checkIn = readDate("Check-in date (yyyy-MM-dd): ");
        LocalDate checkOut = readDate("Check-out date (yyyy-MM-dd): ");

        if (!checkOut.isAfter(checkIn)) {
            System.out.println("Check-out date must be after check-in date. Booking cancelled.");
            return;
        }

        long nights = ChronoUnit.DAYS.between(checkIn, checkOut);
        double amountDue = nights * room.pricePerNight;

        String paymentMethod = simulatePayment(amountDue);
        if (paymentMethod == null) {
            System.out.println("Payment failed or cancelled. Booking not completed.");
            return;
        }

        int id = reservationManager.nextId();
        Reservation reservation = new Reservation(
                id, name, room.roomNumber, room.category,
                checkIn, checkOut, amountDue, paymentMethod, Status.PAID
        );

        reservationManager.add(reservation);

        room.available = false;
        roomManager.save();

        System.out.println("\nBooking confirmed!");
        System.out.println(reservation);
    }

    /** Simulates a payment flow. Returns the payment method string, or null if cancelled. */
    static String simulatePayment(double amountDue) {
        System.out.printf("%nAmount due: $%.2f%n", amountDue);
        System.out.println("1. Cash   2. Card   3. Online   4. Cancel payment");
        int choice = readInt("Select payment method: ");

        String method;
        switch (choice) {
            case 1 -> method = "CASH";
            case 2 -> method = "CARD";
            case 3 -> method = "ONLINE";
            default -> {
                return null;
            }
        }

        System.out.println("Processing " + method + " payment of $" + String.format("%.2f", amountDue) + "...");
        System.out.println("Payment successful.");
        return method;
    }

    // ================= CANCEL =================
    static void cancelBooking() {
        int id = readInt("Enter booking ID to cancel: ");
        Reservation target = reservationManager.findById(id);

        if (target == null) {
            System.out.println("Booking not found.");
            return;
        }

        if (target.status == Status.CANCELLED) {
            System.out.println("This booking is already cancelled.");
            return;
        }

        target.status = Status.CANCELLED;

        Room room = roomManager.findByNumber(target.roomNumber);
        if (room != null) room.available = true;

        roomManager.save();
        reservationManager.save();

        System.out.println("Booking " + id + " cancelled. Room " + target.roomNumber + " is now available.");
    }

    // ================= VIEW BOOKINGS =================
    static void showBookings() {
        if (reservationManager.all().isEmpty()) {
            System.out.println("No bookings yet.");
            return;
        }
        for (Reservation r : reservationManager.all()) System.out.println(r);
    }

    // ================= INPUT HELPERS (crash-proof) =================
    static int readInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                return Integer.parseInt(line);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid whole number.");
            }
        }
    }

    static double readPositiveDouble(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                double val = Double.parseDouble(line);
                if (val < 0) {
                    System.out.println("Value cannot be negative.");
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }
        }
    }

    static LocalDate readDate(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = sc.nextLine().trim();
            try {
                return LocalDate.parse(line, DATE_FMT);
            } catch (DateTimeParseException e) {
                System.out.println("Please enter a date in yyyy-MM-dd format (e.g. 2026-07-15).");
            }
        }
    }

    static Category readCategory() {
        while (true) {
            System.out.print("Category (STANDARD / DELUXE / SUITE): ");
            String line = sc.nextLine().trim();
            Category c = Category.fromString(line);
            if (c != null) return c;
            System.out.println("Invalid category. Please choose STANDARD, DELUXE, or SUITE.");
        }
    }
}
