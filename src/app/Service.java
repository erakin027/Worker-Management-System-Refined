// Refactored to JSON & SOLID — do not change workflow without review
// Service layer for business logic (AssignmentService, FilterService)
// Preserves existing assignment logic: immediate auto-assign, scheduled manual assign
package app;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

/**
 * AssignmentService - handles worker assignment logic
 * 
 * SOLID Principles Applied:
 * - Single Responsibility: Only handles worker assignment logic, no UI or data access
 * - Dependency Inversion: Depends on repository interfaces, can swap implementations easily
 * - Open/Closed: Assignment strategies can be extended without modifying this class
 * 
 * Preserves original immediate and scheduled request processing logic
 */
class AssignmentService {
    private WorkerRepository workerRepo;
    private ServiceRepository serviceRepo;
    
    /**
     * Constructor - initializes assignment service with repositories
     * Dependency Inversion: Accepts repository abstractions, not concrete implementations
     * @param workerRepo Worker repository for data access
     * @param serviceRepo Service repository for data access
     */
    public AssignmentService(WorkerRepository workerRepo, ServiceRepository serviceRepo) {
        this.workerRepo = workerRepo;
        this.serviceRepo = serviceRepo;
    }
    

    /**
     * Load pending requests and process them using AssignmentService
     * Replaces text file reading with JSON repository calls
     */
    public void processAllPending(LinkedHashMap<String, Work> workMatchings, Scanner scanner) {
        ArrayList <Worker> workers = workerRepo.loadAll(workMatchings);

        // Load all pending service requests from JSON
        ArrayList<ServiceRequest> pendingRequests = serviceRepo.findByStatus(Status.PENDING, workMatchings);
        
        // Load all services to update them
        ArrayList<ServiceRequest> allServices = serviceRepo.loadAll(workMatchings);
        
        for (ServiceRequest request : pendingRequests) {
            if (request.getStatus() == Status.PENDING) {
                // Process based on request type - this updates the request object
                if (request.getTypeName() == ServiceType.IMMEDIATE) {
                    processImmediateRequest(request, workers);
                } else {
                    processScheduledRequest(request, workers, scanner);
                }
                
                // Update the request in allServices list
                for (int i = 0; i < allServices.size(); i++) {
                    if (allServices.get(i).getServiceId() == request.getServiceId()) {
                        allServices.set(i, request);
                        break;
                    }
                }
            }
        }
        
        if (!allServices.isEmpty()) {
            serviceRepo.saveAll(allServices);
        } else {
            System.out.println("Prevented empty overwrite of services.json");
        }
    }
    /**
     * Process immediate request - auto-assigns workers
     * Logic: Filters workers by locality, gender, availability, then assigns using greedy algorithm
     * Updates the request object directly instead of returning a string
     * @param request ServiceRequest to process (updated in place)
     * @param workers List of all workers
     */
    public void processImmediateRequest(ServiceRequest request, ArrayList<Worker> workers) {
        // Step 1: Filter workers by locality, gender preference, and availability
        ArrayList<Worker> sameLocality = FilterService.filterWorkers(
            workers,
            request.getLocality(),
            request.getPreferredGender(),  // This returns GenderPref enum
            request.getRequestedWorks(),
            true  // available only
        );
        
        // Step 2: Use greedy set-cover algorithm to assign works to workers
        LinkedHashMap<Worker, ArrayList<Work>> workAssignments = FilterService.assignWorkers(
            request.getRequestedWorks(),
            sameLocality
        );
        
        ArrayList<Work> remainingWorks = new ArrayList<>(request.getRequestedWorks());
        
        // Check if all works were assigned
        for (Work work : request.getRequestedWorks()) {
            boolean assigned = false;
            for (ArrayList<Work> assignedWorks : workAssignments.values()) {
                if (assignedWorks.contains(work)) {
                    assigned = true;
                    remainingWorks.remove(work);
                    break;
                }
            }
        }
        
        // Step 3: If any work couldn't be assigned, reject the request
        if (!remainingWorks.isEmpty()) {
            request.setStatus(Status.REJECTED);
        
            String reason = "No worker available for the following works: " +remainingWorks.stream().map(Work::getWorkName).collect(Collectors.joining(", "));
            request.setReason(reason);
        
            System.out.println("Immediate request rejected for "+ request.getServiceId() + " : " + reason);
            return;
        }
        
        // Step 4: Set work date and time to current
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        request.setWorkDate(now.format(dateFormatter));
        request.setWorkTime(now.format(timeFormatter));
        
        // Step 5: Update worker availability and add booking IDs
        for (Worker worker : workAssignments.keySet()) {
            worker.setAvailable(false);
            worker.addBookingId(request.getServiceId());
            workerRepo.update(worker);
        }
        
        // Step 6: Calculate end time based on longest worker assignment
        int maxTime = 0;
        for (Map.Entry<Worker, ArrayList<Work>> entry : workAssignments.entrySet()) {
            ArrayList<Work> works = entry.getValue();
            int sumTime = works.stream().mapToInt(Work::getTimeRequired).sum();
            maxTime = Math.max(maxTime, sumTime);
        }
        request.setWorkEndtime(calculateEndTime(request.getWorkTime(), maxTime));
        
        // Step 7: Set status to assigned and calculate price
        request.setStatus(Status.ASSIGNED);
        
        // Generate matched worker IDs string
        String matchedWorkers = "[" + workAssignments.keySet().stream()
            .map(Worker::getWorkerId)
            .collect(Collectors.joining(",")) + "]";
        
        // Calculate price with discount
        double price = calculatePrice(request.getRequestedWorks(), request.getPlanName());
        request.setPrice(price);
        
        // Set assigned worker IDs
        ArrayList<String> workerIdList = new ArrayList<>();
        for (Worker worker : workAssignments.keySet()) {
            workerIdList.add(worker.getWorkerId());
        }
        request.setAssignedWorkerIds(workerIdList);
        
        System.out.println("Immediate request processed for " + request.getCustomerId());
    }
    
    /**
     * Process scheduled request - shows eligible workers for admin selection
     * Logic: Filters and scores workers, displays to admin, admin manually selects workers
     * Updates the request object directly instead of returning a string
     * @param request ServiceRequest to process (updated in place)
     * @param workers List of all workers
     * @param scanner Scanner for admin input
     */
    public void processScheduledRequest(ServiceRequest request, ArrayList<Worker> workers, Scanner scanner) {
        System.out.println("Processing scheduled request for Customer " + request.getCustomerId() + "\n");
        
        // Step 1: Filter workers by locality and gender preference
        ArrayList<Worker> eligibleWorkers = FilterService.filterWorkers(
            workers,
            request.getLocality(),
            request.getPreferredGender(),
            request.getRequestedWorks(),
            false  // don't filter by availability for scheduled requests
        );
        
        // Step 2: Score workers by matching work count
        FilterService.scoreWorkers(eligibleWorkers, request.getRequestedWorks());
        
        // Step 3: Sort by matching count (highest first)
        FilterService.sortByMatchingCount(eligibleWorkers);
        
        if (eligibleWorkers.isEmpty()) {
            System.out.println("No eligible workers for the requested services.");
            request.setStatus(Status.REJECTED);
            return;
        }
        
        // Step 4: Display eligible workers to admin
        System.out.println("Eligible workers for Scheduled request:\n");
        for (Worker worker : eligibleWorkers) {
            if (worker != null) {
                String worksStr = worker.getCapableWorks().stream()
                    .map(Work::getWorkName)
                    .collect(Collectors.joining(", ", "[", "]"));
                System.out.println("Worker Id: " + worker.getWorkerId() + 
                                 ", Worker gender: " + worker.getGender() + 
                                 ", Capable works:" + worksStr);
                System.out.println("No. of matching works for " + worker.getWorkerId() + 
                                 " are " + worker.getMatchingCount() + "\n");
            }
        }
        
        // Step 5: Admin manually enters worker IDs
        System.out.println("Enter the IDs of workers to assign (separated by space), or type 'not available' if no workers are suitable:");
        String input = scanner.nextLine();
        
        if (input.equalsIgnoreCase("not available")) {
            System.out.println("Service request rejected: No suitable workers.");
            request.setStatus(Status.REJECTED);
            return;
        }
        
        // Step 6: Parse selected worker IDs
        String[] assignedIds = input.split(" ");
        ArrayList<Worker> assignedWorkers = new ArrayList<>();
        
        for (String workerId : assignedIds) {
            Worker assignedWorker = eligibleWorkers.stream()
                .filter(w -> w.getWorkerId().equals(workerId.trim()))
                .findFirst()
                .orElse(null);
            if (assignedWorker != null) {
                assignedWorkers.add(assignedWorker);
            } else {
                System.out.println("Invalid worker ID: " + workerId);
            }
        }
        
        if (assignedWorkers.isEmpty()) {
            System.out.println("No workers assigned.");
            request.setStatus(Status.REJECTED);
            return;
        }
        
        // Step 7: Assign works to selected workers using greedy algorithm
        LinkedHashMap<Worker, ArrayList<Work>> workerAssignments = FilterService.assignWorkers(
            request.getRequestedWorks(),
            assignedWorkers
        );
        
        // Step 8: Verify all works are assigned
        ArrayList<Work> allAssignedWorks = new ArrayList<>();
        for (ArrayList<Work> works : workerAssignments.values()) {
            allAssignedWorks.addAll(works);
        }
        
        boolean allWorksSatisfied = request.getRequestedWorks().stream()
            .allMatch(allAssignedWorks::contains);
        
            if (!allWorksSatisfied) {
                ArrayList<Work> remainingWorks = new ArrayList<>(request.getRequestedWorks());
                remainingWorks.removeAll(allAssignedWorks);
            
                request.setStatus(Status.REJECTED);
            
                String reason = "No worker available for the following works: " +
                        remainingWorks.stream().map(Work::getWorkName).collect(Collectors.joining(", "));
            
                request.setReason(reason); 
            
                System.out.println("Scheduled service rejected for "+ request.getServiceId() + " : " + reason);
                return;
            }
        
        // Step 9: Calculate end time and set work date/time
        int maxDuration = 0;
        for (Worker worker : workerAssignments.keySet()) {
            int workerDuration = workerAssignments.get(worker).stream()
                .mapToInt(Work::getTimeRequired)
                .sum();
            maxDuration = Math.max(maxDuration, workerDuration);
        }
        request.setWorkEndtime(calculateEndTime(request.getWorkTime(), maxDuration));
        
        // Step 10: Display worker assignments
        System.out.println("Service request accepted: Workers assigned.");
        for (Map.Entry<Worker, ArrayList<Work>> entry : workerAssignments.entrySet()) {
            String worksStr = entry.getValue().stream()
                .map(Work::getWorkName)
                .collect(Collectors.joining(","));
            System.out.println("Worker ID: " + entry.getKey().getWorkerId() +
                             ", Assigned works: " + worksStr);
        }
        System.out.println("");
        
        // Step 11: Update status and worker bookings
        request.setStatus(Status.ASSIGNED);
        for (Worker worker : workerAssignments.keySet()) {
            worker.addBookingId(request.getServiceId());
            workerRepo.update(worker);
        }
        
        // Step 12: Calculate price and set assigned worker IDs
        double price = calculatePrice(request.getRequestedWorks(), request.getPlanName());
        request.setPrice(price);
        
        ArrayList<String> workerIdList = new ArrayList<>();
        for (Worker worker : assignedWorkers) {
            workerIdList.add(worker.getWorkerId());
        }
        request.setAssignedWorkerIds(workerIdList);
    }
    
    /**
     * Find eligible workers for a request using filters
     * @param request ServiceRequest to find workers for
     * @param allWorkers List of all workers
     * @return Filtered list of eligible workers
     */
    public ArrayList<Worker> findEligibleWorkers(ServiceRequest request, ArrayList<Worker> allWorkers) {
        return FilterService.filterWorkers(
            allWorkers,
            request.getLocality(),
            request.getPreferredGender(),
            request.getRequestedWorks(),
            request.getTypeName() == ServiceType.IMMEDIATE  // filter by availability for immediate only
        );
    }
    
    /**
     * Calculate end time by adding duration to start time
     * @param workStartTime Start time in HH:mm:ss format
     * @param totalDurationMinutes Total duration in minutes
     * @return End time in HH:mm:ss format
     */
    public String calculateEndTime(String workStartTime, int totalDurationMinutes) {
        String[] timeParts = workStartTime.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        int second = Integer.parseInt(timeParts[2]);
        
        // Add total duration to start time
        minute += totalDurationMinutes;
        hour += minute / 60;
        minute %= 60;
        hour %= 24;
        
        return String.format("%02d:%02d:%02d", hour, minute, second);
    }
    
    /**
     * Calculate total price with plan-based discount
     * @param requestedWorks List of requested works
     * @param plan Customer's plan
     * @return Total price after discount
     */
    public double calculatePrice(ArrayList<Work> requestedWorks, Plan plan) {
        double totalPrice = 0.0;
        for (Work work : requestedWorks) {
            totalPrice += work.getPrice();
        }
        
        // Apply discount based on plan
        double discount = getDiscount(plan);
        return totalPrice * (1 - discount);
    }
    
    /**
     * Get discount rate for a plan
     * @param plan Customer's plan
     * @return Discount rate (0.0 to 1.0)
     */
    private double getDiscount(Plan plan) {
        switch (plan) {
            case BASIC:
                return 0.0;
            case INTERMEDIATE:
                return 0.1;  // 10% discount
            case PREMIUM:
                return 0.2;  // 20% discount
            default:
                return 0.0;
        }
    }
    
}

/**
 * FilterService - handles worker filtering logic
 * 
 * SOLID Principles Applied:
 * - Single Responsibility: Each filter method has one clear purpose (skill, locality, gender, availability)
 * - Open/Closed: New filters can be added without modifying existing ones
 * - Interface Segregation: Each filter is independent and can be used separately
 * 
 * Implements split filters that can be combined (Chain of Responsibility pattern)
 */
class FilterService {
    
    /**
     * SkillFilter - filters workers by capable works
     * Single Responsibility: Only filters by skills, nothing else
     * @param workers List of workers to filter
     * @param requestedWorks List of requested work types
     * @return Filtered list of workers with matching skills
     */
    public static ArrayList<Worker> filterBySkills(ArrayList<Worker> workers, ArrayList<Work> requestedWorks) {
        ArrayList<Worker> filtered = new ArrayList<>();
        for (Worker worker : workers) {
            // Check if worker has at least one requested skill
            boolean hasSkill = false;
            for (Work requestedWork : requestedWorks) {
                if (worker.getCapableWorks().contains(requestedWork)) {
                    hasSkill = true;
                    break;
                }
            }
            if (hasSkill) {
                filtered.add(worker);
            }
        }
        return filtered;
    }
    
    /**
     * LocalityFilter - filters workers by locality
     * @param workers List of workers to filter
     * @param locality Required locality
     * @return Filtered list of workers in the specified locality
     */
    public static ArrayList<Worker> filterByLocality(ArrayList<Worker> workers, String locality) {
        ArrayList<Worker> filtered = new ArrayList<>();
        for (Worker worker : workers) {
            if (worker.getLocality().equals(locality)) {
                filtered.add(worker);
            }
        }
        return filtered;
    }
    
    /**
     * GenderFilter - filters workers by gender preference
     * @param workers List of workers to filter
     * @param genderPref Gender preference (NP = no preference, M = male, F = female)
     * @return Filtered list of workers matching gender preference
     */
    public static ArrayList<Worker> filterByGender(ArrayList<Worker> workers, GenderPref genderPref) {
        if (genderPref == GenderPref.NP) {
            return new ArrayList<>(workers);  // No filter if no preference
        }
        
        ArrayList<Worker> filtered = new ArrayList<>();
        String requiredGender = genderPref.getValue();
        for (Worker worker : workers) {
            if (worker.getGender().equals(requiredGender)) {
                filtered.add(worker);
            }
        }
        return filtered;
    }
    
    /**
     * AvailabilityFilter - filters workers by availability status
     * @param workers List of workers to filter
     * @param available True to get only available workers, false for all
     * @return Filtered list based on availability
     */
    public static ArrayList<Worker> filterByAvailability(ArrayList<Worker> workers, boolean available) {
        if (!available) {
            return new ArrayList<>(workers);  // Return all if not filtering by availability
        }
        
        ArrayList<Worker> filtered = new ArrayList<>();
        for (Worker worker : workers) {
            if (worker.isAvailableNow()) {
                filtered.add(worker);
            }
        }
        return filtered;
    }
    
    /**
     * Combined filter - applies all filters in sequence
     * 
     * Design Pattern: Chain of Responsibility - each filter in the chain refines the result
     * Open/Closed Principle: New filters can be added to the chain without modifying this method
     * 
     * @param workers List of workers to filter
     * @param locality Required locality
     * @param genderPref Gender preference
     * @param requestedWorks List of requested work types
     * @param available Whether to filter by availability
     * @return Filtered list of workers matching all criteria
     */
    public static ArrayList<Worker> filterWorkers(ArrayList<Worker> workers, String locality, 
                                                  GenderPref genderPref, ArrayList<Work> requestedWorks, 
                                                  boolean available) {
        // Apply filters in sequence - each filter refines the result
        // This follows Chain of Responsibility pattern
        ArrayList<Worker> filtered = filterByLocality(workers, locality);
        filtered = filterByGender(filtered, genderPref);
        filtered = filterBySkills(filtered, requestedWorks);
        filtered = filterByAvailability(filtered, available);
        
        return filtered;
    }
    
    /**
     * Score workers by matching count (for scheduled requests)
     * Counts how many requested works each worker can do
     * @param workers List of workers to score
     * @param requestedWorks List of requested work types
     */
    public static void scoreWorkers(ArrayList<Worker> workers, ArrayList<Work> requestedWorks) {
        for (Worker worker : workers) {
            long matchingCount = worker.getCapableWorks().stream()
                .filter(requestedWorks::contains)
                .count();
            worker.setMatchingCount(matchingCount);
        }
    }
    
    /**
     * Sort workers by matching count (highest first)
     * Used to prioritize workers with more matching skills
     * @param workers List of workers to sort
     */
    public static void sortByMatchingCount(ArrayList<Worker> workers) {
        workers.sort((w1, w2) -> Long.compare(w2.getMatchingCount(), w1.getMatchingCount()));
    }
    
    /**
     * Assign workers using greedy set-cover algorithm
     * 
     * Algorithm: Greedy approach - assigns each work to worker with least current load
     * This balances workload across workers (fair distribution)
     * 
     * Single Responsibility: Only handles assignment logic, no filtering or business rules
     * 
     * @param requestedWorks List of works to assign
     * @param eligibleWorkers List of eligible workers
     * @return Map of worker to assigned works
     */
    public static LinkedHashMap<Worker, ArrayList<Work>> assignWorkers(ArrayList<Work> requestedWorks, 
                                                                        ArrayList<Worker> eligibleWorkers) {
        LinkedHashMap<Worker, ArrayList<Work>> workerAssignments = new LinkedHashMap<>();
        
        for (Work work : requestedWorks) {
            // Greedy selection: Find worker with least assignments who can do this work
            // This ensures balanced workload distribution
            Worker selectedWorker = eligibleWorkers.stream()
                .filter(w -> w.getCapableWorks().contains(work))
                .min(Comparator.comparingInt(w -> 
                    workerAssignments.getOrDefault(w, new ArrayList<>()).size()))
                .orElse(null);
            
            if (selectedWorker != null) {
                workerAssignments.computeIfAbsent(selectedWorker, k -> new ArrayList<>()).add(work);
            }
        }
        
        return workerAssignments;
    }
}
