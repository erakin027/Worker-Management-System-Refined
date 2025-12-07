// Refactored to JSON & SOLID — do not change workflow without review
// Domain models for Admin/Worker modules (User, Admin, Worker, ServiceRequest, Skill)
// All text file operations replaced with JSON repository calls
package app;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Scanner;
import java.util.stream.Collectors;

/**
 * Base User class
 */
abstract class User {
    protected Scanner scanner;
    protected LinkedHashMap<String, Work> workMatchings;
    protected WorkerRepository workerRepo;
    protected ServiceRepository serviceRepo;
    protected AdminRepository adminRepo;
    protected AssignmentService assignmentService;

    public void inject(Scanner scanner, LinkedHashMap<String, Work> workMatchings,WorkerRepository workerRepo,ServiceRepository serviceRepo,
        AdminRepository adminRepo,
        AssignmentService assignmentService){
            this.scanner = scanner;
            this.workMatchings = workMatchings;
            this.workerRepo = workerRepo;
            this.serviceRepo = serviceRepo;
            this.adminRepo = adminRepo;
            this.assignmentService = assignmentService;
        }
    //public abstract void displayMenu(Scanner scanner, LinkedHashMap<String, Work> workMatchings);
    
    public void print(String message) {
        System.out.println(message);
    }
   
    public abstract void handleStart();
    public abstract void displayMenu();
}

/**
 * Admin model - handles worker assignment and pending request processing
 * Uses JSON repositories instead of text files
 * 
 * SOLID Principles Applied:
 * - Single Responsibility: Only handles admin operations (login, process requests)
 * - Dependency Inversion: Depends on repository and service abstractions
 * - Liskov Substitution: Can substitute User (inheritance hierarchy)
 */
class Admin extends User {
    private String adminId;
    private String adminPass;
    private ArrayList<Worker> workers;

    
    // public Admin(String adminId, String adminPass) {
    //     this.adminId = adminId;
    //     this.adminPass = adminPass;
    //     this.workers = new ArrayList<>();
    // }

    public Admin(){}

    @Override
    public void handleStart(){
        print("\n1. Login");
        print("2. Back");

        String ch = scanner.nextLine();
        if (ch.equals("2")) return;

        print("Enter Admin ID:");
        adminId = scanner.nextLine();

        print("Enter Password:");
        adminPass = scanner.nextLine();

        if (adminRepo.verifyCredentials(adminId, adminPass)) {
            print("Admin login successful.");
            this.displayMenu();
        } else {
            print("Invalid credentials.");
        }
    }
    
    @Override
    public void displayMenu() {
        while (true) {
            print("1. Load Pending  and assign workers");
            print("2. Logout");

            int cmd = Integer.parseInt(scanner.nextLine());
            if (cmd == 1) {
                //loadPending(workMatchings);
                assignmentService.processAllPending(workMatchings, scanner);
            } else {
                return;
            }
        }
    }

    public String getId() {
        return adminId;
    }
    
    public String getPass() {
        return adminPass;
    }
    
    /**
     * Load workers from JSON repository
     */
    public void loadWorkers(LinkedHashMap<String, Work> workMatchings) {
        this.workers = workerRepo.loadAll(workMatchings);
    }
    
    
}

/**
 * Worker model - handles worker registration, login, and profile management
 * Uses JSON repositories instead of text files
 * 
 * SOLID Principles Applied:
 * - Single Responsibility: Only handles worker-specific operations
 * - Dependency Inversion: Uses repository abstractions for data access
 * - Liskov Substitution: Can substitute User (inheritance hierarchy)
 * - Open/Closed: Can be extended with new worker features without modifying base User
 */
class Worker extends User {
    private String workerId;
    private String workerPass;
    private String name;
    private String gender;
    private String area;
    private ArrayList<Work> capableWorks;
    private boolean isAvailable;
    private long matchingCount;
    private ArrayList<Integer> bookings;
    
    public Worker() {
        this.capableWorks = new ArrayList<>();
        this.bookings = new ArrayList<>();
        this.isAvailable = true;
        this.matchingCount = 0;
    }
    
    public Worker(String workerId,String workerPass) {
        this();
        this.workerId = workerId;
        this.workerPass = workerPass;
    }
    

    // Getters
    public String getWorkerId() { return workerId; }
    public String getWorkerPass() { return workerPass; }
    public String getName() { return name; }
    public String getGender() { return gender; }
    public String getLocality() { return area; }
    public ArrayList<Work> getCapableWorks() { return capableWorks; }
    public boolean isAvailableNow() { return isAvailable; }
    public long getMatchingCount() { return matchingCount; }
    public ArrayList<Integer> getBookings() { return bookings; }
    
    // Setters
    public void setAvailable(boolean available) { this.isAvailable = available; }
    public void setMatchingCount(long matchingCount) { this.matchingCount = matchingCount; }
    public void addBookingId(int bookingId) { this.bookings.add(bookingId); }
    public void setArea(String area) { this.area = area; }
    public void setCapableWorks(ArrayList<Work> works) { this.capableWorks = works; }
    public void setBookings(ArrayList<Integer> bookings) { this.bookings = bookings; }
    public void setName(String name) { this.name = name; }
    public void setGender(String gender) { this.gender = gender; }
    
    @Override
    public void handleStart() {

        print("\n1. Register");
        print("2. Login");
        print("3. Back");

        String ch = scanner.nextLine();
        if (ch.equals("3")) return;

        if (ch.equals("1")) {
            print("Enter Worker ID:");
            this.workerId = scanner.nextLine();
    
            print("Enter password:");
            this.workerPass = scanner.nextLine();
    
            print("Re-enter password:");
            String repass = scanner.nextLine();
    
            while (!this.workerPass.equals(repass)) {
                print("Passwords do not match. Try again:");
                this.workerPass = scanner.nextLine();
                repass = scanner.nextLine();
            }
    
            this.register(scanner, workMatchings);
            this.displayMenu();
            return;
        }

        // -------- LOGIN --------
        if (ch.equals("2")) {
            print("Enter Worker ID:");
            this.workerId = scanner.nextLine();

            print("Enter password:");
            this.workerPass = scanner.nextLine();

            int status = this.login(workMatchings);

            if (status == 1) {
                this.displayMenu();
            }
        }
    }
    
    /**
     * Look up worker in JSON repository
     */
    public int lookUp(boolean loginCall, LinkedHashMap<String, Work> workMatchings) {
        Worker foundWorker = workerRepo.findById(workerId, workMatchings);
        
        if (foundWorker == null) {
            return -1; // ID doesn't exist
        }
        
        if (loginCall) {
            if (foundWorker.getWorkerPass().equals(workerPass)) {
                // Copy all fields
                this.name = foundWorker.getName();
                this.gender = foundWorker.getGender();
                this.area = foundWorker.getLocality();
                this.capableWorks = foundWorker.getCapableWorks();
                this.isAvailable = foundWorker.isAvailableNow();
                this.bookings = foundWorker.getBookings();
                return 1; // ID and password match
            } else {
                return 0; // ID exists, password incorrect
            }
        } else {
            return 1; // ID exists (for registration check)
        }
    }
    
    /**
     * Register new worker - saves to JSON repository
     */
    public void register(Scanner scanner, LinkedHashMap<String, Work> workMatchings) {
        int existing = lookUp(false, workMatchings);
        if (existing != -1) {
            print("UserId already exists, please login");
            return;
        }
        
        this.capableWorks = new ArrayList<>();
        print("What's your name?");
        name = scanner.nextLine();
        
        print("What is your gender? (Gender should M/F)");
        while(true) {
            String gen = scanner.nextLine();
            if (gen.equals("M")) {
                gender = gen;
                break;
            }
            else if (gen.equals("F")) {
                gender = gen;
                break;
            }
            else {
                print("Gender can be only M or F");
            }
        }
        
        HashMap<Integer, String> areaCodes = new HashMap<>();
        areaCodes.put(1, "Moghalrajpuram");
        areaCodes.put(2, "Bhavanipuram");
        areaCodes.put(3, "Patamata");
        areaCodes.put(4, "Gayatri Nagar");
        areaCodes.put(5, "Benz Circle");
        areaCodes.put(6, "SN Puram");
        print("Which area code do you live in?");
        print("1. Moghalrajpuram  \n2. Bhavanipuram\n3. Patamata\n4. Gayatri Nagar\n5. Benz Circle\n6. SN puram\n");
        int areaCode = Integer.parseInt(scanner.nextLine());
        area = areaCodes.get(areaCode);
        
        ArrayList<String> workNames = new ArrayList<>(workMatchings.keySet());
        print("Available Works:");
        for(int i=0; i< workNames.size(); i++){
            print((i+1)+". " + workNames.get(i));
        }

        print("How many works are you capable of?");
        int num = Integer.parseInt(scanner.nextLine());

        while(true){
            print("Enter exactly " + num + " work numbers (space seperated):");
            String[] inputs = scanner.nextLine().trim().split("\\s+");

            if(inputs.length != num){
                print("Error: Enter exactly " + num + " work numbers.");
                continue;
            }

            capableWorks.clear();
            boolean valid = true;

            for (String input : inputs){
                try {
                    int choice = Integer.parseInt(input);

                    if (choice < 1 || choice > workNames.size()) {
                        print("Invalid work number: " + choice);
                        valid = false;
                        break;
                    }
        
                    String selectedWork = workNames.get(choice - 1);
                    capableWorks.add(workMatchings.get(selectedWork));
        
                } catch (NumberFormatException e) {
                    print("Invalid number format: " + input);
                    valid = false;
                    break;
                }
            }

            if (valid){
                break;
            }else{
                print("Please re-enter the work numbers correctly.\n");
            }
        }
       
        this.bookings = new ArrayList<>();
        
        // Save to JSON repository instead of text file
        workerRepo.add(this);
        print("Worker registered successfully!");
    }
    
    /**
     * Login worker - uses JSON repository
     */
    public int login(LinkedHashMap<String, Work> workMatchings) {
        print("Attempting to log in for Worker ID: " + getWorkerId());
        
        int status = lookUp(true, workMatchings);
        
        switch (status) {
            case 1:
                print("Login successful!");
                print("Welcome back, " + this.name + "!");
                print("Your details:");
                print("Name: " + this.name);
                print("Gender: " + this.gender);
                return 1;
            case 0:
                print("Login failed: Incorrect password for Worker ID: " + getWorkerId());
                return 0;
            case -1:
                print("Login failed: Worker ID does not exist.");
                return 0;
            default:
                print("An unexpected error occurred during login. Please try again.");
                return 0;
        }
    }
    
    /**
     * Display worker menu - preserves original menu structure
     */
    @Override
    public void displayMenu(){
        while(true) {
            print("What do you want to do next? (enter number)");
            print("1.Check your details");
            print("2.Edit Details");
            print("3.View Ongoing Bookings");
            print("4.View Booking History");
            print("5.Change Availability");
            print("6.Mark a service as completed.");
            print("7.Logout.");
            
            int command = Integer.parseInt(scanner.nextLine());
            if(command == 1) {
                checkDetails();
            }
            else if(command == 2) {
                print("Your present details:");
                checkDetails();
                editDetails(scanner, workMatchings);
            }
            else if (command == 3) {
                viewOngoingBookings();
            }
            else if(command == 4){
                viewBookingHistory();
            }
            else if (command == 5) {
                this.isAvailable = !this.isAvailable;
                updateWorkerFile(workerId, workerPass);
                print("Availability Updated.");
            }
            else if(command == 6){
                markWorkAsCompleted();
            }
            else if (command == 7) {
                return;
            }
            else{
                print("Invalid Option. Try again.");
            }
        }
    }
    

    private void viewOngoingBookings() {

        if (bookings.isEmpty()) {
            print("No bookings found.");
            return;
        }
    
        ArrayList<ServiceRequest> allServices = serviceRepo.loadAll(workMatchings);
        boolean found = false;
    
        for (ServiceRequest service : allServices) {
            if (bookings.contains(service.getServiceId()) 
                    && service.getStatus() == Status.ASSIGNED) {
    
                found = true;
    
                print("Service ID: " + service.getServiceId());
                print("Customer: " + service.getCustomerId());
                print("Address: " + service.getAddress());
                print("Work Date: " + service.getWorkDate());
                print("Start Time: " + service.getWorkTime());
            }
        }
    
        if (!found) {
            print("No ongoing bookings.");
        }
    }

    
    private void viewBookingHistory() {

        if (bookings.isEmpty()) {
            print("No booking history found.");
            return;
        }
    
        ArrayList<ServiceRequest> allServices = serviceRepo.loadAll(workMatchings);
        boolean found = false;
    
        for (ServiceRequest service : allServices) {
            if (bookings.contains(service.getServiceId()) 
                    && service.getStatus() == Status.COMPLETED) {
    
                found = true;
                print("Service ID: " + service.getServiceId());
                print("Customer: " + service.getCustomerId());
                print("Address: " + service.getAddress());
                print("Work Date: " + service.getWorkDate());
                print("Start Time: " + service.getWorkTime());
                print("End Time: " + service.getWorkEndTime());
                print("Price: " + service.getPrice());
            }
        }
    
        if (!found) {
            print("No completed services yet.");
        }
    }
    
    /**
     * Edit worker details - updates JSON repository
     */
    public void editDetails(Scanner scanner, LinkedHashMap<String, Work> workMatchings) {
        print("What do you want to edit?");
        print("1. Locality");
        print("2. Re-enter capable Works");
        
        int com = Integer.parseInt(scanner.nextLine());
        
        if (com == 1) {
            HashMap<Integer, String> areaCodes = new HashMap<>();
            areaCodes.put(1, "Moghalrajpuram");
            areaCodes.put(2, "Bhavanipuram");
            areaCodes.put(3, "Patamata");
            areaCodes.put(4, "Gayatri Nagar");
            areaCodes.put(5, "Benz Circle");
            areaCodes.put(6, "SN Puram");
            print("1. Moghalrajpuram  \n2. Bhavanipuram\n3. Patamata\n4. Gayatri Nagar\n5. Benz Circle\n6. SN puram\n");
            int areaCode = Integer.parseInt(scanner.nextLine());
            area = areaCodes.get(areaCode);
        } 
        else if (com == 2) {
            ArrayList<String> workNames = new ArrayList<>(workMatchings.keySet());

            print("Available Works:");
            for (int i = 0; i < workNames.size(); i++) {
                print((i + 1) + ". " + workNames.get(i));
            }

            print("Enter how many total works you are capable of:");
            int num = Integer.parseInt(scanner.nextLine());
            
            while(true){
                print("Select all the works you are capable of (space-seperated):");
                capableWorks.clear();
                String[] inputs = scanner.nextLine().trim().split("\\s+");

                if (inputs.length != num) {
                    print("Error: Enter exactly " + num + " work numbers.");
                    continue;
                }

                boolean valid = true;
                for (String input : inputs) {
                    try {
                        int choice = Integer.parseInt(input);
        
                        if (choice < 1 || choice > workNames.size()) {
                            print("Invalid work number: " + choice);
                            valid = false;
                            break;
                        }
        
                        String selectedWork = workNames.get(choice - 1);
                        Work workToAdd = workMatchings.get(selectedWork);
                        capableWorks.add(workToAdd);
        
                    } catch (NumberFormatException e) {
                        print("Invalid number format: " + input);
                        valid = false;
                        break;
                    }
                }
        
                if (valid) {
                    break;
                } else {
                    print("Please re-enter correctly.\n");
                }
              
            }
        }
        
        // Update in JSON repository
        updateWorkerFile(workerId, workerPass);
        print("Details updated successfully!");
    }
    
    /**
     * Check worker details
     */
    public void checkDetails() {
        print("Your details: ");
        print("Worker Id: " + getWorkerId());
        print("Name: " + getName());
        print("Gender: " + getGender());
        print("Locality: " + getLocality());
        
        ArrayList<Work> capableWorks = getCapableWorks();
        print("Number of Works you can do: " + String.valueOf(capableWorks.size()));
        String works = capableWorks.stream().map(Work::getWorkName).collect(Collectors.joining(", ", "[", "]"));
        print(works);
    }

    private void markWorkAsCompleted() {

        if (bookings.isEmpty()) {
            print("You have no bookings.");
            return;
        }
    
        ArrayList<ServiceRequest> allServices = serviceRepo.loadAll(workMatchings);
        ArrayList<ServiceRequest> myActiveServices = new ArrayList<>();
    
        // Show only ASSIGNED services for this worker
        for (ServiceRequest service : allServices) {
            if (bookings.contains(service.getServiceId()) 
                    && service.getStatus() == Status.ASSIGNED) {
    
                myActiveServices.add(service);
    
                print("Service ID: " + service.getServiceId());
                print("Customer: " + service.getCustomerId());
                print("Address: " + service.getAddress());
                print("Work Date: " + service.getWorkDate());
                print("Start Time: " + service.getWorkTime());
                print("End Time: " + service.getWorkEndTime());
            }
        }
    
        if (myActiveServices.isEmpty()) {
            print("No active services to complete.");
            return;
        }
    
        print("Enter Service ID to mark as COMPLETED:");
        int selectedId = Integer.parseInt(scanner.nextLine());
    
        boolean found = false;
    
        for (ServiceRequest service : myActiveServices) {
            if (service.getServiceId() == selectedId) {
                service.setStatus(Status.COMPLETED);
                LocalTime now = LocalTime.now();
                service.setWorkEndtime(now.toString());
                serviceRepo.update(service);
                this.isAvailable = true;
                workerRepo.update(this);
    
                print("Service marked as COMPLETED successfully.");
                found = true;
                break;
            }
        }
    
        if (!found) {
            print("Invalid Service ID.");
        }
    }
    
    
    
    /**
     * Update worker file - uses JSON repository instead of text file
     */
    public void updateWorkerFile(String workerId, String workerPass) {
        workerRepo.update(this);
    }
}

/**
 * ServiceRequest model - represents a service request
 * Replaces Immediate/Scheduled hierarchy with single class using type field
 * 
 * SOLID Principles Applied:
 * - Single Responsibility: Only represents service request data
 * - Open/Closed: Type field allows extension without class hierarchy
 * - Interface Segregation: Clean getters/setters, no unnecessary methods
 */
class ServiceRequest {
    private int serviceId;
    private Status status;
    private ServiceType typeName;
    private Plan planName;
    private String bookingDate;
    private String bookingTime;
    private String locality;
    private String customerId;
    private String customerGender;
    private String address;
    private ArrayList<Work> requestedWorks;
    private GenderPref preferredGender;
    private String scheduledDate;
    private String scheduledTime;
    private double price;
    private String workDate;
    private String workStartTime;
    private String workEndTime;
    private ArrayList<String> assignedWorkerIds;
    private String reason;
    
    public ServiceRequest(String[] fields, LinkedHashMap<String, Work> workMatchings) {
        this.serviceId = Integer.parseInt(fields[0]);
        this.status = Status.fromInt(Integer.parseInt(fields[1]));
        this.typeName = ServiceType.fromString(fields[2]);
        this.planName = Plan.fromString(fields[3]);
        this.bookingDate = fields[4];
        this.bookingTime = fields[5];
        this.locality = fields[6];
        this.customerId = fields[7];
        this.customerGender = fields[8];
        this.address = fields[9];
        this.requestedWorks = new ArrayList<>();
        
        String[] works = fields[10].substring(1, fields[10].length() - 1).split(",");
        for (String work : works) {
            if (!work.trim().isEmpty()) {
                this.requestedWorks.add(workMatchings.get(work.trim()));
            }
        }
        this.preferredGender = GenderPref.fromString(fields[11]);
        
        this.scheduledDate = fields[12];
        this.scheduledTime = fields[13];

        // Work timing depends on service type
        if (this.typeName == ServiceType.IMMEDIATE) {
            // Immediate = work happens now
            this.workDate = this.bookingDate;
            this.workStartTime = this.bookingTime;
            this.workEndTime = fields[16];   // usually empty initially
        }

        else if (this.typeName == ServiceType.SCHEDULING) {
            // Scheduled = work happens in future
            this.workDate = this.scheduledDate;
            this.workStartTime = this.scheduledTime;
            this.workEndTime = fields[16];   // filled after assignment
        }
    }
    
    // Getters
    public int getServiceId() { return serviceId; }
    public Status getStatus() { return status; }
    public ServiceType getTypeName() { return typeName; }
    public Plan getPlanName() { return planName; }
    public String getLocality() { return locality; }
    public String getCustomerId() { return customerId; }
    public String getCustomerGender() { return customerGender; }
    public GenderPref getPreferredGender() { return preferredGender; }
    public ArrayList<Work> getRequestedWorks() { return requestedWorks; }
    public String getScheduledDate() { return scheduledDate; }
    public String getScheduledTime() { return scheduledTime; }
    public String getWorkDate() { return workDate; }
    public String getWorkTime() { return workStartTime; }
    public String getWorkEndTime() { return workEndTime; }
    public double getPrice() { return price; }
    public String getAddress() { return address; }
    public String getBookingDate() { return bookingDate; }
    public String getBookingTime() { return bookingTime; }
    public String getReason(){ return reason; }
    public ArrayList<String> getAssignedWorkerIds() { return assignedWorkerIds != null ? assignedWorkerIds : new ArrayList<>(); }
    
    // Setters
    public void setStatus(Status status) { this.status = status; }
    public void setReason(String reason){ this.reason = reason;}
    public void setPrice(double price) { this.price = price; }
    public void incrementPrice(double price) { this.price += price; }
    public void setWorkDate(String date) { this.workDate = date; }
    public void setWorkTime(String time) { this.workStartTime = time; }
    public void setWorkEndtime(String endTime) { this.workEndTime = endTime; }
    public void setAssignedWorkerIds(ArrayList<String> ids) { this.assignedWorkerIds = ids; }
    
    public double getDiscount() {
        switch (planName) {
            case BASIC: return 0;
            case INTERMEDIATE: return 0.1;
            case PREMIUM: return 0.2;
            default: return 0;
        }
    }
}

/**
 * Work hierarchy - same as original
 */
class Work {
    private int id;
    private String workName;
    private int time;
    private double price;
    
    public Work(int id, String workName, int time, double price) {
        this.id = id;
        this.workName = workName;
        this.time = time;
        this.price = price;
    }
    
    public int getId() { return id; }
    public String getWorkName() { return workName; }
    public int getTimeRequired() { return time; }
    public Double getPrice() { return price; }


    //@Override
    public String toString() {
        return String.format("Work{id=%d, name='%s', time=%d, price=%.2f}",
                id, workName, time, price);
    }
}

