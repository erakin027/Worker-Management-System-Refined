package app;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.*;
import java.util.*;

public class TestBackend {

    //Helper to create Works
    private LinkedHashMap<String, Work> createWorkMatchings() {
        WorkRepository workRepo = new WorkRepository("data/works_config.json");
        workRepo.loadAllWorks();
        LinkedHashMap<String, Work> workMatchings =workRepo.getAllWorks();

        return workMatchings;
    }

    //Helper to create workers
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
        w1.setWorkMatchings(works);

        Worker w2 = new Worker("W2", "p2");
        w2.setName("Sita");
        w2.setGender("F");
        w2.setArea("Moghalrajpuram");
        w2.setAvailable(true);
        w2.setCapableWorks(new ArrayList<>(List.of(
                works.get("Window Cleaning")
        )));
        w2.setWorkMatchings(works);

        return new ArrayList<>(List.of(w1, w2));
    }

    //Test work creation
    @Test
    void testWorkCreation() {
        Work w = new Work(1, "Sweeping", 30, 200);
        assertEquals(1, w.getId());
        assertEquals("Sweeping", w.getWorkName());
        assertEquals(30, w.getTimeRequired());
        assertEquals(200, w.getPrice());
    }
    
    @Test
    void testWorkerCreationAndBooking() {
        Worker w = new Worker("W1", "pw");
        w.addBookingId(101);
        assertTrue(w.getBookings().contains(101));
    }

    @Test
    void testImmediateServiceRejectedWhenNoWorker() {
        LinkedHashMap<String, Work> works = createWorkMatchings();
        ArrayList<Worker> workers = new ArrayList<>();

        WorkerRepository wr = new WorkerRepository("data/test_workers.json");
        ServiceRepository sr = new ServiceRepository("data/test_services.json");
        AssignmentService as = new AssignmentService(wr, sr);

        String[] fields = {
                "2", "0", "Immediate", "Basic",
                "2026-01-01", "11:00:00",
                "Moghalrajpuram",
                "C2", "M",
                "Addr",
                "[Sweeping]",
                "NP", "", "", "", "", ""
        };

        ServiceRequest req = new ServiceRequest(fields, works);
        as.processImmediateRequest(req, workers);

        assertEquals(Status.REJECTED, req.getStatus());
        assertNotNull(req.getReason());
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

    /* 
       IMMEDIATE REQUEST IS ASSIGNED
     */
    @Test
    void testImmediateServiceAssignment() {
        LinkedHashMap<String, Work> works = createWorkMatchings();
        ArrayList<Worker> workers = createWorkers(works);

        WorkerRepository wr = new WorkerRepository("data/test_workers.json");
        ServiceRepository sr = new ServiceRepository("data/test_services.json");
        AssignmentService as = new AssignmentService(wr, sr);

        String[] fields = {
                "1", "0", "Immediate", "Basic",
                "2026-01-01", "10:00:00",
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
       IMMEDIATE REQUEST REJECTED IF NO WORKER CAN DO THE WORK
     */
    @Test
    void testImmediateServiceRejected(){
        LinkedHashMap<String, Work> works = createWorkMatchings();

        // No workers at all
        ArrayList<Worker> workers =createWorkers(works);

        WorkerRepository wr = new WorkerRepository("data/test_workers.json");
        ServiceRepository sr = new ServiceRepository("data/test_services.json");
        AssignmentService as = new AssignmentService(wr, sr);

        String[] fields = {
                "2", "0", "Immediate", "Basic",
                "2026-01-01", "11:00:00",
                "Moghalrajpuram",
                "C2", "M",
                "Addr",
                "[Ironing]",
                "NP", "", "", "", "", ""
        };

        ServiceRequest req = new ServiceRequest(fields, works);

        as.processImmediateRequest(req, workers);

        assertEquals(Status.REJECTED, req.getStatus());
        assertNotNull(req.getReason());
    }

    /* 
       SCHEDULED REQUEST MANUAL ASSIGN
     */
    @Test
    void testScheduledServiceAssignment() {
        LinkedHashMap<String, Work> works = createWorkMatchings();
        ArrayList<Worker> workers = createWorkers(works);

        WorkerRepository wr = new WorkerRepository("data/test_workers.json");
        ServiceRepository sr = new ServiceRepository("data/test_services.json");
        AssignmentService as = new AssignmentService(wr, sr);

        String[] fields = {
                "3", "0", "Scheduling", "Premium",
                "2026-01-01", "12:00:00",
                "Moghalrajpuram",
                "C3", "F",
                "Addr",
                "[Window Cleaning]",
                "NP",
                "2026-01-10", "09:00:00", "", "", ""
        };

        ServiceRequest req = new ServiceRequest(fields, works);

        // Fake admin input selecting worker W2
        Scanner fakeScanner = new Scanner("W2\n");
        as.processScheduledRequest(req, workers, fakeScanner);

        assertEquals(Status.ASSIGNED, req.getStatus());
        assertEquals(1, req.getAssignedWorkerIds().size());
        assertEquals("W2", req.getAssignedWorkerIds().get(0));
        assertEquals(480.0, req.getPrice(), 0.01);
    }

    /* 
       MARK SERVICE AS COMPLETED
     */
    @Test
    void testMarkServiceCompleted() {
        LinkedHashMap<String, Work> works = createWorkMatchings();
        ArrayList<Worker> workers = createWorkers(works);

        Worker worker = workers.get(0);

        String[] fields = {
                "4", "1", "Immediate", "Basic",
                "2026-01-01", "10:00:00",
                "Moghalrajpuram",
                "C4", "M",
                "Addr",
                "[Sweeping]",
                "NP", "2026-01-01", "10:00:00", "10:30:00"
        };

        ServiceRequest service = new ServiceRequest(fields, works);
        service.setPrice(60);
        service.setAssignedWorkerIds(new ArrayList<>(List.of(worker.getWorkerId())));
        worker.addBookingId(4);

        WorkerRepository wr = new WorkerRepository("data/test_workers.json");
        ServiceRepository sr = new ServiceRepository("data/test_services.json");
        worker.setServiceRepo(sr);
        worker.setWorkerRepo(wr);
        //worker.setWorkMatchings(works);

        sr.saveAll(new ArrayList<>(List.of(service)));

        //ByteArrayInputStream input =new ByteArrayInputStream("4\n".getBytes());
        worker.setScanner(new Scanner("4\n"));
        //System.setIn(input);

        worker.markWorkAsCompleted();
        worker.setAvailable(true);

        ServiceRequest updated = sr.loadAll(works).get(0);


        assertEquals(Status.COMPLETED, updated.getStatus());
        assertNotNull(updated.getWorkEndTime());
        assertTrue(worker.isAvailableNow());
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
    void testWorkerViewBookingsOnlyAssigned() {
        LinkedHashMap<String, Work> workMap = new LinkedHashMap<>();
        Work sweep = new Work(1, "Sweeping", 30, 200);
        workMap.put("Sweeping", sweep);

        // ASSIGNED service
        String[] f1 = {
            "1", "1", "Immediate", "Basic",
            "2026-12-10", "10:00:00",
            "City", "C1", "M", "Addr",
            "[Sweeping]", "NP",
            "", "", "2026-12-10", "10:00:00", "11:00:00"
        };

        // COMPLETED service
        String[] f2 = {
            "2", "2", "Immediate", "Basic",
            "2026-12-10", "10:00:00",
            "City", "C1", "M", "Addr",
            "[Sweeping]", "NP",
            "", "", "2026-12-10", "10:00:00", "11:30:00"
        };

        ServiceRequest s1 = new ServiceRequest(f1, workMap);
        ServiceRequest s2 = new ServiceRequest(f2, workMap);

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
            "2026-12-10", "10:00:00",
            "City", "C1", "M", "Addr",
            "[Sweeping]", "NP",
            "", "", "2026-12-10", "10:00:00", "11:30:00"
        };

        String[] f2 = {
            "2", "1", "Immediate", "Basic",
            "2026-12-10", "10:00:00",
            "City", "C1", "M", "Addr",
            "[Sweeping]", "NP",
            "", "", "2026-12-10", "10:00:00", "11:00:00"
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
            "2026-12-10", "10:00:00",
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
        fields[4] = "2026-12-03";
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
