import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Worker extends User{
    private String workerId;
    private String workerPass;
    private String name;
    private String gender;
    private String area;
    private ArrayList<Work> capableWorks;
    private boolean isAvailable;
    long matchingCount;
    ArrayList<String>bookings;

    public String getName() {
        return name;
    }
    
    public ArrayList<Work> getCapableWorks(){
        return capableWorks;
    }
    public String getLocality(){
        return area;
    }

    public Worker(String workerId, String workerPass) {
        this.workerId = workerId;
        this.workerPass = workerPass;
    }

    public Worker(String[] fields, LinkedHashMap <String, Work> workMatchings){
        this.workerId = fields[0];
        this.workerPass = fields[1];
        this.name = fields[2];
        this.gender = fields[3];
        this.area = fields[4];
        int numberCapable = Integer.parseInt(fields[5]);
        //print(String.valueOf(numberCapable));
        this.capableWorks = new ArrayList<>();

        int i;
        for (i=0;i<numberCapable;i++){
            if (6+i<fields.length){
                this.capableWorks.add(workMatchings.get(fields[6+i]));
            }
        }
        i = 6+i;
        this.isAvailable = fields[i].trim().equals("true")  ? true : false;
        String[] booking =  fields[i+1].substring(1,fields[i+1].length()-1).split(",");
        this.bookings = new ArrayList<>();
        for(String temp :booking){
            if (temp != null && !temp.isEmpty()) { // Filter null or empty strings
                this.bookings.add(temp);
            }
        }
        
    }   

    // Check if the worker is available now (for immediate bookings)
    public boolean isAvailableNow() {
        return isAvailable;
    }

    // Setter for availability
    public void setAvailable(boolean available) {
        this.isAvailable = available;
    }

    public String getGender(){
        return gender;
    }


    // Get worker ID
    public String getWorkerId() {
        return workerId;
    }

    public String getWorkerPass(){
        return workerPass;
    }

    public long getMatchingCount(){
        return matchingCount;
    }

    public void setMatchingCount(long matchingCount){
        this.matchingCount = matchingCount;
    }

    public void addBookingId(String bookingId){
        this.bookings.add(bookingId);
    }


    public int lookUp(boolean loginCall, LinkedHashMap <String, Work> workMatchings) {  
        //loginCall tells if lookUp function is called from register or login funciton
        File fileName = new File("Workers.txt");

        if(!fileName.exists()){
            //print("No worker records found");
            return -1;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("#");
                //if (parts.length >= 2) {
                String id = parts[0];
                String pass = parts[1];
                if (workerId.equals(id)) {
                    if (loginCall){
                        if (workerPass.equals(pass)) {
                            print("Worker logged in successfully!");
                            this.name = parts[2];
                            this.gender = parts[3];
                            this.area = parts[4];

                            int numberCapable = Integer.parseInt(parts[5]);
                            this.capableWorks = new ArrayList<>();

                            int i;
                            for (i = 0;i<numberCapable;i++){
                                if (6+i<parts.length){
                                    this.capableWorks.add(workMatchings.get(parts[6+i]));
                                }
                            }
                            i = 6+i;
                            this.isAvailable = parts[i] == "true"  ? true : false;
                            this.bookings = new ArrayList<>();
                            String[] booking =  parts[i+1].substring(1,parts[i+1].length()-1).split(",");
                            for(String temp :booking){
                                if (temp!= null  && !temp.isEmpty())
                                    this.bookings.add(temp);
                            }
                            return 1; //id and pswd match
                        }
                        else{
                            return 0; //id exists, pswd incorect
                        }
                    }
                    else {
                        print("Wrong password!");
                        return 1;//id exists - this is called when register() function
                    }
                } 
            }
            return -1; //id doesnt exist
                //0 - id yes, pswd wrong
                //1 - id and pswd match;

        } 
        catch (IOException e) {
            print("error while opening file"+ e.getMessage());
            return -2;

        }   
    }

    public void register(Scanner scanner, LinkedHashMap<String, Work> workMatchings){
        int existing = lookUp(false, workMatchings);
        if (existing != -1){//uId  exist
            print("UserId already exists, please login");
            return;
        }
        //-1 - userId doesnt exist

        this.capableWorks = new ArrayList<>();
        print("What's your name?");
        name = scanner.nextLine();

        print("What is your gender? (Gender should M/F)");

        while(true){
            String gen = scanner.nextLine();
            if (gen == "M"){
                gender = gen;
                break;
            }
            else if (gen == "F"){
                gender = gen;
                break;
            }
            else{
                System.out.println("Gender can be only M or F");
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


        print("How many works are you capable of?");
        print("1. Window Cleaning\n2. Mopping\n3. Sweeping\n4. Fan Cleaning\n5. Bathroom Cleaning\n6. Mowing\n7. Pruning\n8. Washing\n9. Drying\n10. Ironing");
        int num = Integer.parseInt(scanner.nextLine());

        HashMap<Integer, String> workCodes = new HashMap<>();
        workCodes.put(1, "Window Cleaning");
        workCodes.put(2, "Mopping");
        workCodes.put(3, "Sweeping");
        workCodes.put(4, "Fan Cleaning");
        workCodes.put(5, "Bathroom Cleaning");
        workCodes.put(6, "Mowing");
        workCodes.put(7, "Pruning");
        workCodes.put(8, "Washing");
        workCodes.put(9, "Drying");
        workCodes.put(10, "Ironing");

        print("Enter the work codes:");
        for (int i = 0; i < num; i++) {
            int code = Integer.parseInt(scanner.nextLine());
            capableWorks.add(workMatchings.get(workCodes.get(code)));
        }

        isAvailable = true;
        this.bookings = new ArrayList<>();

        String newLine = toString();

        File file = new File("Workers.txt");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("Workers.txt", true))) {
            if(file.exists() && file.length() == 0){
                writer.write(newLine);
            }
            else{
                writer.newLine();
                writer.write(newLine);
            }
            print("Worker registered successfully!");
        } 
        catch (IOException e) {
            e.printStackTrace();
        }
    }

    public int login(LinkedHashMap <String, Work> workMatchings) {
        print("Attempting to log in for Worker ID: " + getWorkerId());
        
        int status = lookUp(true, workMatchings); // Search for credentials in the file and populate details
        
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
                print("If you have forgotten your password, please contact support.");
                return 0;
                
            case -1:
                print("Login failed: Worker ID does not exist.");
                print("Would you like to register instead? Please select the registration option from the main menu.");
                return 0;
                
            case -2:
                print("An error occurred while accessing the file. Please try again later.");
                return 0;
            default:
                print("An unexpected error occurred during login. Please try again.");
                return 0;
        }
    }

    @Override
    public void displayMenu(Scanner scanner,LinkedHashMap <String, Work> workMatchings){
        while(true){
            print("What do you want to do next? (enter number)");
            print("1.Check your details");
            print("2.Edit Details"); //rewritres line
            print("3.Check Bookings");
            print("4.Change Availability"); //rewrite lines
            print("5.Logout");
            
            int command = Integer.parseInt(scanner.nextLine());
            if(command == 1){
                checkDetails();
            }
            else if(command == 2){
                System.out.println("Your present details:");
                checkDetails();
                editDetails(scanner, workMatchings);
            }
            else if (command == 3){ 
                File fileName = new File("services.txt");

                try (BufferedReader reader = new BufferedReader(new FileReader(fileName))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("#");
                        String serviceId = parts[0];
                        for(int i = 0; i < bookings.size(); i++){
                            if(bookings.get(i).equals(serviceId)){
                                System.out.println("Service Id: " + serviceId);
                                System.out.println("Customer ID: " + parts[7]);
                                System.out.println("Customer Address: " + parts[9]);
                                System.out.println("Date of work: " + parts[12]);
                                System.out.println("Start Time: " + parts[13]);
                                System.out.println("End Time: " + parts[14]);
                                System.out.println("\n");
                            }
                        }
                    }
                }
                catch (IOException e){
                    System.out.println("error");
                }
        
            }
            else if (command ==4){
                this.isAvailable = true;
                updateWorkerFile(name, workerId, workerPass);
            }
            else if (command == 5){
                return;
            }
        }
    }

    public void editDetails(Scanner scanner,LinkedHashMap <String, Work> workMatchings) {
        System.out.println("What do you want to edit?");
        System.out.println("1. Locality");
        System.out.println("2. Add Capable Works");

        int com = Integer.parseInt(scanner.nextLine());
        //int com = Integer.parseInt(scanner.nextLine());
    
        if (com == 1) {
            System.out.println("Enter new area code:");
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
            String newArea = scanner.nextLine();
            area = newArea;
        } 
        
        else if (com == 2) {
            System.out.println("Enter how many new works you want to add:");
            int num = Integer.parseInt(scanner.nextLine());
            HashMap<Integer, String> workcodes = new HashMap<>();
            workcodes.put(1, "Window Cleaning");
            workcodes.put(2, "Mopping");
            workcodes.put(3, "Sweeping");
            workcodes.put(4, "Fan Cleaning");
            workcodes.put(5, "Bathroom Cleaning");
            workcodes.put(6, "Mowing");
            workcodes.put(7, "Pruning");
            workcodes.put(8, "Washing");
            workcodes.put(9, "Drying");
            workcodes.put(10, "Ironing");
    
            for (int i = 0; i < num; i++) {
                System.out.println("Enter work code from the following:");
                System.out.println("House Cleaning:\n1. Window Cleaning\n2. Mopping\n3. Sweeping\n4. Fan Cleaning\n5. Bathroom Cleaning");
                System.out.println("Garden Cleaning:\n6. Mowing\n7. Pruning");
                System.out.println("Laundry:\n8. Washing\n9. Drying\n10. Ironing");
    
                int j = Integer.parseInt(scanner.nextLine());
                if (workcodes.containsKey(j)) {
                    capableWorks.add(workMatchings.get(workcodes.get(j)));
                } else {
                    System.out.println("Invalid work code. Skipping...");
                }
            }
        }

        // Rewrite data to file
        updateWorkerFile("Workers.txt", this.workerId, this.workerPass);
        print("Details updated successfully!");
    }

    public void checkDetails(){
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

    public String toString(){
        String newLine = workerId + "#" + workerPass + "#" + name + "#" + gender + "#" + area + "#" + capableWorks.size() + "#";
        for (Work work : capableWorks) {
            newLine += work.getWorkName()+ "#";
        }
        newLine += isAvailable + "#[" + String.join(",", this.bookings)+"]";
        return newLine;
    }

    public void updateWorkerFile(String fileName, String workerId, String workerPass){
        String newLine = toString();
        try {
            List<String> lines = Files.readAllLines(Paths.get(fileName)); // Read all lines into memory
            boolean updated = false;
    
            for (int i = 0; i < lines.size(); i++) {
                String[] parts = lines.get(i).split("#");
                if (getWorkerId().equals(parts[0]) && getWorkerPass().equals(parts[1])) {
                    lines.set(i, newLine);
                    updated = true;
                    break;
                }
            }
    
            if (updated) {
                Files.write(Paths.get(fileName), lines); // Write updated lines back to file
            } else {
                System.out.println("Worker not found in file.");
            }
        } catch (IOException e) {
            System.err.println("An error occurred while updating worker details: " + e.getMessage());
        }
    }
}