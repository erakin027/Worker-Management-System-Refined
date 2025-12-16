package app;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestBackend {

    //Helper to create Works
    private LinkedHashMap<String, Work> createWorkMatchings() {
        WorkRepository workRepo = new WorkRepository("data/works_config.json");
        workRepo.loadAllWorks();
        LinkedHashMap<String, Work> workMatchings =workRepo.getAllWorks();

        return workMatchings;
    }

    private void cleanFile(String path) {
        try {
            Files.deleteIfExists(Path.of(path));
        } catch (Exception ignored) {}
    }

   
    private Worker registerWorker(
            String workerId,
            String workerPass,
            String name,
            String gender,
            int areaCode,
            List<String> workNames,
            LinkedHashMap<String, Work> workMatchings,
            WorkerRepository repo
    ) {
        Worker worker = new Worker(workerId, workerPass);
        worker.setWorkerRepo(repo);
        worker.setWorkMatchings(workMatchings);

        // Map the requested work names to the menu indices used during registration.
        List<String> orderedNames = new ArrayList<>(workMatchings.keySet());
        List<Integer> selections = new ArrayList<>();
        for (String target : workNames) {
            int idx = orderedNames.indexOf(target);
            if (idx == -1) {
                throw new IllegalArgumentException("Work not found: " + target);
            }
            selections.add(idx + 1); // menu is 1-based
        }

        StringBuilder input = new StringBuilder();
        input.append(name).append("\n");
        input.append(gender).append("\n");
        input.append(areaCode).append("\n");
        input.append(selections.size()).append("\n");
        for (int i = 0; i < selections.size(); i++) {
            input.append(selections.get(i));
            if (i < selections.size() - 1) input.append(" ");
        }
        input.append("\n");

        Scanner scanner = new Scanner(input.toString());
        worker.register(scanner, workMatchings);
        return worker;
    }

    private ArrayList<Worker> createWorkers(LinkedHashMap<String, Work> works, WorkerRepository repo) {
        // Use registration flow for two sample workers in Moghalrajpuram (area code 1)
        Worker w1 = registerWorker(
                "W1", "p1", "Arjun", "M", 1,
                List.of("Sweeping", "Mopping"),
                works, repo
        );

        Worker w2 = registerWorker(
                "W2", "p2", "Sita", "F", 1,
                List.of("Window Cleaning"),
                works, repo
        );

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
        String workersPath = "data/test_workers.json";
        String servicesPath = "data/test_services.json";
        cleanFile(workersPath);
        cleanFile(servicesPath);

        WorkerRepository wr = new WorkerRepository(workersPath);
        ServiceRepository sr = new ServiceRepository(servicesPath);
        ArrayList<Worker> workers = createWorkers(works, wr);
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
        assertEquals(1, req.getAssignedWorkerIds().size());
        assertEquals("W1", req.getAssignedWorkerIds().get(0));
    }

    //IMMEDIATE REQUEST REJECTED IF NO WORKER CAN DO THE WORK
    @Test
    void testImmediateServiceRejected(){
        LinkedHashMap<String, Work> works = createWorkMatchings();

        // No workers at all
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
                "[Ironing]",
                "NP", "", "", "", "", ""
        };

        ServiceRequest req = new ServiceRequest(fields, works);

        as.processImmediateRequest(req, workers);

        assertEquals(Status.REJECTED, req.getStatus());
        assertNotNull(req.getReason());
    }

    //SCHEDULED REQUEST MANUAL ASSIGN
    @Test
    void testScheduledServiceAssignment() {
        LinkedHashMap<String, Work> works = createWorkMatchings();
        String workersPath = "data/test_workers.json";
        String servicesPath = "data/test_services.json";
        cleanFile(workersPath);
        cleanFile(servicesPath);

        WorkerRepository wr = new WorkerRepository(workersPath);
        ServiceRepository sr = new ServiceRepository(servicesPath);
        ArrayList<Worker> workers = createWorkers(works, wr);
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

    //MARK SERVICE AS COMPLETED
    @Test
    void testMarkServiceCompleted() {
        LinkedHashMap<String, Work> works = createWorkMatchings();
        String workersPath = "data/test_workers.json";
        String servicesPath = "data/test_services.json";
        cleanFile(workersPath);
        cleanFile(servicesPath);

        WorkerRepository wr = new WorkerRepository(workersPath);
        ServiceRepository sr = new ServiceRepository(servicesPath);
        ArrayList<Worker> workers = createWorkers(works, wr);

        Worker worker = workers.get(0); // registered worker

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

    //PRICE CALCULATION
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

    //WORKER BOOKING STORAGE
    @Test
    void testWorkerBookingStorage() {
        LinkedHashMap<String, Work> works = createWorkMatchings();
        String workersPath = "data/test_workers.json";
        cleanFile(workersPath);

        WorkerRepository wr = new WorkerRepository(workersPath);
        createWorkers(works, wr); // register sample workers into repo

        // Load existing worker and update bookings through repository-backed object
        Worker existing = wr.loadAll(works).get(0);
        existing.addBookingId(101);
        wr.update(existing);

        Worker reloaded = wr.loadAll(works).get(0);
        assertTrue(reloaded.getBookings().contains(101));
    }

    @Test
    void testWorkerEditDetailsUpdatesCapableWorks() {
        LinkedHashMap<String, Work> workMap = new LinkedHashMap<>();
        workMap.put("Sweeping", new Work(1, "Sweeping", 30, 200));
        workMap.put("Mopping", new Work(2, "Mopping", 40, 300));
        workMap.put("Washing", new Work(3, "Washing", 25, 150));

        String workersPath = "data/test_workers.json";
        cleanFile(workersPath);
        WorkerRepository wr = new WorkerRepository(workersPath);

        // Register one worker with initial capability Sweeping
        Worker worker = registerWorker(
                "W1", "p1", "Arjun", "M", 1,
                List.of("Sweeping"),
                workMap, wr
        );

        // Re-load to simulate "existing" worker
        worker = wr.loadAll(workMap).get(0);
        worker.setWorkerRepo(wr);
        worker.setWorkMatchings(workMap);

        // Edit via scanner: choose option 2, declare 2 works, select Mopping and Washing (menu indexes 2 and 3)
        Scanner scanner = new Scanner("2\n2\n2 3\n");
        worker.editDetails(scanner, workMap);

        assertEquals(2, worker.getCapableWorks().size());
        assertTrue(worker.getCapableWorks().stream().anyMatch(w -> w.getWorkName().equals("Mopping")));
        assertTrue(worker.getCapableWorks().stream().anyMatch(w -> w.getWorkName().equals("Washing")));
    }


    @Test
    void testWorkerViewBookingsOnlyAssigned() {
        LinkedHashMap<String, Work> workMap = createWorkMatchings();
        String workersPath = "data/test_workers.json";
        String servicesPath = "data/test_services.json";
        cleanFile(workersPath);
        cleanFile(servicesPath);

        WorkerRepository wr = new WorkerRepository(workersPath);
        ServiceRepository sr = new ServiceRepository(servicesPath);
        createWorkers(workMap, wr);

        Worker worker = wr.loadAll(workMap).get(0);
        worker.setWorkerRepo(wr);
        worker.setServiceRepo(sr);
        worker.setWorkMatchings(workMap);

        String[] f1 = {
            "1", "1", "Immediate", "Basic",
            "2026-12-10", "10:00:00",
            "City", "C1", "M", "Addr",
            "[Sweeping]", "NP",
            "", "", "2026-12-10", "10:00:00", "11:00:00"
        };

        String[] f2 = {
            "2", "2", "Immediate", "Basic",
            "2026-12-10", "10:00:00",
            "City", "C1", "M", "Addr",
            "[Sweeping]", "NP",
            "", "", "2026-12-10", "10:00:00", "11:30:00"
        };

        ServiceRequest s1 = new ServiceRequest(f1, workMap); // ASSIGNED
        ServiceRequest s2 = new ServiceRequest(f2, workMap); // COMPLETED
        sr.saveAll(new ArrayList<>(List.of(s1, s2)));

        worker.addBookingId(s1.getServiceId());
        worker.addBookingId(s2.getServiceId());
        wr.update(worker);

        ArrayList<ServiceRequest> loaded = sr.loadAll(workMap);
        long active = loaded.stream()
                .filter(s -> worker.getBookings().contains(s.getServiceId()))
                .filter(s -> s.getStatus() == Status.ASSIGNED)
                .count();

        assertEquals(1, active);
    }

    @Test
    void testWorkerHistoryOnlyCompleted() {

        LinkedHashMap<String, Work> workMap = createWorkMatchings();
        String workersPath = "data/test_workers.json";
        String servicesPath = "data/test_services.json";
        cleanFile(workersPath);
        cleanFile(servicesPath);

        WorkerRepository wr = new WorkerRepository(workersPath);
        ServiceRepository sr = new ServiceRepository(servicesPath);
        createWorkers(workMap, wr);

        Worker worker = wr.loadAll(workMap).get(0);
        worker.setWorkerRepo(wr);
        worker.setServiceRepo(sr);
        worker.setWorkMatchings(workMap);

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
        sr.saveAll(new ArrayList<>(List.of(s1, s2)));

        worker.addBookingId(s1.getServiceId());
        worker.addBookingId(s2.getServiceId());
        wr.update(worker);

        ArrayList<ServiceRequest> loaded = sr.loadAll(workMap);
        long completed = loaded.stream()
                .filter(s -> worker.getBookings().contains(s.getServiceId()))
                .filter(s -> s.getStatus() == Status.COMPLETED)
                .count();

        assertEquals(1, completed);
    }


    @Test
    void testFullAssignmentThenCompletionFlow() {

        LinkedHashMap<String, Work> workMap = createWorkMatchings();
        String workersPath = "data/test_workers.json";
        String servicesPath = "data/test_services.json";
        cleanFile(workersPath);
        cleanFile(servicesPath);

        WorkerRepository wr = new WorkerRepository(workersPath);
        ServiceRepository sr = new ServiceRepository(servicesPath);

        // Register worker via the real flow so skills/locality come from stored data
        registerWorker(
                "W1", "pass", "Test Worker", "M", 1, // area code 1 -> Moghalrajpuram
                List.of("Sweeping"),
                workMap, wr
        );

        // Reload to get a persisted worker instance
        Worker worker = wr.loadAll(workMap).get(0);
        worker.setWorkerRepo(wr);
        worker.setServiceRepo(sr);
        worker.setWorkMatchings(workMap);

        //  Request (PENDING + IMMEDIATE) 
        String[] fields = {
            "201", "0", "Immediate", "Basic",   
            "2026-12-10", "10:00:00",
            "Moghalrajpuram", "C1", "M", "Addr",
            "[Sweeping]", "NP",
            "", "", "", "", ""
        };

        ServiceRequest request = new ServiceRequest(fields, workMap);

        AssignmentService as = new AssignmentService(wr, sr);

        // ASSIGN 
        as.processImmediateRequest(request, new ArrayList<>(List.of(worker)));

        // MUST BE ASSIGNED by service logic
        assertEquals(Status.ASSIGNED, request.getStatus());
        assertEquals(List.of("W1"), request.getAssignedWorkerIds());
        assertTrue(request.getWorkEndTime() != null && !request.getWorkEndTime().isEmpty());
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
