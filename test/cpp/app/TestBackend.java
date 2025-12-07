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
        // Using signature you provided
        Work w = new Work(1, "Sweeping", 30, 200.0);
        assertEquals(1, w.getId(), "Work id should be preserved");
        assertEquals("Sweeping", w.getWorkName(), "Work name should match");
        assertEquals(30, w.getTimeRequired(), "Work time should match");
        assertEquals(200.0, w.getPrice(), 0.0001, "Work price should match");
    }

    @Test
    void testWorkerBasicConstructor() {
        Worker worker = new Worker("w1", "pass123");
        // Basic checks — does ctor preserve fields (getters must exist)
        assertEquals("w1", worker.getWorkerId());
        assertEquals("pass123", worker.getWorkerPass());
        // Initially no name/gender/area unless set by repository lookup
        // Availability default check (if your ctor sets true by default)
        // We test the setter/getter roundtrip if available
        worker.setAvailable(false);
        assertFalse(worker.isAvailableNow(), "Worker availability toggled");
    }

    @Test
    void testAssignmentServiceConstruction() {
        // Construct repositories with the typical file-path strings (they won't be used here)
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

        // Assuming Plan.BASIC exists in your code
        double price = as.calculatePrice(works, Plan.BASIC);
        // For BASIC we expect no discount: 600 + 300 = 900
        assertEquals(900.0, price, 0.0001);
    }

    @Test
    void testWorkerAddBookingAndGetBookings() {
        Worker w = new Worker("w2", "pw");
        // addBookingId likely exists per your model
        w.addBookingId(100);
        assertTrue(w.getBookings().contains(100), "Booking id should be stored");
    }


    /* --------------------------------------------
       Helper: Create Work Map (like works_config)
    -------------------------------------------- */
    private LinkedHashMap<String, Work> createWorkMap() {
        LinkedHashMap<String, Work> map = new LinkedHashMap<>();
        map.put("Sweeping", new Work(1, "Sweeping", 30, 200));
        map.put("Mopping", new Work(2, "Mopping", 40, 300));
        map.put("Window Cleaning", new Work(3, "Window Cleaning", 80, 600));
        return map;
    }

    /* --------------------------------------------
       Helper: Create Workers
    -------------------------------------------- */
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

    /* --------------------------------------------
       TEST 1: IMMEDIATE REQUEST IS ASSIGNED
    -------------------------------------------- */
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

    /* --------------------------------------------
       TEST 2: IMMEDIATE REQUEST REJECTED IF NO WORKER
    -------------------------------------------- */
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

    /* --------------------------------------------
       TEST 3: SCHEDULED REQUEST MANUAL ASSIGN
    -------------------------------------------- */
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

    /* --------------------------------------------
       TEST 4: MARK SERVICE AS COMPLETED
    -------------------------------------------- */
    @Test
    void testMarkServiceCompleted() {
        LinkedHashMap<String, Work> works = createWorkMap();

        String[] fields = {
                "4", "1", "Immediate", "Basic",
                "2025-01-01", "10:00:00",
                "Moghalrajpuram",
                "C4", "M",
                "Addr",
                "[Sweeping]",
                "NP", "", "", "2025-01-01", "10:00:00", "11:00:00"
        };

        ServiceRequest req = new ServiceRequest(fields, works);
        req.setStatus(Status.COMPLETED);

        assertEquals(Status.COMPLETED, req.getStatus());
    }

    /* --------------------------------------------
       TEST 5: PRICE CALCULATION
    -------------------------------------------- */
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

    /* --------------------------------------------
       TEST 6: WORKER BOOKING STORAGE
    -------------------------------------------- */
    @Test
    void testWorkerBookingStorage() {
        Worker w = new Worker("W9", "pw");
        w.addBookingId(101);

        assertTrue(w.getBookings().contains(101));
    }
}
