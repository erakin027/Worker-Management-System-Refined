import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Scanner;

public class Admin extends User{
    private String adminId;
    private String adminPass;
    private ArrayList<Worker> workers;

    public Admin(String adminId, String adminPass) {
        this.adminId = adminId;
        this.adminPass = adminPass;
        this.workers = new ArrayList<>();
    }

    public String getId() {
        return adminId;
    }

    public String getPass() {
        return adminPass;
    }
    
    public boolean login() {
        try (BufferedReader reader = new BufferedReader(new FileReader("Admin.txt"))) {
            String line = reader.readLine();
            if (line != null) {
                String[] credentials = line.split("#");
                return credentials[0].equals(getId()) && credentials[1].equals(getPass());
            }
        } catch (IOException e) {
            System.out.println("Error reading Admin.txt file.");
        }
        return false;
    }

    public void loadWorkers(LinkedHashMap <String, Work> workMatchings){
        this.workers = new ArrayList<>();
        String fileName = "Workers.txt";

        try(BufferedReader reader = new BufferedReader(new FileReader(fileName))){
            String line;
            while((line =reader.readLine())!= null){
                String[] fields = line.split("#");
                //print(line);
                this.workers.add(new Worker(fields, workMatchings));
            }
        }
        catch(IOException e){
            print("jaa");
            //e.printStackTrace();
        }
        
    }
    public void LoadPending(LinkedHashMap <String, Work> workMatchings){
        //all pending requests
        //append each request to PendingRequests vector
        String fileName = "services.txt";
        loadWorkers(workMatchings);
        Scanner scanner = new Scanner(System.in);
        StringBuilder updatedFileContent = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))){
            String line;
            while((line = reader.readLine())!=null){
                String[] fields = line.split("#");
                //print(line);
                //fields[0] = serviceId;
                //fields[1] represents status

                int status = Integer.parseInt(fields[1]);
                //status 0 - customer requested, worker not assigned
                //status -1 - worker not assigned
                //status 1 - worker assigned

                if(status ==0){
                    //fields[0] is serviceId;
                    Request request = null;
                    String typeName = fields[2];
                    switch (typeName){
                        
                        case "Immediate":                    
                        //int serviceId = Integer.parseInt(fields[0]);
                            request = new Immediate(fields, workMatchings);
                            break;
                        case "Scheduling":
                            request = new Scheduled(fields, workMatchings);
                            break;
                        default:
                            print("Unknown Service");
                    }
                    if(request!=null){
                        line = request.process(this.workers,scanner);      
                    }
                }
                updatedFileContent.append(line).append(System.lineSeparator());
            }
        }
        catch (IOException e){
            System.out.println("Error reading from Services.txt");
        }
        try(BufferedWriter writer = new BufferedWriter(new FileWriter("services.txt"))){
            writer.write(updatedFileContent.toString());
        }

        catch (IOException e){
            System.out.println("Error reading from Services.txt");
        }
    }

    public void displayMenu(Scanner scanner, LinkedHashMap <String, Work> workMatchings){
        while(true){
            print("What do you want to do next?");
            print("1.Load Pending Requests and Assign Workers");
            print("2.Logout");
            
            int command = Integer.parseInt(scanner.nextLine());
            if(command == 1){
                print("assigning worker");
                LoadPending(workMatchings);
            }
            else if (command == 2){
                return;
            }
            else{
                print("Invalid choice");
            }
        }
    }
    
}

