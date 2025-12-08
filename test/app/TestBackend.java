package app;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Scanner;

public class TestBackend {

    @Test
    void testWorkConstructorAndAccessors() {
        Work w = new Work(1, "Sweeping", 30, 200.0);
        assertEquals(1, w.getId(), "Work id should be preserved");
        assertEquals("Sweeping", w.getWorkName(), "Work name should match");
        assertEquals(30, w.getTimeRequired(), "Work time should match");
        assertEquals(200.0, w.getPrice(), 0.0001, "Work price should match");
    }

    @Test
    void testWorkerBasicConstructor() {
        Worker worker = new Worker("w1", "pass123");
        // Basic checks
        assertEquals("w1", worker.getWorkerId());
        assertEquals("pass123", worker.getWorkerPass());
        // Initially no name/gender/area unless set by repository lookup
        // We test the setter/getter roundtrip if available
        worker.setAvailable(false);
        assertFalse(worker.isAvailableNow(), "Worker availability toggled");
    }

    @Test
    void testAssignmentServiceConstruction() {
        
        WorkerRepository wr = new WorkerRepository("data/workers.json");
        ServiceRepository sr = new ServiceRepository("data/services.json");

        AssignmentService as = new AssignmentService(wr, sr);
        assertNotNull(as, "AssignmentService instance should be created");
    }

    @Test
    void testCalculatePriceUsingAssignmentService() {
        // Create an AssignmentService (we only need the calculatePrice method)
        WorkerRepository wr = new WorkerRepository("data/workers.json");
        ServiceRepository sr = new ServiceRepository("data/services.json");
        AssignmentService as = new AssignmentService(wr, sr);

        ArrayList<Work> works = new ArrayList<>();
        works.add(new Work(1, "Window Cleaning", 80, 600.0));
        works.add(new Work(2, "Mopping", 40, 300.0));

        double price = as.calculatePrice(works, Plan.BASIC);
        // For BASIC we expect no discount: 600 + 300 = 900
        assertEquals(900.0, price, 0.0001);
    }

    @Test
    void testWorkerAddBookingAndGetBookings() {
        Worker w = new Worker("w2", "pw");
        w.addBookingId(100);
        assertTrue(w.getBookings().contains(100), "Booking id should be stored");
    }


    /* 
       Helper: Create Work Map (like works_config)
  */
     private LinkedHashMap<String, Work> createWorkMap() {
        LinkedHashMap<String, Work> map = new LinkedHashMap<>();
        map.put("Sweeping", new Work(1, "Sweeping", 30, 200));
        map.put("Mopping", new Work(2, "Mopping", 40, 300));
        map.put("Window Cleaning", new Work(3, "Window Cleaning", 80, 600));
        return map;
    }
    /* 
       Helper: Create Workers
     */
    private ArrayList<Worker> createWorkers(LinkedHashMap<String, Work> works) {
        Worker w1 = new Worker("W1", "p1");
        w1.setName("Arjun");
        w1.setGender("M");
        w1.setArea("Moghalrajpuram");
        w1.setAvailable(true);
        w1.setCapableWorks(new ArrayList<>(List.of(
                works.get("Sweeping"),
                works.get("Mopping")
        )));

        Worker w2 = new Worker("W2", "p2");
        w2.setName("Sita");
        w2.setGender("F");
        w2.setArea("Moghalrajpuram");
        w2.setAvailable(true);
        w2.setCapableWorks(new ArrayList<>(List.of(
                works.get("Window Cleaning")
        )));

        return new ArrayList<>(List.of(w1, w2));
    }

    /* 
       IMMEDIATE REQUEST IS ASSIGNED
     */
    @Test
    void testImmediateServiceAssignment() {
        LinkedHashMap<String, Work> works = createWorkMap();
        ArrayList<Worker> workers = createWorkers(works);

        WorkerRepository wr = new WorkerRepository("data/test_workers.json");
        ServiceRepository sr = new ServiceRepository("data/test_services.json");
        AssignmentService as = new AssignmentService(wr, sr);

        String[] fields = {
                "1", "0", "Immediate", "Basic",
                "2025-01-01", "10:00:00",
                "Moghalrajpuram",
                "C1", "M",
                "Some Addr",
                "[Sweeping,Mopping]",
                "NP", "", "", "", "", ""
        };

        ServiceRequest req = new ServiceRequest(fields, works);

        as.processImmediateRequest(req, workers);

        assertEquals(Status.ASSIGNED, req.getStatus());
        assertFalse(req.getAssignedWorkerIds().isEmpty());
    }

    /* 
       IMMEDIATE REQUEST REJECTED IF NO WORKER
     */
    @Test
    void testImmediateServiceRejectedWhenNoWorker() {
        LinkedHashMap<String, Work> works = createWorkMap();

        // No workers at all
        ArrayList<Worker> workers = new ArrayList<>();

        WorkerRepository wr = new WorkerRepository("data/test_workers.json");
        ServiceRepository sr = new ServiceRepository("data/test_services.json");
        AssignmentService as = new AssignmentService(wr, sr);

        String[] fields = {
                "2", "0", "Immediate", "Basic",
                "2025-01-01", "11:00:00",
                "Moghalrajpuram",
                "C2", "M",
                "Addr",
                "[Sweeping]",
                "NP", "", "", "", "", ""
        };

        ServiceRequest req = new ServiceRequest(fields, works);

        as.processImmediateRequest(req, workers);

        assertEquals(Status.REJECTED, req.getStatus());
    }

    /* 
       SCHEDULED REQUEST MANUAL ASSIGN
     */
    @Test
    void testScheduledServiceAssignment() {
        LinkedHashMap<String, Work> works = createWorkMap();
        ArrayList<Worker> workers = createWorkers(works);

        WorkerRepository wr = new WorkerRepository("data/test_workers.json");
        ServiceRepository sr = new ServiceRepository("data/test_services.json");
        AssignmentService as = new AssignmentService(wr, sr);

        String[] fields = {
                "3", "0", "Scheduling", "Premium",
                "2025-01-01", "12:00:00",
                "Moghalrajpuram",
                "C3", "F",
                "Addr",
                "[Window Cleaning]",
                "NP",
                "2025-01-10", "09:00:00", "", "", ""
        };

        ServiceRequest req = new ServiceRequest(fields, works);

        // Fake admin input selecting worker W2
        Scanner fakeScanner = new Scanner("W2\n");

        as.processScheduledRequest(req, workers, fakeScanner);

        assertEquals(Status.ASSIGNED, req.getStatus());
        assertEquals(1, req.getAssignedWorkerIds().size());
    }

    
    /* 
       PRICE CALCULATION
     */
    @Test
    void testPriceCalculation() {
        WorkerRepository wr = new WorkerRepository("data/test_workers.json");
        ServiceRepository sr = new ServiceRepository("data/test_services.json");
        AssignmentService as = new AssignmentService(wr, sr);

        ArrayList<Work> works = new ArrayList<>();
        works.add(new Work(1, "Sweeping", 30, 200));
        works.add(new Work(2, "Mopping", 40, 300));

        double price = as.calculatePrice(works, Plan.INTERMEDIATE);

        assertEquals(450.0, price, 0.01); // 10% discount
    }

    /* 
       WORKER BOOKING STORAGE
     */
    @Test
    void testWorkerBookingStorage() {
        Worker w = new Worker("W9", "pw");
        w.addBookingId(101);

        assertTrue(w.getBookings().contains(101));
    }

    @Test
    void testWorkerEditDetailsUpdatesCapableWorks() {
        LinkedHashMap<String, Work> workMap = new LinkedHashMap<>();
        Work sweeping = new Work(1, "Sweeping", 30, 200);
        Work mopping  = new Work(2, "Mopping", 40, 300);
        Work washing  = new Work(3, "Washing", 25, 150);

        workMap.put("Sweeping", sweeping);
        workMap.put("Mopping", mopping);
        workMap.put("Washing", washing);

        Worker worker = new Worker("W1", "pass");
        worker.setCapableWorks(new ArrayList<>(List.of(sweeping)));

        // Simulate edit → replace old skill with new ones
        worker.setCapableWorks(new ArrayList<>(List.of(mopping, washing)));

        assertEquals(2, worker.getCapableWorks().size());
        assertTrue(worker.getCapableWorks().contains(mopping));
        assertTrue(worker.getCapableWorks().contains(washing));
    }


    @Test
    void testMarkServiceAsCompleted() {
        LinkedHashMap<String, Work> workMap = new LinkedHashMap<>();
        Work sweeping = new Work(1, "Sweeping", 30, 200);
        workMap.put("Sweeping", sweeping);

        String[] fields = {
            "101", "1", "Immediate", "Basic",
            "2025-12-10", "10:00:00",
            "City", "C1", "M", "Addr",
            "[Sweeping]", "NP",
            "", "", "2025-12-10", "10:00:00", "11:00:00"
        };

        ServiceRequest req = new ServiceRequest(fields, workMap);
        req.setStatus(Status.ASSIGNED);

        req.setStatus(Status.COMPLETED);
        req.setWorkEndtime("12:30:00");

        assertEquals(Status.COMPLETED, req.getStatus());
        assertEquals("12:30:00", req.getWorkEndTime());
    }

    @Test
    void testWorkerViewBookingsOnlyAssigned() {
        LinkedHashMap<String, Work> works = createWorkMap();
        Work sweep = new Work(1, "Sweeping", 30, 200);
        works.put("Sweeping", sweep);

        // ASSIGNED service
        String[] f1 = {
            "1", "1", "Immediate", "Basic",
            "2025-12-10", "10:00:00",
            "City", "C1", "M", "Addr",
            "[Sweeping]", "NP",
            "", "", "2025-12-10", "10:00:00", "11:00:00"
        };

        // COMPLETED service
        String[] f2 = {
            "2", "2", "Immediate", "Basic",
            "2025-12-10", "10:00:00",
            "City", "C1", "M", "Addr",
            "[Sweeping]", "NP",
            "", "", "2025-12-10", "10:00:00", "11:30:00"
        };

        ServiceRequest s1 = new ServiceRequest(f1, works);
        ServiceRequest s2 = new ServiceRequest(f2, works);

        ArrayList<ServiceRequest> services = new ArrayList<>();
        services.add(s1);
        services.add(s2);

        long active = services.stream()
            .filter(s -> s.getStatus() == Status.ASSIGNED)
            .count();

        assertEquals(1, active);
    }

    @Test
    void testWorkerHistoryOnlyCompleted() {

        LinkedHashMap<String, Work> workMap = new LinkedHashMap<>();
        Work sweep = new Work(1, "Sweeping", 30, 200);
        workMap.put("Sweeping", sweep);

        String[] f1 = {
            "1", "2", "Immediate", "Basic",
            "2025-12-10", "10:00:00",
            "City", "C1", "M", "Addr",
            "[Sweeping]", "NP",
            "", "", "2025-12-10", "10:00:00", "11:30:00"
        };

        String[] f2 = {
            "2", "1", "Immediate", "Basic",
            "2025-12-10", "10:00:00",
            "City", "C1", "M", "Addr",
            "[Sweeping]", "NP",
            "", "", "2025-12-10", "10:00:00", "11:00:00"
        };

        ServiceRequest s1 = new ServiceRequest(f1, workMap); // COMPLETED
        ServiceRequest s2 = new ServiceRequest(f2, workMap); // ASSIGNED

        ArrayList<ServiceRequest> services = new ArrayList<>();
        services.add(s1);
        services.add(s2);

        long completed = services.stream()
            .filter(s -> s.getStatus() == Status.COMPLETED)
            .count();

        assertEquals(1, completed);
    }


    @Test
    void testFullAssignmentThenCompletionFlow() {

        // Setup Work 
        Work sweep = new Work(1, "Sweeping", 30, 200);
        LinkedHashMap<String, Work> workMap = new LinkedHashMap<>();
        workMap.put("Sweeping", sweep);

        // Worker 
        Worker worker = new Worker("W1", "pass");
        worker.setName("Test Worker");
        worker.setGender("M");
        worker.setArea("City");
        worker.setAvailable(true);
        worker.setCapableWorks(new ArrayList<>(List.of(sweep)));

        //  Request (PENDING + IMMEDIATE) 
        String[] fields = {
            "201", "0", "Immediate", "Basic",   
            "2025-12-10", "10:00:00",
            "City", "C1", "M", "Addr",
            "[Sweeping]", "NP",
            "", "", "", "", ""
        };

        ServiceRequest request = new ServiceRequest(fields, workMap);

        //Repositories
        WorkerRepository wr = new WorkerRepository("data/test_workers.json");
        ServiceRepository sr = new ServiceRepository("data/test_services.json");

        AssignmentService as = new AssignmentService(wr, sr);

        // ASSIGN 
        as.processImmediateRequest(request, new ArrayList<>(List.of(worker)));

        // MUST BE ASSIGNED
        assertEquals(Status.ASSIGNED, request.getStatus());

        //  COMPLETE 
        request.setStatus(Status.COMPLETED);
        request.setWorkEndtime("12:00:00");

        //MUST BE COMPLETED
        assertEquals(Status.COMPLETED, request.getStatus());
        assertEquals("12:00:00", request.getWorkEndTime());
    }



    @Test
    void testRejectedRequestRemainsRejected() {

        String[] fields = new String[17];
        fields[0] = "1";                     // serviceId
        fields[1] = "-1";                    // REJECTED
        fields[2] = "Immediate";
        fields[3] = "Basic";
        fields[4] = "2025-12-03";
        fields[5] = "18:00:00";
        fields[6] = "Moghalrajpuram";
        fields[7] = "customer1";
        fields[8] = "M";
        fields[9] = "addr1";
        fields[10] = "[Sweeping]";
        fields[11] = "NP";
        fields[12] = ""; fields[13] = ""; fields[14] = ""; fields[15] = ""; fields[16] = "";

        LinkedHashMap<String, Work> workMap = new LinkedHashMap<>();
        workMap.put("Sweeping", new Work(1, "Sweeping", 30, 200));

        ServiceRequest req = new ServiceRequest(fields, workMap);

        assertEquals(Status.REJECTED, req.getStatus());
    }


}
