// Refactored to JSON & SOLID — do not change workflow without review
// Repository layer for JSON persistence (Worker, Service, Admin repositories)
// Implements atomic file writes (temp file + rename) for data integrity
package app;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import com.google.gson.JsonParser;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.io.*;

class WorkRepository{
    private String filePath;
    private Gson gson;

    private LinkedHashMap<String, Work> workByName = new LinkedHashMap<>();
    //private HashMap<Integer, Work> workById = new HashMap<>();

    /**
     * Constructor - initializes repository with file path
     * @param filePath Path to works_config.json
     */
    public WorkRepository(String filePath) {
        this.filePath = filePath;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        ensureDirectoryExists();
    }

    /**
     * Ensure directory exists
     */
    private void ensureDirectoryExists() {
        try {
            File file = new File(filePath);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
        } catch (Exception e) {
            System.err.println("Error creating directory: " + e.getMessage());
        }
    }

    /**
     * Load all works from JSON file
     * This must be called once at program startup before using the repository.
     */
    public void loadAllWorks() {
        //workByName.clear();
        try {
            File file = new File(filePath);
            if (!file.exists() || file.length() == 0) {
                System.err.println("Warning: works_config.json is missing or empty: " + filePath);
                return;
            }

            try (JsonReader jr = new JsonReader(new FileReader(file))) {
                JsonElement rootEl = JsonParser.parseReader(jr);
                if (rootEl == null || !rootEl.isJsonObject()) {
                    System.err.println("Invalid works_config.json: root is not an object.");
                    return;
                }
                JsonObject root = rootEl.getAsJsonObject();
                
                JsonArray worksArr = null;
                if (root.has("works") && root.get("works").isJsonArray()) {
                    worksArr = root.getAsJsonArray("works");
                } else {
                    System.err.println("works_config.json: 'works' array is missing or invalid.");
                    return;
                }
                

                for (JsonElement el : worksArr) {
                    if(!el.isJsonObject()) continue;
                    JsonObject w = el.getAsJsonObject();

                    int id = w.get("id").getAsInt();
                    String name = w.get("name").getAsString();
                    int timeMinutes = w.get("timeMinutes").getAsInt();
                    double price = w.get("price").getAsDouble();

                    Work work = new Work(id, name, timeMinutes, price);
                    if (!workByName.containsKey(name)) {
                        workByName.put(name, work);
                    } 
                    else {
                        System.err.println("Duplicate work name in works_config.json (ignored): " + name);
                    }    
                }
            }
        }catch (FileNotFoundException e) {
            System.err.println("works_config.json not found: " + filePath);
        } catch (Exception e) {
            System.err.println("Error loading works: " + e.getMessage());
        }
    }

    /**
     * Get work by name
     */
    public Work getWorkByName(String name) {
        return workByName.get(name);
    }

    /**
     * Get work by ID
     */
    // public Work getWorkById(int id) {
    //     return workById.get(id);
    // }

    /**
     * Get all works as an ordered list
     */
    public LinkedHashMap<String, Work> getAllWorks() {
        return workByName;
    }

    public boolean containsWOrkName(String name){
        return workByName.containsKey(name);
    }

    // /**
    //  * Atomic write for saving modified work list (rarely needed)
    //  */
    // private void writeToFileAtomically(JsonObject root) {
    //     String tempPath = filePath + ".tmp";
    //     try {
    //         try (FileWriter writer = new FileWriter(tempPath)) {
    //             gson.toJson(root, writer);
    //         }

    //         File tmp = new File(tempPath);
    //         File target = new File(filePath);

    //         if (target.exists()) target.delete();
    //         tmp.renameTo(target);

    //     } catch (IOException e) {
    //         System.err.println("Error writing works file: " + e.getMessage());
    //         new File(tempPath).delete();
    //     }
    // }
}  

/**
 * WorkerRepository - handles worker data persistence using JSON
 * Follows Repository Pattern and Single Responsibility Principle
 * Uses atomic file writes to prevent data corruption
 */
class WorkerRepository {
    private String filePath;
    private Gson gson;
    
    /**
     * Constructor - initializes repository with file path
     * @param filePath Path to workers.json file
     */
    public WorkerRepository(String filePath) {
        this.filePath = filePath;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        ensureDirectoryExists();
    }
    
    /**
     * Ensure data directory exists, create if it doesn't
     */
    private void ensureDirectoryExists() {
        try {
            java.io.File file = new java.io.File(filePath);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
        } catch (Exception e) {
            System.err.println("Error creating directory: " + e.getMessage());
        }
    }
    
    /**
     * Load all workers from JSON file
     * @param workMatchings Map of work types for deserialization
     * @return ArrayList of all workers
     */
    public ArrayList<Worker> loadAll(LinkedHashMap<String, Work> workMatchings) {
        ArrayList<Worker> workers = new ArrayList<>();
        try {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists() || file.length() == 0) {
                return workers; // Return empty list if file doesn't exist
            }
            
            JsonArray jsonArray = readFromFile();
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObj = jsonArray.get(i).getAsJsonObject();
                Worker worker = deserializeWorker(jsonObj, workMatchings);
                if (worker != null) {
                    workers.add(worker);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading workers: " + e.getMessage());
        }
        return workers;
    }
    
    /**
     * Save all workers to JSON file using atomic write
     * @param workers ArrayList of workers to save
     */
    public void saveAll(ArrayList<Worker> workers) {
        try {
            JsonArray jsonArray = new JsonArray();
            for (Worker worker : workers) {
                jsonArray.add(serializeWorker(worker));
            }
            writeToFileAtomically(jsonArray);
        } catch (Exception e) {
            System.err.println("Error saving workers: " + e.getMessage());
        }
    }
    
    /**
     * Find worker by ID
     * @param workerId Worker ID to search for
     * @param workMatchings Map of work types for deserialization
     * @return Worker if found, null otherwise
     */
    public Worker findById(String workerId, java.util.LinkedHashMap<String, Work> workMatchings) {
        ArrayList<Worker> workers = loadAll(workMatchings);
        for (Worker worker : workers) {
            if (worker.getWorkerId().equals(workerId)) {
                return worker;
            }
        }
        return null;
    }
    
    /**
     * Add new worker to repository
     * @param worker Worker to add
     */
    public void add(Worker worker) {
        ArrayList<Worker> workers = loadAll(new LinkedHashMap<>());
        workers.add(worker);
        saveAll(workers);
    }
    
    /**
     * Update existing worker in repository
     * @param worker Worker with updated information
     */
    public void update(Worker worker) {
        ArrayList<Worker> workers = loadAll(new java.util.LinkedHashMap<>());
        for (int i = 0; i < workers.size(); i++) {
            if (workers.get(i).getWorkerId().equals(worker.getWorkerId())) {
                workers.set(i, worker);
                break;
            }
        }
        saveAll(workers);
    }
    
    /**
     * Find eligible workers based on filters (locality, gender, skills, availability)
     * @param locality Required locality
     * @param genderPref Gender preference filter
     * @param requestedWorks List of required work types
     * @param workMatchings Map of work types
     * @return ArrayList of eligible workers
     */
    public ArrayList<Worker> findEligibleWorkers(String locality, GenderPref genderPref, 
                                                 ArrayList<Work> requestedWorks, 
                                                 java.util.LinkedHashMap<String, Work> workMatchings) {
        ArrayList<Worker> allWorkers = loadAll(workMatchings);
        ArrayList<Worker> eligible = new ArrayList<>();
        
        for (Worker worker : allWorkers) {
            // Check locality match
            if (!worker.getLocality().equals(locality)) {
                continue;
            }
            
            // Check gender preference match
            if (genderPref != GenderPref.NP && !worker.getGender().equals(genderPref.getValue())) {
                continue;   
            }
            
            // Check if worker has at least one requested skill
            boolean hasSkill = false;
            for (Work requestedWork : requestedWorks) {
                if (worker.getCapableWorks().contains(requestedWork)) {
                    hasSkill = true;
                    break;
                }
            }
            
            if (hasSkill) {
                eligible.add(worker);
            }
        }
        
        return eligible;
    }
    
    /**
     * Atomic file write - writes to temp file then renames to prevent corruption
     * 
     * Design Pattern: Atomic Operation - ensures data integrity
     * If write fails, original file remains intact (no partial writes)
     * 
     * Single Responsibility: Only handles atomic file writing, no business logic
     * 
     * @param data JsonArray to write
     */
    private void writeToFileAtomically(JsonArray data) {
        String tempPath = filePath + ".tmp";
        try {
            // Write to temp file first (not directly to target)
            // This ensures original file is not corrupted if write fails
            try (FileWriter writer = new FileWriter(tempPath)) {
                gson.toJson(data, writer);
            }
            
            // Atomic rename - this is an atomic operation on most filesystems
            // Either succeeds completely or fails completely (no partial state)
            java.io.File tempFile = new java.io.File(tempPath);
            java.io.File targetFile = new java.io.File(filePath);
            if (targetFile.exists()) {
                targetFile.delete();
            }
            tempFile.renameTo(targetFile);
        } catch (IOException e) {
            System.err.println("Error writing workers file: " + e.getMessage());
            // Clean up temp file on error to prevent orphaned files
            new java.io.File(tempPath).delete();
        }
    }
    
    /**
     * Read JSON array from file
     * @return JsonArray from file, empty array if file doesn't exist
     */
    private JsonArray readFromFile() {
        try {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists() || file.length() == 0) {
                return new JsonArray();
            }
            
            try (FileReader reader = new FileReader(filePath)) {
                return gson.fromJson(reader, JsonArray.class);
            }
        } catch (IOException e) {
            System.err.println("Error reading workers file: " + e.getMessage());
            return new JsonArray();
        }
    }
    
    /**
     * Serialize Worker to JsonObject
     * @param worker Worker to serialize
     * @return JsonObject representation
     */
    private JsonObject serializeWorker(Worker worker) {
        JsonObject json = new JsonObject();
        json.addProperty("workerId", worker.getWorkerId());
        json.addProperty("workerPass", worker.getWorkerPass());
        json.addProperty("name", worker.getName());
        json.addProperty("gender", worker.getGender());
        json.addProperty("area", worker.getLocality());
        json.addProperty("isAvailable", worker.isAvailableNow());
        json.addProperty("matchingCount", worker.getMatchingCount());
        
        // Serialize capable works
        JsonArray worksArray = new JsonArray();
        for (Work work : worker.getCapableWorks()) {
            worksArray.add(work.getWorkName());
        }
        json.add("capableWorks", worksArray);
        
        // Serialize bookings
        JsonArray bookingsArray = new JsonArray();
        for (Integer booking : worker.getBookings()) {
            bookingsArray.add(booking);
        }
        json.add("bookings", bookingsArray);
        
        return json;
    }
    
    /**
     * Deserialize JsonObject to Worker
     * @param json JsonObject to deserialize
     * @param workMatchings Map of work types
     * @return Worker object
     */
    private Worker deserializeWorker(JsonObject json, java.util.LinkedHashMap<String, Work> workMatchings) {
        try {
            String workerId = json.get("workerId").getAsString();
            String workerPass = json.get("workerPass").getAsString();
            Worker worker = new Worker(workerId, workerPass);
            
            worker.setName(json.get("name").getAsString());
            worker.setGender(json.get("gender").getAsString());
            worker.setArea(json.get("area").getAsString());
            worker.setAvailable(json.get("isAvailable").getAsBoolean());
            worker.setMatchingCount(json.get("matchingCount").getAsLong());
            
            // Deserialize capable works
            JsonArray worksArray = json.getAsJsonArray("capableWorks");
            ArrayList<Work> capableWorks = new ArrayList<>();
            for (int i = 0; i < worksArray.size(); i++) {
                String workName = worksArray.get(i).getAsString();
                Work work = workMatchings.get(workName);
                if (work != null) {
                    capableWorks.add(work);
                }
            }
            worker.setCapableWorks(capableWorks);
            
            // Deserialize bookings
            JsonArray bookingsArray = json.getAsJsonArray("bookings");
            ArrayList<Integer> bookings = new ArrayList<>();
            for (int i = 0; i < bookingsArray.size(); i++) {
                bookings.add(bookingsArray.get(i).getAsInt());
            }
            worker.setBookings(bookings);
            
            return worker;
        } catch (Exception e) {
            System.err.println("Error deserializing worker: " + e.getMessage());
            return null;
        }
    }
}

/**
 * ServiceRepository - handles service request data persistence using JSON
 * Follows Repository Pattern and Single Responsibility Principle
 * Uses atomic file writes to prevent data corruption
 */
class ServiceRepository {
    private String filePath;
    private Gson gson;
    
    /**
     * Constructor - initializes repository with file path
     * @param filePath Path to services.json file
     */
    public ServiceRepository(String filePath) {
        this.filePath = filePath;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        ensureDirectoryExists();
    }
    
    /**
     * Ensure data directory exists, create if it doesn't
     */
    private void ensureDirectoryExists() {
        try {
            java.io.File file = new java.io.File(filePath);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
        } catch (Exception e) {
            System.err.println("Error creating directory: " + e.getMessage());
        }
    }
    
    /**
     * Load all service requests from JSON file
     * @param workMatchings Map of work types for deserialization
     * @return ArrayList of all service requests
     */
    public ArrayList<ServiceRequest> loadAll(LinkedHashMap<String, Work> workMatchings) {
        ArrayList<ServiceRequest> services = new ArrayList<>();
        try {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists() || file.length() == 0) {
                return services; // Return empty list if file doesn't exist
            }
            
            JsonArray jsonArray = readFromFile();
            for (int i = 0; i < jsonArray.size(); i++) {
                JsonObject jsonObj = jsonArray.get(i).getAsJsonObject();
                ServiceRequest service = deserializeServiceRequest(jsonObj, workMatchings);
                if (service != null) {
                    services.add(service);
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading services: " + e.getMessage());
        }
        return services;
    }
    
    /**
     * Save all service requests to JSON file using atomic write
     * @param services ArrayList of service requests to save
     */
    public void saveAll(ArrayList<ServiceRequest> services) {
        try {
            JsonArray jsonArray = new JsonArray();
            for (ServiceRequest service : services) {
                jsonArray.add(serializeServiceRequest(service));
            }
            writeToFileAtomically(jsonArray);
        } catch (Exception e) {
            System.err.println("Error saving services: " + e.getMessage());
        }
    }
    
    /**
     * Find service request by ID
     * @param serviceId Service ID to search for
     * @param workMatchings Map of work types for deserialization
     * @return ServiceRequest if found, null otherwise
     */
    public ServiceRequest findById(int serviceId,LinkedHashMap<String, Work> workMatchings) {
        ArrayList<ServiceRequest> services = loadAll(workMatchings);
        for (ServiceRequest service : services) {
            if (service.getServiceId() == serviceId) {
                return service;
            }
        }
        return null;
    }

    
    /**
     * Update existing service request in repository
     * @param service ServiceRequest with updated information
     */
    public void update(ServiceRequest service) {
        ArrayList<ServiceRequest> services = loadAll(new LinkedHashMap<>());
        for (int i = 0; i < services.size(); i++) {
            if (services.get(i).getServiceId() == service.getServiceId()) {
                services.set(i, service);
                break;
            }
        }
        saveAll(services);
    }
    
    
    /**
     * Find service requests by customer ID
     * @param customerId Customer ID to search for
     * @param workMatchings Map of work types for deserialization
     * @return ArrayList of service requests for the customer
     */
    public ArrayList<ServiceRequest> findByCustomer(String customerId, 
                                                     java.util.LinkedHashMap<String, Work> workMatchings) {
        ArrayList<ServiceRequest> allServices = loadAll(workMatchings);
        ArrayList<ServiceRequest> result = new ArrayList<>();
        for (ServiceRequest service : allServices) {
            if (service.getCustomerId().equals(customerId)) {
                result.add(service);
            }
        }
        return result;
    }
    
    /**
     * Find service requests by status
     * @param status Status to filter by
     * @param workMatchings Map of work types for deserialization
     * @return ArrayList of service requests with the specified status
     */
    public ArrayList<ServiceRequest> findByStatus(Status status, 
                                                  java.util.LinkedHashMap<String, Work> workMatchings) {
        ArrayList<ServiceRequest> allServices = loadAll(workMatchings);
        ArrayList<ServiceRequest> result = new ArrayList<>();
        for (ServiceRequest service : allServices) {
            if (service.getStatus() == status) {
                result.add(service);
            }
        }
        return result;
    }
    
    /**
     * Atomic file write - writes to temp file then renames to prevent corruption
     * @param data JsonArray to write
     */
    private void writeToFileAtomically(JsonArray data) {
        String tempPath = filePath + ".tmp";
        try {
            // Write to temp file
            try (FileWriter writer = new FileWriter(tempPath)) {
                gson.toJson(data, writer);
            }
            
            // Atomic rename
            java.io.File tempFile = new java.io.File(tempPath);
            java.io.File targetFile = new java.io.File(filePath);
            if (targetFile.exists()) {
                targetFile.delete();
            }
            tempFile.renameTo(targetFile);
        } catch (IOException e) {
            System.err.println("Error writing services file: " + e.getMessage());
            // Clean up temp file on error
            new java.io.File(tempPath).delete();
        }
    }
    
    /**
     * Read JSON array from file
     * @return JsonArray from file, empty array if file doesn't exist
     */
    private JsonArray readFromFile() {
        try {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists() || file.length() == 0) {
                return new JsonArray();
            }
            
            try (FileReader reader = new FileReader(filePath)) {
                return gson.fromJson(reader, JsonArray.class);
            }
        } catch (IOException e) {
            System.err.println("Error reading services file: " + e.getMessage());
            return new JsonArray();
        }
    }
    
    /**
     * Serialize ServiceRequest to JsonObject
     * @param service ServiceRequest to serialize
     * @return JsonObject representation
     */
    private JsonObject serializeServiceRequest(ServiceRequest service) {
        JsonObject json = new JsonObject();
        json.addProperty("id", service.getServiceId());
        json.addProperty("status", service.getStatus().getValue());
        json.addProperty("type", service.getTypeName().getDisplayName());
        json.addProperty("plan", service.getPlanName().getDisplayName());
        json.addProperty("bookingDate", service.getBookingDate());
        json.addProperty("bookingTime", service.getBookingTime());
        json.addProperty("locality", service.getLocality());
        json.addProperty("customerID", service.getCustomerId());
        json.addProperty("customerGender", service.getCustomerGender());
        json.addProperty("address", service.getAddress());
        json.addProperty("genderPref", service.getPreferredGender() != null ? service.getPreferredGender().getValue() : "NP");
        
        if (service.getReason() != null && !service.getReason().isEmpty()) {
            json.addProperty("rejectionReason", service.getReason());
        }
        
        if (service.getScheduledDate() != null && !service.getScheduledDate().isEmpty())
            json.addProperty("scheduledDate", service.getScheduledDate());
    
        if (service.getScheduledTime() != null && !service.getScheduledTime().isEmpty())
            json.addProperty("scheduledTime", service.getScheduledTime());
    
        if (service.getWorkDate() != null && !service.getWorkDate().isEmpty())
            json.addProperty("workDate", service.getWorkDate());
    
        if (service.getWorkTime() != null && !service.getWorkTime().isEmpty())
            json.addProperty("workStartTime", service.getWorkTime());
    
        if (service.getWorkEndTime() != null && !service.getWorkEndTime().isEmpty())
            json.addProperty("workEndTime", service.getWorkEndTime());

        json.addProperty("price", service.getPrice());
        
        // Serialize requested works
        JsonArray worksArray = new JsonArray();
        for (Work work : service.getRequestedWorks()) {
            worksArray.add(work.getWorkName());
        }
        json.add("requestedServices", worksArray);
        
        // Serialize assigned worker IDs
        JsonArray workersArray = new JsonArray();
        if (service.getAssignedWorkerIds() != null) {
            for (String workerId : service.getAssignedWorkerIds()) {
                workersArray.add(workerId);
            }
        }
        json.add("assignedWorkerIds", workersArray);
        
        return json;
    }
    
    /**
     * Deserialize JsonObject to ServiceRequest
     * @param json JsonObject to deserialize
     * @param workMatchings Map of work types
     * @return ServiceRequest object
     */
    private ServiceRequest deserializeServiceRequest(JsonObject json, java.util.LinkedHashMap<String, Work> workMatchings) {
        try {
            String[] fields = new String[17];
            fields[0] = String.valueOf(json.get("id").getAsInt());
            fields[1] = String.valueOf(json.get("status").getAsInt());
            fields[2] = json.get("type").getAsString();
            fields[3] = json.get("plan").getAsString();
            fields[4] = json.get("bookingDate").getAsString();
            fields[5] = json.get("bookingTime").getAsString();
            fields[6] = json.get("locality").getAsString();
            fields[7] = json.get("customerID").getAsString();
            fields[8] = json.get("customerGender").getAsString();
            fields[9] = json.get("address").getAsString();
            
            // Deserialize requested works
            JsonArray worksArray = json.getAsJsonArray("requestedServices");
            StringBuilder worksStr = new StringBuilder("[");
            for (int i = 0; i < worksArray.size(); i++) {
                if (i > 0) worksStr.append(",");
                worksStr.append(worksArray.get(i).getAsString());
            }
            worksStr.append("]");
            fields[10] = worksStr.toString();
            
            fields[11] = json.get("genderPref").getAsString();
            fields[12] = json.has("scheduledDate") ? json.get("scheduledDate").getAsString() : "";
            fields[13] = json.has("scheduledTime") ? json.get("scheduledTime").getAsString() : "";
            fields[14] = json.has("workDate") ? json.get("workDate").getAsString() : "";
            fields[15] = json.has("workStartTime") ? json.get("workStartTime").getAsString() : "";
            fields[16] = json.has("workEndTime") ? json.get("workEndTime").getAsString() : "";
            
            // Create ServiceRequest (replaces Immediate/Scheduled hierarchy)
            ServiceRequest service = new ServiceRequest(fields, workMatchings);
            
            // Set additional fields
            if (json.has("price")) {
                service.setPrice(json.get("price").getAsDouble());
            }
            
            if (json.has("assignedWorkerIds")) {
                JsonArray workersArray = json.getAsJsonArray("assignedWorkerIds");
                ArrayList<String> workerIds = new ArrayList<>();
                for (int i = 0; i < workersArray.size(); i++) {
                    workerIds.add(workersArray.get(i).getAsString());
                }
                service.setAssignedWorkerIds(workerIds);
            }
            if (json.has("reason")) {
                service.setReason(json.get("reason").getAsString());
            }
            return service;
        } catch (Exception e) {
            System.err.println("Error deserializing service request: " + e.getMessage());
            return null;
        }
    }
}

/**
 * AdminRepository - handles admin data persistence using JSON
 * Follows Repository Pattern and Single Responsibility Principle
 */
class AdminRepository {
    private String filePath;
    private Gson gson;
    

    public AdminRepository(String filePath) {
        this.filePath = filePath;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        ensureDirectoryExists();
        ensureDefaultAdmin();
    }

    
    /**
     * Ensure data directory exists, create if it doesn't
     */
    private void ensureDirectoryExists() {
        try {
            java.io.File file = new java.io.File(filePath);
            if (file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
        } catch (Exception e) {
            System.err.println("Error creating directory: " + e.getMessage());
        }
    }
    
    /**
     * Load admin credentials from JSON file
     * @return Admin object if found, null otherwise
     */

    public void ensureDefaultAdmin() {
        try {
            File file = new File(filePath);
            if (!file.exists() || file.length() == 0) {
                JsonObject json = new JsonObject();
                json.addProperty("adminId", "Admin");
                json.addProperty("adminPass", "admin123");
                
                try (FileWriter writer = new FileWriter(filePath)) {
                    gson.toJson(json, writer);
                }
            }
        }
        catch (IOException e) {
            System.err.println("Error initializing admin: " + e.getMessage());
        }
    }
    

    /**
     * Verify admin credentials
     */
    public boolean verifyCredentials(String adminId, String adminPass) {
        try (FileReader reader = new FileReader(filePath)) {
            JsonObject json = gson.fromJson(reader, JsonObject.class);

            String storedId = json.get("adminId").getAsString();
            String storedPass = json.get("adminPass").getAsString();

            return storedId.equals(adminId) && storedPass.equals(adminPass);
        } 
        catch (IOException e) {
            System.err.println("Error verifying admin: " + e.getMessage());
            return false;
        }
    }
}
