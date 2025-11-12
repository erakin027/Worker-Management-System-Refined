#include<iostream>
#include<string>
#include<vector>
#include <algorithm>
#include <map>
#include <set>
#include <fstream>
#include <sstream>
#include <ctime>
#include <regex>

using namespace std;

// some useful helper functions
class Helper{
public:
    static string getCurrDate(){
        time_t now = time(0);
        tm *ltm = localtime(&now);
        // Get current date as string
        string currentDate = to_string(1900 + ltm->tm_year) + "-" + 
                            (ltm->tm_mon + 1 < 10 ? "0" : "") + to_string(1 + ltm->tm_mon) + "-" +
                            (ltm->tm_mday < 10 ? "0" : "") + to_string(ltm->tm_mday);
        return currentDate;
    }

    static string getCurrTime(){
        time_t now = time(0);
        tm *ltm = localtime(&now);
        // Get time as string
        string currentTime = (ltm->tm_hour < 10 ? "0" : "") + to_string(ltm->tm_hour) + ":" +
                            (ltm->tm_min < 10 ? "0" : "") + to_string(ltm->tm_min) + ":" +
                            (ltm->tm_sec < 10 ? "0" : "") + to_string(ltm->tm_sec);
        return currentTime;
    }

    // Function to validate date format and logic
    static bool isValidDate(const string& date) {
        // Regular expression for YYYY-MM-DD format
        regex dateRegex(R"(\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01]))");

        if (!regex_match(date, dateRegex)) {
            return false;
        }

        // Extract year, month, and day
        int year = stoi(date.substr(0, 4));
        int month = stoi(date.substr(5, 2));
        int day = stoi(date.substr(8, 2));

        // Check for valid day ranges in each month
        int daysInMonth[] = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

        // Leap year adjustment for February
        if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
            daysInMonth[1] = 29;
        }

        return day <= daysInMonth[month - 1];
    }

    // Function to validate time format
    static bool isValidTime(const string& time) {
        // Regular expression for HH:MM:SS format
        regex timeRegex(R"((0[0-9]|1[0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9]))");
        return regex_match(time, timeRegex);
    }


    // THE BELOW FUNCTIONS ARE USED IN CHECK SUBSCRIPTION TO CHECK HOW MANY DAYS ARE REMAINING IN THE SUBSCRIPTION

    // Helper function to parse a date string in the format "YYYY-MM-DD"
    static tm parseDate(const string& dateStr) {
        tm date = {};
        stringstream ss(dateStr);
        string year, month, day;

        getline(ss, year, '-');
        getline(ss, month, '-');
        getline(ss, day, '-');

        date.tm_year = stoi(year) - 1900; // tm_year is years since 1900
        date.tm_mon = stoi(month) - 1;   // tm_mon is 0-based
        date.tm_mday = stoi(day);

        return date;
    }

    // Helper function to calculate the difference in days
    static int daysRemaining(const string& endDateStr) {
        // Parse the subscription end date
        tm endDate = parseDate(endDateStr);

        // Get the current date
        time_t now = time(0);
        tm* currentDate = localtime(&now);

        // Convert both dates to time_t for comparison
        time_t end = mktime(&endDate);
        time_t current = mktime(currentDate);

        // Calculate the difference in seconds and convert to days
        double seconds = difftime(end, current);
        return static_cast<int>(seconds / (60 * 60 * 24)); // Seconds to days
    }
};


class Service{
protected:
    //necessary to request a service
    int serviceID;
    int status;
    string statusStr;
    string type;
    string plan;
    string bookingDate;
    string bookingTime;
    string locality;
    string customerID;
    string customerGender;
    string address;
    vector<string> requestedServices;
    string genderPref;
    // updated line from admin has these extra
    string workDate;
    string workStartTime;
    string workEndTime;
    string assignedWorkerIDs;
    string reason;
    string price;

    // sets statusStr based on the status 
    void refreshStatus(){
        string currentDate = Helper::getCurrDate();
        string currentTime = Helper::getCurrTime();

        if(status == 0){
            statusStr = "Pending";
        }
        else if(status == -1){
            statusStr = "Rejected";
        }
        else if(status == 1 && (workDate > currentDate || (workDate == currentDate && workEndTime > currentTime))){
            statusStr = "Assigned";
        }
        else if(status == 1 && (workDate < currentDate || (workDate == currentDate && workEndTime < currentTime))){
            statusStr = "Completed";
        }
        // cout << "Refresh done"<< endl;
    }

public:
    // to create a new request ; MAIN PURPOSE : TO CREATE A REQUEST
    Service(const string& type, const string& plan, const string& locality, const string& customerID, const string& gender, const string&address, const vector<string>& requestedServices, const string& genderPreference)
        : type(type), plan(plan), locality(locality),customerID(customerID), customerGender(gender), address(address), requestedServices(requestedServices), genderPref(genderPreference) {

        // Determine line number in services.txt (count of existing lines)
        // The line no will be our service ID
        ifstream file("services.txt");
        string temp;
        int lineNo = 0;
        while (getline(file, temp))
            lineNo++;
        file.close();
        serviceID = lineNo + 1;

        // as it will create a new request, set default status 0
        status = 0;

        // Get current date and time
        bookingDate = Helper::getCurrDate();
        bookingTime = Helper::getCurrTime();
    }

    // if input is the line from services.txt - MAIN PURPOSE: READ 
    Service(const string& line){
        stringstream ss(line);
        vector<string> fields;
        string field;

        // Split the line by '#'
        while (getline(ss, field, '#')) {
            fields.push_back(field);
        }
        serviceID = stoi(fields[0]);
        status = stoi(fields[1]);
        type = fields[2];
        plan = fields[3];
        bookingDate = fields[4];
        bookingTime = fields[5];
        locality = fields[6];
        customerID = fields[7];
        customerGender = fields[8];
        address = fields[9];        

        string works_str = fields[10].substr(1, fields[10].length() - 2);
        stringstream ss1(works_str);
        string work;
        // Split the line by ','
        while (getline(ss1, work, ',')) {
            requestedServices.push_back(work);
        }
        genderPref = fields[11];
    }

    virtual ~Service() {}

    int getServiceID(){
        return serviceID;
    }

    string getStatus(){
        refreshStatus();
        return statusStr;
    }
    
    string getType(){
        return type;
    }

    string getPlan(){
        return plan;
    }

    vector<string> getRequestedServices(){
        return requestedServices;
    }

    string getGenderPref(){
        return genderPref;
    }

    virtual void addToDB() = 0;

    // Display service details (override in subclasses if needed)
    virtual void displayServiceDetails() {
        cout << "Service ID: " << serviceID << "\n"
             << "Status: " << statusStr <<  "\n"
             << "Type: " << type << "\n"
             << "Plan: " << plan << "\n"
             << "Booking Date: " << bookingDate << "\n"
             << "Booking Time: " << bookingTime << "\n"
             << "Requested Services: " << "\n";
        for (const auto& service : requestedServices)
            cout << service << " ";
        cout << endl;
    }

};

class Immediate : public Service {

public:
    Immediate(const string& plan, const string& locality, const string& customerID, const string& gender, const string&address, const vector<string>& requestedServices, const string& genderPreference)
        : Service("Immediate", plan, locality, customerID, gender, address, requestedServices, genderPreference){}

    Immediate(const string& line) : Service(line) {
        stringstream ss(line);
        vector<string> fields;
        string field;

        // Split the line by '#'
        while (getline(ss, field, '#')) {
            fields.push_back(field);
        }
        refreshStatus();
        if(status == 1){
            workDate = fields[12];
            workStartTime = fields[13];
            workEndTime = fields[14];
            assignedWorkerIDs = fields[15];
            price = fields[16]; 
        }
        else if(status == -1){
            if (fields.size() > 12){
                reason = fields[12];
            }
        }

    }

    void addToDB() override {
        ofstream services("services.txt", ios::app);
        services << serviceID << '#' << status << '#' << type << '#' << plan << '#' << bookingDate << '#' << bookingTime << '#' << locality << '#' << customerID << '#' << customerGender << '#' << address << '#' << "[";
        for(int i = 0; i < requestedServices.size(); i++){
            services << requestedServices[i];
            if (i < requestedServices.size() - 1) {
                services << ",";  // Add a comma only if it's not the last element
            }
        }
        services << "]" << "#" << genderPref <<  "\n";
        services.close();
    }

    void displayServiceDetails() override {
        Service::displayServiceDetails();
        if(status == 1){
            cout << "Work Date: " << workDate << "\n"
                << "Work Start Time: " << workStartTime << "\n"
                << "Work End Time: " << workEndTime << "\n"
                << "Assigned Worker IDs: " << assignedWorkerIDs << "\n"
                << "Price: " << price << "\n";
        }
        else if(status == -1){
            cout << "Reason for Rejection: " << reason << endl;
        }
    }
};

class Scheduling : public Service {
    string scheduledDate;
    string scheduledTime;

public:
    Scheduling(const string& plan, const string& locality, const string& customerID, const string& gender, const string&address, const vector<string>& requestedServices, const string& genderPreference, const string& scheduledDate, const string& scheduledTime)
        : Service("Scheduling", plan, locality, customerID, gender, address, requestedServices, genderPreference),
          scheduledDate(scheduledDate), scheduledTime(scheduledTime) {}

    Scheduling(const string& line) : Service(line) {
        stringstream ss(line);
        vector<string> fields;
        string field;

        // Split the line by '#'
        while (getline(ss, field, '#')) {
            fields.push_back(field);
        }

        scheduledDate = fields[12];
        scheduledTime = fields[13];

        refreshStatus();
        if(status == 1){
            workDate = fields[12];
            workStartTime = fields[13];
            workEndTime = fields[14];
            assignedWorkerIDs = fields[15];
            price = fields[16];
        }
        else if(status == -1){
            if (fields.size() > 14){
                reason = fields[14];
            }
        }

    }

    void addToDB() override {
        ofstream services("services.txt", ios::app);
        services << serviceID << '#' << status << '#' << type << '#' << plan << '#' << bookingDate << '#' << bookingTime << '#' << locality << '#' << customerID << '#' << customerGender << '#' << address << '#' << "[";
        for(int i = 0; i < requestedServices.size(); i++){
            services << requestedServices[i];
            if (i < requestedServices.size() - 1) {
                services << ",";  // Add a comma only if it's not the last element
            }
        }
        services << "]#" << genderPref << '#' << scheduledDate << '#' << scheduledTime << "\n";
        services.close();
    }

    void displayServiceDetails() override {
        Service::displayServiceDetails();
        if(status == 0){
            cout << "Scheduled Date: " << scheduledDate << "\n"
                << "Scheduled Time: " << scheduledTime << "\n";
        }
        else if(status == 1){
            cout << "Work Date: " << workDate << "\n"
                << "Work Start Time: " << workStartTime << "\n"
                << "Work End Time: " << workEndTime << "\n"
                << "Assigned Worker IDs: " << assignedWorkerIDs << "\n"
                << "Price: " << price << "\n";
        }
        else if(status == -1){
            cout << "Reason for Rejection: " << reason << endl;
        }
    }
};

class Customer{
private:
    string ID; //Note: ID and phone number are the same
    string password;
    string name;
    string gender;
    string locality;
    string address;
    vector <int> bookingsIDs; // a vector of just the service IDs of the services requested by the customer
    vector<Service*> history; // a set of all past bookings - USEFUL FOR SHOWHISTORY, REBOOK AND RATING

    // this method updates customer.txt with the updated details of "this" customer, if any
    void refreshCustomerDB(){
        // Modify the record in the file
        fstream file("customer.txt", ios::in | ios::out);

        string line;
        stringstream buffer;
        while (getline(file, line)) {
            stringstream ss(line);
            string fileID;
            getline(ss, fileID, '#');

            if (fileID == ID) {
                // Found the customer, now update their details
                ss.seekg(0, ios::beg); // Go to the start of the line
                buffer << ID << "#" << password << "#" << name << "#" << gender << "#" << locality << "#" << address << "#" << "[";

                // Iterate through the bookingsIDs vector and append service IDs to the buffer
                for (int i = 0; i < bookingsIDs.size(); i++) {
                    buffer << bookingsIDs[i];
                    if (i < bookingsIDs.size()-1) {
                        buffer << ",";  // Add a comma only if it's not the last element
                    }
                }
                buffer << "]\n";

                continue;
            }
            buffer << line << "\n";
        }

        // Rewriting the file with updated details
        file.close();
        ofstream outFile("customer.txt");
        outFile << buffer.str();
        outFile.close();
    }

    // fetches all bookings of the customers from the stored serviceIDs in bookingIDs vector and updates bookings vector
    vector<Service*> fetchBookings(){
        
        ifstream file("services.txt");

        if (!file) {
            cout << "ERROR: Unable to open services.txt file.\n";
            return {};
        }

        string line;
        vector <string> bookingsStr;
        vector<Service*> bookings;

        while (getline(file, line)) {
            // Extract the first part of the line (before the #)
            stringstream ss(line);
            string fileServiceID;
            getline(ss, fileServiceID, '#');

            // Convert the first part to an integer
            int bookingID = stoi(fileServiceID);

            // Check if the booking ID is in the bookingIDs vector
            if (find(bookingsIDs.begin(), bookingsIDs.end(), bookingID) != bookingsIDs.end()) {
                bookingsStr.push_back(line); // Add the service to the bookingsStr vector
            }
        }
        file.close();

        for(string line : bookingsStr){
            stringstream ss(line);
            vector<string> fields;
            string field;

            // Split the line by '#'
            while (getline(ss, field, '#')) {
                fields.push_back(field);
            }
            if(fields[2] == "Immediate"){
                Service* newService = new Immediate(line);
                bookings.push_back(newService);
            }
            else if(fields[2] == "Scheduling"){
                Service* newService = new Scheduling(line);
                bookings.push_back(newService);
            }
        }
        return bookings;
    }

public:
    //constructor
    Customer(string id, string pass, string name, string gender, string loc, string addr, vector<int> bookingsIDs)
        : ID(id), password(pass), name(name), gender(gender), locality(loc), address(addr), bookingsIDs(bookingsIDs) {}

    string getID(){
        return ID;
    }

    string getCustomerGender(){
        return gender;
    }

    string getLocality(){
        return locality;
    }

    string getAddress(){
        return address;
    }

    void serviceRequest() {
        cout << "Processing service request...\n";

        int typeID, planID;
        string type, plan;
        string genderPref;
        vector<string> requestedServices;

        vector<string> availableWorks = {"Window Cleaning", "Mopping", "Sweeping", "Fan Cleaning", "Bathroom Cleaning", 
                                        "Mowing", "Pruning", "Washing", "Drying", "Ironing"};
        vector<string> availablePackages = {"House Cleaning", "Garden Cleaning", "Laundry"};

        // Map packages to their respective works
        map<string, vector<string>> packageMap = {
            {"House Cleaning(will have service codes 1-5)", {"Window Cleaning", "Mopping", "Sweeping", "Fan Cleaning", "Bathroom Cleaning"}},
            {"Garden Cleaning(will have service codes 6-7)", {"Mowing", "Pruning"}},
            {"Laundry(will have service codes 8-10)", {"Washing", "Drying", "Ironing"}}
        };
    
        cout << "Select type of service you want (1/2): \n";
        cout << "1. Immediate\n2. Scheduling\n";
        cin >> typeID;
        if(typeID <= 0 || typeID >=3){
            cout << "ERROR: Invalid type selection\n";
            return;
        }

        // Display available works and packages
        cout << "Available services:\n";
        for (int i = 0; i < availableWorks.size(); ++i) {
            cout << i + 1 << ". " << availableWorks[i] << "\n";
        }
        cout << "Available packages(You can book this entire package as premium):\n";
        for (int i = 0; i < availablePackages.size(); ++i) {
            cout << i + 1 << ". " << availablePackages[i] << "\n";
        }

        cout << "Select the type of plan you'd like to take (1/2/3): \n";
        cout << "1. Basic - select 1 service (no discount overall)\n2. Intermediate - select 3 services(10% discount on total price)\n3. Premium - select 5 services/ 1 package (20% discount on total price)\n";
        cin >> planID; 
        if(planID <= 0 || planID >=4){
            cout << "ERROR: Invalid plan selection\n";
            return;
        }
        if(planID == 1){
            plan = "Basic";
        }
        else if(planID == 2){
            plan = "Intermediate";
        }
        else if(planID == 3){
            plan = "Premium";
        }

        if(planID == 1 || planID == 2){
            // PlanID 1 or 2 (select respective number of works)
            int selectionCount = (planID == 1) ? 1 : 3;
            cout << "Select " << selectionCount << " service(s) you want to request (enter numbers separated by spaces):\n";
            for (int i = 0; i < selectionCount; i++) {
                int workChoice;
                cin >> workChoice;

                if (workChoice > 0 && workChoice <= availableWorks.size()) {
                    requestedServices.push_back(availableWorks[workChoice - 1]);
                } else {
                    cout << "Invalid work selection. Please restart the request.\n";
                    return;
                }
            }
        }

        else if(planID == 3){
            // Ask the user if they want to select 1 package or 5 individual services
            int packageOrServices;
            cout << "Do you want to:\n";
            cout << "1. Select 1 Package\n";
            cout << "2. Select up to 5 Individual Services\n";
            cin >> packageOrServices;

            if (packageOrServices == 1) {
                // Prompt user to select a package
                int packageChoice;
                cout << "Select a package (enter the number): ";
                cin >> packageChoice;

                if (packageChoice > 0 && packageChoice <= availablePackages.size()) {
                    string selectedPackage = availablePackages[packageChoice - 1];
                    requestedServices = packageMap[selectedPackage];  // Add all works in the selected package
                } 
                else {
                    cout << "Invalid package selection. Please restart the request.\n";
                    return;
                }
            }
            else if (packageOrServices == 2) {
                // Allow user to select up to 5 works
                cout << "Select  5 services (enter numbers separated by spaces):\n";
                for (int i = 0; i < 5; ++i) {
                    int workChoice;
                    cin >> workChoice;

                    if (workChoice > 0 && workChoice <= static_cast<int>(availableWorks.size())) {
                        requestedServices.push_back(availableWorks[workChoice - 1]);
                    } else {
                        cout << "Invalid service selection. Please restart the request.\n";
                        return;
                    }
                }
            } else {
                cout << "Invalid choice. Please restart the request.\n";
                return;
            }
        }     
        int gpInt;
        cout << "Do you have a gender preference for the worker? (0 for No Preference, 1 for Male, 2 for Female): ";
        cin >> gpInt;
        switch (gpInt) {
            case 0:
                genderPref = "NP"; //no preference
                break;
            case 1:
                genderPref = "M"; // male pref
                break;
            case 2:
                genderPref = "F"; // female pref
                break;
            default:
                cout << "ERROR: Invalid selection.\n";
                return;
        }

        if(typeID == 1){
            Service* newService = new Immediate(plan, locality, ID, gender, address, requestedServices, genderPref);
            newService->addToDB();
            bookingsIDs.push_back(newService->getServiceID());
            delete newService;
        }

        //scheduling
        else if (typeID == 2) {
            string scheduledDate, scheduledTime;
            cout << "Enter scheduled date (YYYY-MM-DD): ";
            cin >> scheduledDate;
            if (!Helper::isValidDate(scheduledDate)) {
                cout << "ERROR: Invalid date format or logical date. Please try again.\n";
                return; 
            }
            cout << "Enter scheduled time (HH:MM:SS): ";
            cin >> scheduledTime;
            if (!Helper::isValidTime(scheduledTime)) {
                cout << "ERROR: Invalid time format. Please try again.\n";
                return; 
            }

            string currentDate = Helper::getCurrDate();
            string currentTime = Helper::getCurrTime();
            if (scheduledDate < currentDate || (scheduledDate == currentDate && scheduledTime <= currentTime)) {
                cout << "Scheduled date and time must be after the current date and time.\n";
                return;
            }

            Service* newService = new Scheduling(plan, locality, ID, gender, address, requestedServices, genderPref, scheduledDate, scheduledTime);
            newService->addToDB();
            bookingsIDs.push_back(newService->getServiceID());
            delete newService;
        }

        cout << "Service request recorded successfully.\n";
        refreshCustomerDB(); // to update bookingsIDs of the customer in customer.txt 
    }

    // all pending and upcoming requests DO NOT INCLUDE SUBSCRIPTION
    void showCurrBookedServicesInfo() {
        cout << "Displaying current booked services info...\n";

        // Check if there are any bookingsIDs for this customer
        if (bookingsIDs.empty()) {
            cout << "You have no bookings yet.\n";
            return;
        }
        
        vector<Service*> bookings = fetchBookings();
        vector<Service*> pending;
        vector<Service*> upcoming;

        for(Service* service : bookings){
            if(service->getStatus() == "Pending"){
                pending.push_back(service);
            }
            else if(service->getStatus() == "Assigned"){
                upcoming.push_back(service);
            }
        }

        cout << "Upcoming:\n"; //worker assigned, to be completed
        if(upcoming.empty()){
            cout << "No upcoming requests" << endl;
            cout << endl;
        }
        for(int i = 0; i < upcoming.size(); i++){
            upcoming[i]->displayServiceDetails();
            cout << endl;
        }

        cout << "Pending:\n"; //(worker not assigned yet)
        if(pending.empty()){
            cout << "No pending requests" << endl;
            cout << endl;
        }

        for(int i = 0; i < pending.size(); i++){
            pending[i]->displayServiceDetails();
            cout << endl;
        }

        cout << "If you are not able to see your requested service listed here, please go to Show Rejected Requests\n";
        cout << endl;
    }
 
    // updates and shows history
    void showHistory() {
        // Check if there are any bookingsIDs for this customer
        if (bookingsIDs.empty()) {
            cout << "You have no bookings yet.\n";
            return;
        }

        vector<Service*> bookings = fetchBookings();
        vector<Service*> completed;

        for(Service* service : bookings){
            if(service->getStatus() == "Completed"){
                completed.push_back(service);
            }
        }

        cout << "History:\n"; //(worker not assigned yet)
        if(completed.empty()){
            cout << "No completed requests" << endl;
        }
        for(int i = 0; i < completed.size(); i++){
            completed[i]->displayServiceDetails();
            cout << endl;
        }
        history = completed;
    }

    void showRejections() {
        // Check if there are any bookingsIDs for this customer
        if (bookingsIDs.empty()) {
            cout << "You have no bookings yet.\n";
            return;
        }

        vector<Service*> bookings = fetchBookings();
        vector<Service*> rejected;

        for(Service* service : bookings){
            if(service->getStatus() == "Rejected"){
                rejected.push_back(service);
            }
        }

        cout << "Rejected requests:\n"; //(worker not assigned yet)
        if(rejected.empty()){
            cout << "No rejected requests!" << endl;
        }
        for(int i = 0; i < rejected.size(); i++){
            rejected[i]->displayServiceDetails();
            cout << endl;
        }
    }

    void rebookService() {
        showHistory();
        if(history.empty()){
            return;
        }
        // Ask user which service they want to rebook
        cout << "Enter the Service ID of the past service you want to rebook: ";
        int rebookServiceID;
        cin >> rebookServiceID;
        bool proceed = false;
        Service* rbservice;

        for(auto& service : history) {
            if(rebookServiceID == service->getServiceID()){
                rbservice = service;
                proceed = true;
                break;
            }          
        }
        if(!proceed){
            cout << "Unable to find the given past service ID. Please check and proceed again.\n";
            return;
        }

        string plan, genderPref;
        vector<string> requestedServices;
        plan = rbservice->getPlan();
        requestedServices = rbservice->getRequestedServices();
        genderPref = rbservice->getGenderPref();

        int typeID;
        string type;

        cout << "Select type of service you want (1/2): \n";
        cout << "1. Immediate\n2. Scheduling\n";
        cin >> typeID;
        if(typeID <= 0 || typeID >=3){
            cout << "ERROR: Invalid type selection\n";
            return;
        }
        if(typeID == 1){
            type = "Immediate";
        }
        else if(typeID == 2){
            type = "Scheduling";
        }

        if(typeID == 1){
            Service* newService = new Immediate(plan, locality, ID, gender, address, requestedServices, genderPref);
            newService->addToDB();
            bookingsIDs.push_back(newService->getServiceID());
            delete newService;
        }

        //scheduling
        else if (typeID == 2) {
            string scheduledDate, scheduledTime;
            cout << "Enter scheduled date (YYYY-MM-DD): ";
            cin >> scheduledDate;
            if (!Helper::isValidDate(scheduledDate)) {
                cout << "ERROR: Invalid date format or logical date. Please try again.\n";
                return; 
            }
            cout << "Enter scheduled time (HH:MM:SS): ";
            cin >> scheduledTime;
            if (!Helper::isValidTime(scheduledTime)) {
                cout << "ERROR: Invalid time format. Please try again.\n";
                return; 
            }

            string currentDate = Helper::getCurrDate();
            string currentTime = Helper::getCurrTime();
            if (scheduledDate < currentDate || (scheduledDate == currentDate && scheduledTime <= currentTime)) {
                cout << "Scheduled date and time must be after the current date and time.\n";
                return;
            }

            Service* newService = new Scheduling(plan, locality, ID, gender, address, requestedServices, genderPref, scheduledDate, scheduledTime);
            newService->addToDB();
            bookingsIDs.push_back(newService->getServiceID());
            delete newService;
        }

        cout << "Service request recorded successfully.\n";

        refreshCustomerDB(); // to update bookingsIDs of the customer in customer.txt
    }

    void CheckDetails() {
        cout << "Customer details:\n" << "ID: " << ID << "\n" << "Name: " << name << "\n" << "Gender: " << gender << "\n" << "Locality: " << locality << "\n" << "Address: " << address << "\n";
    }

    void editDetails() {
        int choice;
        cout << "What would you like to edit?\n";
        cout << "1. Password\n2. Locality\n3. Address\n4. Go Back\n";
        cout << "Enter your choice: ";
        cin >> choice;

        // Update customer details
        switch (choice) {
            case 1: {
                string oldPass, newPass;
                cout << "Enter your old password: ";
                cin.ignore();
                getline(cin, oldPass);
                if(oldPass != password){
                    cout << "Password not matching your current password.\n";
                    return;
                }
                cout << "Enter your new password: ";
                getline(cin, newPass);
                password = newPass;
                cout << "Password updated\n";
                break;
            }
            case 2: {
                int newLocality;
                cout << "Select your new locality (1-6):\n";
                cout << "1. Moghalrajpuram\n2. Bhavanipuram\n3. Patamata\n4. Gayatri Nagar\n5. Benz Circle\n6. SN Puram\n";
                cin >> newLocality;
                if (newLocality >= 1 && newLocality <= 6) {
                    switch (newLocality) {
                        case 1:
                            locality = "Moghalrajpuram";
                            break;
                        case 2:
                            locality = "Bhavanipuram";
                            break;
                        case 3:
                            locality = "Patamata";
                            break;
                        case 4:
                            locality = "Gayatri Nagar";
                            break;
                        case 5:
                            locality = "Benz Circle";
                            break;
                        case 6:
                            locality = "SN Puram";
                            break;
                        default:
                            cout << "Invalid locality selection.\n";
                    }
                    cout << "Locality updated successfully to: " << locality << "\n";
                } else {
                    cout << "Invalid locality. Locality not updated.\n";
                    return;
                }
                break;
            }
            case 3: {
                string newAddress;
                cout << "Enter new address: ";
                cin.ignore();
                getline(cin, newAddress);
                address = newAddress;
                cout << "Address updated successfully to: " << address << "\n";
                break;
            }
            case 4: {
                return;
            }
            default:
                cout << "Invalid choice. No updates made.\n";
                return;
        }

        refreshCustomerDB();

        cout << "Changes saved successfully to file.\n";
    }
};

int main(){
    cout << "WELCOME CUSTOMER!" << endl;
    while(1){
        cout << "Please enter (1/2/3) to proceed:" << endl;
        cout << "1. Sign Up\n2. Login\n3. Exit\n";
        int choice;
        cin >> choice;
        if (choice==1){

            // check for unique ID
            ifstream checkCustomersDB("customer.txt");
            string id, password, name, gender, locality, address;

            cout << "Enter your ID: ";
            cin >> id;

            // Check if ID already exists
            bool idExists = false;
            string line;
            while (getline(checkCustomersDB, line)) {
                stringstream ss(line);
                string fileID;
                getline(ss, fileID, '#');
                if (fileID == id) {
                    idExists = true;
                    break;
                }
            }
            checkCustomersDB.close();

            if (idExists) {
                cout << "ERROR: ID already exists. Please try again with a different ID\n";
                continue;
            }
            cout << "Enter password: ";
            cin >> password;
            cout << "Name: ";
            cin.ignore();
            getline(cin, name);
            cout << "Gender (M/F): ";
            cin >> gender;
            if (gender != "M" && gender != "F") {
                cout << "ERROR: Invalid gender\n";
                continue;
            }
            int localityInt;
            cout  << "select your locality(1/2/3/4/5/6): " << endl;
            cout << "1. Moghalrajpuram  \n2. Bhavanipuram\n3. Patamata\n4. Gayatri Nagar\n5. Benz Circle\n6. SN puram\n";
            cin >> localityInt;
            // Use a switch statement to convert the locality number to its corresponding string
            switch (localityInt) {
                case 1:
                    locality = "Moghalrajpuram";
                    break;
                case 2:
                    locality = "Bhavanipuram";
                    break;
                case 3:
                    locality = "Patamata";
                    break;
                case 4:
                    locality = "Gayatri Nagar";
                    break;
                case 5:
                    locality = "Benz Circle";
                    break;
                case 6:
                    locality = "SN Puram";
                    break;
                default:
                    cout << "Invalid locality selection.\n";
                    continue;  // Exit if invalid input
            }

            cout << "Enter address: ";
            cin.ignore();
            getline(cin, address);

            // append to the file
            ofstream customersDB("customer.txt", ios::app);
            customersDB << id << "#" << password << "#" << name << "#" << gender << "#" << locality << "#" << address << "#" << "\n";
            customersDB.close();
            cout << "User successfully registered!\n";
            continue;
        }
        else if (choice==2){
            ifstream customersDB("customer.txt");
            string id, password;
            cout << "Enter ID: ";
            cin >> id;
            cout << "Enter password: ";
            cin >> password;
            string line;
            bool login = false;

            while (getline(customersDB, line)) {
                stringstream ss(line);
                string fileID, filePassword;
                getline(ss, fileID, '#');
                getline(ss, filePassword, '#');

                if (fileID == id && filePassword == password) {
                    login = true;
                    string name, gender, locality, address;
                    vector<int> bookingsIDs;
                    getline(ss, name, '#');
                    getline(ss, gender, '#');
                    getline(ss, locality, '#');
                    getline(ss, address, '#');

                    // Extracting history vector from file
                    string historyStr;
                    getline(ss, historyStr, '#');
                    if(!historyStr.empty()){
                        historyStr = historyStr.substr(1, historyStr.length() - 2); // Remove [ and ]
                        stringstream historyStream(historyStr);
                        string historyItem;
                        while (getline(historyStream, historyItem, ',')) {
                            bookingsIDs.push_back(stoi(historyItem));
                        }
                    }

                    Customer* customer = new Customer(id, password, name, gender, locality, address, bookingsIDs);
                    cout << "Login successful! Welcome, " << name << "!\n";
                    customersDB.close();
                    
                    int option;

                    do {
                        cout << "Choose an option:\n";
                        cout << "1. Request Service\n";
                        cout << "2. Show Current Booked Services Info\n";
                        cout << "3. Show History\n";
                        cout << "4. Show Rejected Requests\n";
                        cout << "5. Rebook Service\n";
                        cout << "6. Check Details\n";
                        cout << "7. Edit Details\n";
                        cout << "8. Logout\n";
                        cin >> option;

                        switch (option) {
                            case 1: customer->serviceRequest(); break;
                            case 2: customer->showCurrBookedServicesInfo(); break;
                            case 3: customer->showHistory(); break;
                            case 4: customer->showRejections(); break;
                            case 5: customer->rebookService(); break;
                            case 6: customer->CheckDetails(); break;
                            case 7: customer->editDetails(); break;
                            case 8: cout << "Logging out...\n"; break;
                            default: cout << "Invalid choice.\n"; break;
                        }
                    } while (option != 8);

                    delete customer; // Free the dynamically allocated customer
                }
            }
            if(!login)
                cout << "Invalid credentials." << endl;
        }

        else if (choice == 3)
            break;

        else{
            cout << "Invalid choice\n";
        }
    }
    return 0;
}