import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.stream.Collectors;

public class Request{
    private int status;
    private String serviceId,typeName,planName,bookingDate, bookingTime;  //requestedService
    private String  locality, customerId, customerGender, address, preferredGender;
    private ArrayList<Work> requestedWorks;

    //these fields are entered service booked:
    private double price; //discount applied if possible;
    private String workDate, workStartTime, workEndTime;
    public Request(String[] fields, LinkedHashMap <String, Work> workMatchings){
        this.serviceId = fields[0];
        this.status = Integer.parseInt(fields[1]);
        this.typeName = fields[2];
        this.planName  = fields[3];
        this.bookingDate = fields[4];
        this.bookingTime = fields[5];
        this.locality = fields[6];
        this.customerId = fields[7];
        this.customerGender = fields[8];
        this.address = fields[9];
        this.requestedWorks = new ArrayList<>();

        String[] works = fields[10].substring(1,fields[10].length()-1).split(",");
        for (String work :works){
            this.requestedWorks.add(workMatchings.get(work));
        }
        this.preferredGender = fields[11];
    }

    //this has details of a line in Service.txt
    //status, serviceID(to identify services in Service.txt)
    //planID(basic,medium,premium)
    //typeID(immediate scheduling or subscription)
    //bookingDate, bookingTime, locality, customerID,customerGEnder,address
    //requestwork - can be accessed by serviceId;

    public String getServiceId() {
        return serviceId;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status){
        this.status = status;
    }

    public String getTypeName() {
        return typeName;
    }

    public String getPlanName() {
        return planName;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public String getBookingTime() {
        return bookingTime;
    }

    public String getLocality() {
        return locality;
    }

    public String getCustomerId() {
        return customerId;
    }

    public String getCustomerGender() {
        return customerGender;
    }

    public double getPrice(){
        return price;
    }
    public void setPrice(double price){
        this.price = price;
    }

    public void incrementPrice(double price){
        this.price+=price;
    }

    public String getAddress() {
        return address;
    }

    public ArrayList<Work> getRequestedWorks() {
        return new ArrayList<>(requestedWorks);
    }

    public String getPreferredGender() {
        return preferredGender;
    }

    public String getWorkDate(){
        return workDate;
    }

    public String getWorkTime(){
        return workStartTime;
    }

    public String getWorkEndTime(){
        return workEndTime;
    }
    public void setWorkEndtime(String endTime){
        this.workEndTime = endTime;
    }

    public void setWorkDate(String date){
        this.workDate = date;
    }

    public void setWorkTime(String time){
        this.workStartTime = time;
    }

    public double getDiscount(){
        switch ("this.planName"){
            case "Basic":
                return 0;

            case "Intermediate":
                return 0.1;

            case "Premium":
                return 0.2;
            default:
                return 0;
        }
    }

    public String process(ArrayList<Worker> workers, Scanner scanner){return "";}

}

class Immediate extends Request{
    public Immediate (String[] fields, LinkedHashMap <String, Work> workMatchings){
        super(fields, workMatchings);
    }

    @Override
    public String process(ArrayList <Worker> workers, Scanner scanner){
        ArrayList <Worker> sameLocality = new ArrayList<>();
        for (Worker worker :workers){
            if (worker.getLocality().equals(getLocality()) && (getPreferredGender().equals("NP") || getPreferredGender().equals(worker.getGender())) && worker.isAvailableNow()){
                sameLocality.add(worker);
            }
        }

        LinkedHashMap<Worker, ArrayList<Work>> workAssignments = new LinkedHashMap<>();
        ArrayList<Work> remainingWorks = new ArrayList<>(getRequestedWorks());

        for (Work work : getRequestedWorks()){ 
            Worker matchedWorker = findEligibleWorker(work, sameLocality);
            if (matchedWorker !=null){
                workAssignments.computeIfAbsent(matchedWorker, k->new ArrayList<>()).add(work);        
                remainingWorks.remove(work);     
            }
        }

        if (!remainingWorks.isEmpty()){
            // If any one of the work cannot be assigned, reject the request
            setStatus(-1);
            String reason = "Workers not available for all tasks";
            String newLine = generateLine("[]", reason);
            System.out.println("Immediate request rejected for " + getCustomerId() + " : "+reason);
            return newLine;
        }
        
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        setWorkDate(now.format(dateFormatter));
        setWorkTime(now.format(timeFormatter));
        
        for (Worker worker : workAssignments.keySet()){
            worker.setAvailable(false);
            worker.addBookingId(getServiceId());
            worker.updateWorkerFile("Workers.txt", worker.getWorkerId(), worker.getWorkerPass());
        }
        setStatus(1);

        int maxTime = 0;

        for (Map.Entry<Worker, ArrayList<Work>> entry : workAssignments.entrySet()) {
            ArrayList<Work> works = entry.getValue();
            int sumTime= works.stream().mapToInt(Work::getTimeRequired).sum(); 

            // Update the overall max time
            maxTime = Math.max(maxTime, sumTime);
        }
        setWorkEndtime(calculateEndTime(getWorkTime(),maxTime));

        String matchedWorkers = "[" + String.join(",", workAssignments.keySet().stream()
                                                          .map(Worker::getWorkerId).toList()) + "]";

        setPrice(0);                                             
        for (Work work: getRequestedWorks()){
            incrementPrice(work.getPrice());
        }
        setPrice((1-getDiscount())*getPrice());
        String newLine = generateLine(matchedWorkers, "");
        System.out.println("Immediate request processed for " + getCustomerId());

        System.out.println(newLine);
        return newLine;
    
    }

    public Worker findEligibleWorker(Work work, ArrayList<Worker>sameLocality){
        for (Worker worker :sameLocality){
            if(worker.getCapableWorks().contains(work)){
                return worker;
            }
        }
        return null;
    }

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

    public String generateLine(String matchedWorkers, String unMatchedReason){
        String workss = getRequestedWorks().stream()
                                   .filter(Objects::nonNull) // Ignore null elements
                                   .map(Work::getWorkName)
                                   .collect(Collectors.joining(",", "[", "]"));

        

        if (getStatus() == -1){
            return getServiceId() + "#" + getStatus() + "#" + getTypeName() + "#" + getPlanName() + "#" +
               getBookingDate() + "#" + getBookingTime() + "#" + getLocality() + "#" + getCustomerId() + "#" +
               getCustomerGender() + "#" + getAddress() + "#" +  workss + "#" + getPreferredGender() + "#" + unMatchedReason;
        }
        return getServiceId() + "#" + getStatus() + "#" + getTypeName() + "#" + getPlanName() + "#" +
               getBookingDate() + "#" + getBookingTime() + "#" + getLocality() + "#" + getCustomerId() + "#" +
               getCustomerGender() + "#" + getAddress() + "#" + workss + "#" + getPreferredGender() + "#" +
               getWorkDate() + "#" + getWorkTime() +"#"+ getWorkEndTime() + "#" + matchedWorkers+"#" + getPrice();
    }
}


class Scheduled extends Request{
    public Scheduled(String[] fields,LinkedHashMap <String, Work> workMatchings){
        super(fields, workMatchings);
        setWorkDate(fields[12]);
        setWorkTime(fields[13]);
    }

    @Override
    public String process(ArrayList<Worker>workers, Scanner scanner){
        ArrayList<Worker> sameLocality = new ArrayList<>();

        System.out.println("Processing scheduled request for Customer " + getCustomerId() +"\n");
        
        //1 : filtering eligible workers
        for (Worker worker : workers) {
            if (worker.getLocality().equals(getLocality()) && (getPreferredGender().equals("NP") || getPreferredGender().equals(worker.getGender()))) {   
                long matchingCount = worker.getCapableWorks().stream().filter(getRequestedWorks()::contains).count();
                if (matchingCount>0){
                    worker.setMatchingCount(matchingCount);
                    sameLocality.add(worker);
                }
            }
        }
        sameLocality.sort((w1, w2) -> Long.compare(w2.getMatchingCount(), w1.getMatchingCount()));


        if (!sameLocality.isEmpty()){
            //2: display eligible workers
            System.out.println("Eligible workers for Scheduled request:\n");
            for (Worker worker : sameLocality){
                if (worker!= null){
                    System.out.println("Worker Id: " + worker.getWorkerId()+ ", Worker gender: " + worker.getGender()+ ", Capable works:" + worker.getCapableWorks().stream().map(Work::getWorkName).collect(Collectors.joining(", ", "[", "]")));
                    System.out.println("No. of matching works for " + worker.getWorkerId() + " are " + worker.getMatchingCount() + "\n");
                }
            }

            //3 : user manually enters worker id
            System.out.println("Enter the IDs of workers to assign (seperated by space), or type 'not aviable' if no workers are suitable:");
            String input = scanner.nextLine();

            if (input.equalsIgnoreCase("not available")){
                System.out.println("Service request rejected: No suitable workers.");
                setStatus(-1);
                return generateLine(new ArrayList<>(), "No workers available in the specified timings.");
            }

            String[] assignedIds = input.split(" ");
            ArrayList<Worker> assignedWorkers = new ArrayList<>();

            for(String workerId :assignedIds){
                Worker assignedWorker = sameLocality.stream().filter(w->w.getWorkerId().equals(workerId.trim())).findFirst().orElse(null);
                if (assignedWorker!= null){
                    assignedWorkers.add(assignedWorker);
                }
                else{
                    System.out.println("Invalid worker ID: " + workerId);
                }
            }

            if (assignedWorkers.isEmpty()) {
                System.out.println("No workers assigned.");
                setStatus(-1);
                return generateLine(new ArrayList<>(), "No workers assigned.");
            }

            //4: assigning works to workers and checking all works satisfied
            LinkedHashMap<Worker, ArrayList<Work>> workerAssignments = new LinkedHashMap<>();
            //ArrayList<String> unassignedWorks = new ArrayList<>(getRequestedWorks());

            for (Work work : getRequestedWorks()){
                Worker selectedWorker = assignedWorkers.stream()
                .filter(w -> w.getCapableWorks().contains(work))
                .min(Comparator.comparingInt(w -> workerAssignments.getOrDefault(w, new ArrayList<>()).size()))
                .orElse(null);

                if(selectedWorker != null){
                    workerAssignments.computeIfAbsent(selectedWorker, k->new ArrayList<>()).add(work);
                }
            }

            //5:check if all works assigned
            ArrayList<Work> allAssignedWorks = new ArrayList<>();
            for (ArrayList<Work> works : workerAssignments.values()) {
                allAssignedWorks.addAll(works);
            }

            boolean allWorksSatisfied = getRequestedWorks().stream().allMatch(allAssignedWorks::contains);

            if (!allWorksSatisfied) {
                System.out.println("Service request rejected: No suitable worker assigned");
                setStatus(-1);
                return generateLine(new ArrayList<>(), "No workers assigned");
            } 
            
            //6:total time
            int maxDuration = 0;
            for (Worker worker : workerAssignments.keySet()) {
                int workerDuration = workerAssignments.get(worker).stream()
                                                    .mapToInt(Work::getTimeRequired)
                                                    .sum();
                maxDuration = Math.max(maxDuration, workerDuration);
            }
            setWorkEndtime(calculateEndTime(getWorkTime(), maxDuration));
            System.out.println("Service request accepted: Workers assigned.");

            //display worker assignments - optional
            for (Map.Entry<Worker, ArrayList<Work>> entry : workerAssignments.entrySet()) {
                String workss = entry.getValue().stream().map(Work::getWorkName).collect(Collectors.joining(","));
                System.out.println("Worker ID: " + entry.getKey().getWorkerId() +
                                   ", Assigned works: " + workss);
            }
            System.out.println("");
            setStatus(1);
            for (Worker worker : workerAssignments.keySet()){
                worker.addBookingId(getServiceId());
                worker.updateWorkerFile("Workers.txt", worker.getWorkerId(), worker.getWorkerPass());
            }
            setPrice(0);                                             
            for (Work work: getRequestedWorks()){
                incrementPrice(work.getPrice());
            }
            setPrice((1-getDiscount())*getPrice());
            return generateLine(assignedWorkers, "");
            
        }  
        else{
            System.out.println("No eligible workers for the requested services.");
            setStatus(-1);
            return generateLine(new ArrayList<>(),"No workers available in the locality with specified gender preference.");
        }
    }

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

    public String generateLine(ArrayList<Worker> assignedWorkers, String reason) {
        String workss = getRequestedWorks().stream()
                                   .filter(Objects::nonNull) // Ignore null elements
                                   .map(Work::getWorkName)
                                   .collect(Collectors.joining(",", "[", "]"));

        if (getStatus() == -1){
            return  getServiceId() + "#" + getStatus() + "#" + getTypeName() + "#" + getPlanName() + "#" + getBookingDate() + "#" +
            getBookingTime() + "#" + getLocality() + "#" + getCustomerId() + "#" + getCustomerGender() + "#" + getAddress() +
            "#" + workss + "#" + getPreferredGender() + "#" + getWorkDate() + "#" + getWorkTime() + "#" + reason;
        }
        String assignedWorkerIds = "[" + assignedWorkers.stream().map(Worker::getWorkerId).collect(Collectors.joining(", ")) + "]";
        return getServiceId() + "#" + getStatus() + "#" + getTypeName()+ "#"+ getPlanName()+ "#"+ getBookingDate() + "#" + getBookingTime() + "#" +
                getLocality() + "#" + getCustomerId() + "#" + getCustomerGender() + "#" + getAddress() + "#" + workss+"#" + getPreferredGender()+"#"+ getWorkDate()+"#"+ getWorkTime()+"#"+getWorkEndTime()
                +"#" + assignedWorkerIds +"#" + getPrice();
    }

    
}