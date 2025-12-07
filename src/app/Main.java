// Refactored to JSON & SOLID — do not change workflow without review
// Main entry point for Java Admin/Worker application
// Preserves original menu structure and workflows
package app;

import java.util.Scanner;
import java.util.LinkedHashMap;

public class Main {
    public static void print(String message) {
        System.out.println(message);
    }

    public static void main(String[] args) {
        print("Welcome to our Worker Booking System!!");
        //Admin admin = null;
        Scanner scanner = new Scanner(System.in);

        // Initialize work mappings
        WorkRepository workRepo = new WorkRepository("data/works_config.json");
        workRepo.loadAllWorks();
        LinkedHashMap<String, Work> workMatchings =workRepo.getAllWorks();

        // Initialize repositories
        WorkerRepository workerRepo = new WorkerRepository("data/workers.json");
        ServiceRepository serviceRepo = new ServiceRepository("data/services.json");
        AdminRepository adminRepo = new AdminRepository("data/admin.json");
        
        // Initialize services
        AssignmentService assignmentService = new AssignmentService(workerRepo, serviceRepo);

        while (true) {
            print("Choose your role by entering: (1/2/3)");
            print("1.Admin\n2.Worker\n3.Exit");
            String choice = scanner.nextLine();

            if(choice.equals("3")) break;

            User user = null;
            if(choice.equals("1")){
                user = new Admin();
            }
            else if (choice.equals("2")){
                user = new Worker();
            }
            else {
                print("Invalid choice. Please choose again.");
                continue;
            }

            user.inject(scanner, workMatchings,workerRepo,serviceRepo, adminRepo, assignmentService);
            user.handleStart();
        }
    }
}
