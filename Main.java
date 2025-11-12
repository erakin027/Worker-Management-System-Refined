import java.util.Scanner;
import java.util.LinkedHashMap;

public class Main {
    public static void print(String message){
        System.out.println(message);
    }

    public static void main(String[] args) {
        print("Welcome to our Worker Booking System!!");
        Admin admin = null;

        LinkedHashMap <String, Work> workMatchings = new LinkedHashMap<>();
        workMatchings.put("Window Cleaning", new WindowCleaning());
        workMatchings.put("Mopping", new Mopping());
        workMatchings.put("Sweeping", new Sweeping());
        workMatchings.put("Fan Cleaning", new FanCleaning());
        workMatchings.put("Bathroom Cleaning", new BathroomCleaning());
        workMatchings.put("Mowing", new Mowing());
        workMatchings.put("Pruning", new Pruning());
        workMatchings.put("Washing", new Washing());
        workMatchings.put("Drying", new Drying());
        workMatchings.put("Ironing", new Ironing());

        Scanner scanner = new Scanner(System.in);


        while (true) {
            print("\nPlease enter (1/2/3) to proceed:");
            print("1. Sign Up\n2. Login\n3. Exit");
            String sion = scanner.nextLine();

            if (sion.equals("1")) { // Sign Up
                print("Are you the Admin or a Worker?");
                String ador = scanner.nextLine();

                if(!ador.equalsIgnoreCase("Admin") && !ador.equalsIgnoreCase("Worker")){
                    print("Invalid input. Please choose 'Admin' or 'Worker'.");
                    continue;
                }

                if (ador.equalsIgnoreCase("Admin")){
                    print("Error: Admin is already registered.");
                    continue;
                }
                //admin can't be registered;

                //this is worker registration
                print("Enter your ID: ");
                String uId = scanner.nextLine();
                print("Enter password: ");
                String pass = scanner.nextLine();
                print("Reconfirm your password: ");
                String repass = scanner.nextLine();
                while (!pass.equals(repass)) {
                    print("Passwords do not match. Please try again.");
                    print("Enter password: ");
                    pass = scanner.nextLine();
                    print("Reconfirm your password: ");
                    repass = scanner.nextLine();

                    Worker worker = new Worker(uId, pass);
                    worker.register(scanner, workMatchings);
                }

            }

            else if (sion.equals("2")) { // Login
                print("Are you the Admin or a Worker?");
                String ador = scanner.nextLine();

                if (ador.equalsIgnoreCase("Admin")) {
                    print("Enter Admin Id ");
                    String uId = scanner.nextLine();

                    print("Enter Admin password: ");
                    String pass = scanner.nextLine();

                    admin = new Admin (uId, pass);
                    if (admin.login()) {;
                        print("Admin logged in successfully.");
                        admin.displayMenu(scanner,workMatchings);
                    } 
                    else {
                        print("Admin credentials are incorrect.");
                    }
                }
                else if (ador.equalsIgnoreCase("Worker")) {
                    print("Enter your userId: ");
                    String uId = scanner.nextLine();

                    print("Enter your password: ");
                    String pass = scanner.nextLine();

                    Worker worker = new Worker(uId, pass);
                    int workerLogged = worker.login(workMatchings);
                    if (workerLogged == 1){
                        worker.displayMenu(scanner, workMatchings);
                    }
                } 
                else {
                    print("Invalid input. Please choose 'Admin' or 'Worker'.");
                }
            } 
            else if (sion.equals("3")) { // Exit
                print("Exiting the system. Goodbye!");
                break;
            } 
            else {
                print("Invalid option. Please try again.");
            }
        }
    }
}