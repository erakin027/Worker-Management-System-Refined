#include <iostream>
#include <fstream>
#include <sstream>
#include <string>
#include <vector>
#include <optional>
#include <ctime>
#include <algorithm>
#include <chrono>
#include <nlohmann/json.hpp>

using json = nlohmann::json;
using namespace std;

// Helper utilities 
class Helper {
public:
    static string currentDate() {
        using namespace std::chrono;
        std::time_t t = system_clock::to_time_t(system_clock::now());
        tm tm = *localtime(&t);
        char buf[32];
        std::snprintf(buf, sizeof(buf), "%04d-%02d-%02d", tm.tm_year + 1900, tm.tm_mon + 1, tm.tm_mday);
        return string(buf);
    }
    
    static string currentTime() {
        using namespace std::chrono;
        std::time_t t = system_clock::to_time_t(system_clock::now());
        tm tm = *localtime(&t);
        char buf[9];
        std::snprintf(buf, sizeof(buf), "%02d:%02d:%02d", tm.tm_hour, tm.tm_min, tm.tm_sec);
        return string(buf);
    }
    
    static bool isValidDate(const string& d) {
        if (d.size() != 10) return false;
        if (d[4] != '-' || d[7] != '-') return false;
        for (size_t i : {0u,1u,2u,3u,5u,6u,8u,9u}) if (!isdigit(d[i])) return false;
        return true;
    }
    
    static bool isValidTime(const string& t) {
        if (t.size() != 8) return false;
        if (t[2] != ':' || t[5] != ':') return false;
        for (size_t i : {0u,1u,3u,4u,6u,7u}) if (!isdigit(t[i])) return false;
        return true;
    }
};

// Models 

class Service {
public:
    enum class Type { Immediate, Scheduling };

    int id = 0;
    int status = 0; // 0=Pending, 1=Assigned, -1=Rejected
    Type type = Type::Immediate;
    string plan;
    string bookingDate;
    string bookingTime;
    string locality;
    string customerID;
    string customerGender;
    string address;
    vector<string> requestedServices;
    string genderPref = "NP";

    optional<string> workDate;
    optional<string> workStartTime;
    optional<string> workEndTime;
    optional<string> assignedWorkerIDs;
    optional<string> reason;
    optional<double> price;

    string typeStr() const { return type == Type::Immediate ? "Immediate" : "Scheduling"; }
};

inline void to_json(json& j, const Service::Type& t) {
    j = (t == Service::Type::Immediate ? "Immediate" : "Scheduling");
}

inline void from_json(const json& j, Service::Type& t) {
    string s = j.get<string>();
    if (s == "Immediate") t = Service::Type::Immediate;
    else t = Service::Type::Scheduling;
}

inline void to_json(json& j, const Service& s) {
    j = json{
        {"id", s.id},
        {"status", s.status},
        {"type", s.type},
        {"plan", s.plan},
        {"bookingDate", s.bookingDate},
        {"bookingTime", s.bookingTime},
        {"locality", s.locality},
        {"customerID", s.customerID},
        {"customerGender", s.customerGender},
        {"address", s.address},
        {"requestedServices", s.requestedServices},
        {"genderPref", s.genderPref}
    };
    if (s.workDate) j["workDate"] = *s.workDate;
    if (s.workStartTime) j["workStartTime"] = *s.workStartTime;
    if (s.workEndTime) j["workEndTime"] = *s.workEndTime;
    if (s.assignedWorkerIDs) j["assignedWorkerIDs"] = *s.assignedWorkerIDs;
    if (s.reason) j["reason"] = *s.reason;
    if (s.price) j["price"] = *s.price;
}

inline void from_json(const json& j, Service& s) {
    j.at("id").get_to(s.id);
    j.at("status").get_to(s.status);
    j.at("type").get_to(s.type);
    j.at("plan").get_to(s.plan);
    j.at("bookingDate").get_to(s.bookingDate);
    j.at("bookingTime").get_to(s.bookingTime);
    j.at("locality").get_to(s.locality);
    j.at("customerID").get_to(s.customerID);
    j.at("customerGender").get_to(s.customerGender);
    j.at("address").get_to(s.address);
    j.at("requestedServices").get_to(s.requestedServices);
    j.at("genderPref").get_to(s.genderPref);
    if (j.contains("workDate")) s.workDate = j.at("workDate").get<string>();
    if (j.contains("workStartTime")) s.workStartTime = j.at("workStartTime").get<string>();
    if (j.contains("workEndTime")) s.workEndTime = j.at("workEndTime").get<string>();
    if (j.contains("assignedWorkerIDs")) s.assignedWorkerIDs = j.at("assignedWorkerIDs").get<string>();
    if (j.contains("reason")) s.reason = j.at("reason").get<string>();
    if (j.contains("price")) s.price = j.at("price").get<double>();
}

class Customer {
public:
    string id;
    string password;
    string name;
    string gender;
    string locality;
    string address;
    vector<int> bookingsIDs;
};

inline void to_json(json& j, const Customer& c) {
    j = json{
        {"id", c.id},
        {"password", c.password},
        {"name", c.name},
        {"gender", c.gender},
        {"locality", c.locality},
        {"address", c.address},
        {"bookingsIDs", c.bookingsIDs}
    };
}

inline void from_json(const json& j, Customer& c) {
    j.at("id").get_to(c.id);
    j.at("password").get_to(c.password);
    j.at("name").get_to(c.name);
    j.at("gender").get_to(c.gender);
    j.at("locality").get_to(c.locality);
    j.at("address").get_to(c.address);
    if (j.contains("bookingsIDs")) j.at("bookingsIDs").get_to(c.bookingsIDs);
}

class Payment {
public:
    int serviceID;
    double amountDue;
    bool paid = false;
};

inline void to_json(json& j, const Payment& p) {
    j = json{{"serviceID", p.serviceID}, {"amountDue", p.amountDue}, {"paid", p.paid}};
}

inline void from_json(const json& j, Payment& p) {
    j.at("serviceID").get_to(p.serviceID);
    j.at("amountDue").get_to(p.amountDue);
    j.at("paid").get_to(p.paid);
}

// Repositories (interfaces) 

class ICustomerRepository {
public:
    virtual ~ICustomerRepository() = default;
    virtual optional<Customer> findById(const string& id) = 0;
    virtual bool save(const Customer& c) = 0;
    virtual bool exists(const string& id) = 0;
};

class IServiceRepository {
public:
    virtual ~IServiceRepository() = default;
    virtual int nextId() = 0;
    virtual bool save(const Service& s) = 0;
    virtual vector<Service> findByCustomer(const string& customerId) = 0;
};

class IPaymentRepository {
public:
    virtual ~IPaymentRepository() = default;
    virtual bool save(const Payment& p) = 0;
    virtual optional<Payment> findByService(int serviceID) = 0;
};

// JSON helpers & repos 

class JsonFileHandler {
public:
    static json loadJsonArrayFile(const string& path) {
        ifstream in(path);
        if (!in) return json::array();
        try { 
            json j; 
            in >> j; 
            if (!j.is_array()) return json::array(); 
            return j; 
        }
        catch(...) { 
            return json::array(); 
        }
    }
    
    static void writeJsonArrayFile(const string& path, const json& j) {
        ofstream out(path);
        out << j.dump(4) << endl;
    }
    
    static void ensureFileExists(const string& path) {
        ifstream in(path); 
        if (!in) { 
            ofstream out(path); 
            out << "[]"; 
        }
    }
};

class JsonCustomerRepository : public ICustomerRepository {
private:
    string path_;
    
public:
    JsonCustomerRepository(const string& path = "customers.json") : path_(path) {
        JsonFileHandler::ensureFileExists(path_);
    }
    
    optional<Customer> findById(const string& id) override {
        json j = JsonFileHandler::loadJsonArrayFile(path_);
        for (auto &it : j) {
            if (it.value("id", string("")) == id) 
                return it.get<Customer>();
        }
        return nullopt;
    }
    
    bool save(const Customer& c) override {
        json j = JsonFileHandler::loadJsonArrayFile(path_);
        bool updated = false;
        for (auto &it : j) {
            if (it.value("id", string("")) == c.id) { 
                it = json(c); 
                updated = true; 
                break; 
            }
        }
        if (!updated) j.push_back(json(c));
        JsonFileHandler::writeJsonArrayFile(path_, j); 
        return true;
    }
    
    bool exists(const string& id) override { 
        return (bool)findById(id); 
    }
};

class JsonServiceRepository : public IServiceRepository {
private:
    string path_;
    
public:
    JsonServiceRepository(const string& path = "services.json") : path_(path) {
        JsonFileHandler::ensureFileExists(path_);
    }
    
    int nextId() override {
        json j = JsonFileHandler::loadJsonArrayFile(path_);
        int maxid = 0;
        for (auto &it : j) {
            if (it.contains("id")) 
                maxid = max(maxid, it["id"].get<int>());
        }
        return maxid + 1;
    }
    
    bool save(const Service& s) override {
        json j = JsonFileHandler::loadJsonArrayFile(path_);
        bool updated = false;
        for (auto &it : j) {
            if (it.contains("id") && it["id"].get<int>() == s.id) { 
                it = json(s); 
                updated = true; 
                break; 
            }
        }
        if (!updated) j.push_back(json(s));
        JsonFileHandler::writeJsonArrayFile(path_, j); 
        return true;
    }
    
    vector<Service> findByCustomer(const string& customerId) override {
        json j = JsonFileHandler::loadJsonArrayFile(path_);
        vector<Service> out;
        for (auto &it : j) {
            if (it.value("customerID", string("")) == customerId) 
                out.push_back(it.get<Service>());
        }
        return out;
    }
};

class JsonPaymentRepository : public IPaymentRepository {
private:
    string path_;
    
public:
    JsonPaymentRepository(const string& path = "payments.json") : path_(path) {
        JsonFileHandler::ensureFileExists(path_);
    }
    
    bool save(const Payment& p) override {
        json arr = JsonFileHandler::loadJsonArrayFile(path_);
        bool updated = false;
        for (auto &it : arr) {
            if (it.value("serviceID", -1) == p.serviceID) { 
                it = json(p); 
                updated = true; 
                break; 
            }
        }
        if (!updated) arr.push_back(json(p));
        JsonFileHandler::writeJsonArrayFile(path_, arr); 
        return true;
    }
    
    optional<Payment> findByService(int serviceID) override {
        json arr = JsonFileHandler::loadJsonArrayFile(path_);
        for (auto &it : arr) {
            if (it.value("serviceID", -1) == serviceID) 
                return it.get<Payment>();
        }
        return nullopt;
    }
};

// Payment Strategy (Open/Closed Principle) 

class IPricingStrategy {
public:
    virtual ~IPricingStrategy() = default;
    virtual double calculatePrice(size_t serviceCount) const = 0;
};

class BasicPricing : public IPricingStrategy {
public:
    double calculatePrice(size_t serviceCount) const override {
        return 1000 + (500.0 * serviceCount);
    }
};

class IntermediatePricing : public IPricingStrategy {
public:
    double calculatePrice(size_t serviceCount) const override {
        return (2000 + (400.0 * serviceCount)) * 0.90;
    }
};

class PremiumPricing : public IPricingStrategy {
public:
    double calculatePrice(size_t serviceCount) const override {
        return (3000 + (300.0 * serviceCount)) * 0.80;
    }
};

// Services / Use-cases 

class BookingService {
private:
    IServiceRepository& serviceRepo;
    
public:
    BookingService(IServiceRepository& repo) : serviceRepo(repo) {}
    
    Service createImmediate(const string& plan, const string& locality, const string& customerID,
                            const string& customerGender, const string& address,
                            const vector<string>& requestedServices, const string& genderPref)
    {
        Service s;
        s.id = serviceRepo.nextId();
        s.status = 0;
        s.type = Service::Type::Immediate;
        s.plan = plan;
        s.bookingDate = Helper::currentDate();
        s.bookingTime = Helper::currentTime();
        s.locality = locality;
        s.customerID = customerID;
        s.customerGender = customerGender;
        s.address = address;
        s.requestedServices = requestedServices;
        s.genderPref = genderPref;
        serviceRepo.save(s);
        return s;
    }
    
    Service createScheduling(const string& plan, const string& locality, const string& customerID,
                             const string& customerGender, const string& address,
                             const vector<string>& requestedServices, const string& genderPref,
                             const string& scheduledDate, const string& scheduledTime)
    {
        Service s;
        s.id = serviceRepo.nextId();
        s.status = 0;
        s.type = Service::Type::Scheduling;
        s.plan = plan;
        s.bookingDate = Helper::currentDate();
        s.bookingTime = Helper::currentTime();
        s.locality = locality;
        s.customerID = customerID;
        s.customerGender = customerGender;
        s.address = address;
        s.requestedServices = requestedServices;
        s.genderPref = genderPref;
        s.workDate = scheduledDate;
        s.workStartTime = scheduledTime;
        serviceRepo.save(s);
        return s;
    }
};

class CustomerService {
private:
    ICustomerRepository& custRepo;
    IServiceRepository& servRepo;
    
public:
    CustomerService(ICustomerRepository& c, IServiceRepository& s) : custRepo(c), servRepo(s) {}
    
    bool registerCustomer(const Customer& c) {
        if (custRepo.exists(c.id)) return false;
        custRepo.save(c); 
        return true;
    }

    bool idExists(const string& id) {
        return custRepo.exists(id);
    }

    bool updateCustomer(const Customer& c) {
        return custRepo.save(c);
    }
    
    optional<Customer> authenticate(const string& id, const string& password) {
        auto oc = custRepo.findById(id);
        if (!oc) return nullopt;
        if (oc->password != password) return nullopt;
        return oc;
    }
    
    vector<Service> viewCustomerBookings(const string& customerID) { 
        return servRepo.findByCustomer(customerID); 
    }
    
    bool addBookingToCustomer(Customer& c, int serviceId) { 
        c.bookingsIDs.push_back(serviceId); 
        return custRepo.save(c); 
    }
};

class PaymentService {
private:
    IPaymentRepository& payRepo;
    
    double calculateBillByPlan(const string& plan, size_t count) {
        if (plan == "Basic") {
            BasicPricing pricing;
            return pricing.calculatePrice(count);
        } else if (plan == "Intermediate") {
            IntermediatePricing pricing;
            return pricing.calculatePrice(count);
        } else if (plan == "Premium") {
            PremiumPricing pricing;
            return pricing.calculatePrice(count);
        }
        return 1500 + (300.0 * count); // Default fallback
    }
    
public:
    PaymentService(IPaymentRepository& repo) : payRepo(repo) {}
    
    double calculateBill(const Service& s) {
        return calculateBillByPlan(s.plan, s.requestedServices.size());
    }
    
    Payment generatePayment(const Service& s) {
        Payment p; 
        p.serviceID = s.id; 
        p.amountDue = calculateBill(s); 
        p.paid = false; 
        payRepo.save(p); 
        return p;
    }
    
    bool processPayment(int serviceID, double amount) {
        auto op = payRepo.findByService(serviceID); 
        if (!op) return false;
        Payment p = *op; 
        
        // Exact amount required
        if (amount == p.amountDue) { 
            p.paid = true; 
            payRepo.save(p); 
            return true; 
        } 
        return false;
    }
    
    optional<Payment> getPayment(int serviceID) { 
        return payRepo.findByService(serviceID); 
    }
};

// UI Components (SRP) 

class MenuDisplay {
public:
    static void showMainMenu() {
        cout << "\n=== WORKER MANAGEMENT SYSTEM - CUSTOMER ===\n";
        cout << "1) Sign Up\n";
        cout << "2) Login\n";
        cout << "3) Exit\n";
        cout << "Choose: ";
    }
    
    static void showCustomerMenu() {
        cout << "\n=== CUSTOMER DASHBOARD ===\n";
        cout << "1. Request Service\n";
        cout << "2. Show Current Booked Services\n";
        cout << "3. Show History (Completed)\n";
        cout << "4. Show Rejected Requests\n";
        cout << "5. Rebook from History\n";
        cout << "6. Check Profile Details\n";
        cout << "7. Edit Profile Details\n";
        cout << "8. Logout\n";
        cout << "Choice: ";
    }
    
    static void showPaymentMethods() {
        cout << "\n=== SELECT PAYMENT METHOD ===\n";
        cout << "1) UPI\n";
        cout << "2) Credit/Debit Card\n";
        cout << "3) Net Banking\n";
        cout << "4) Cancel Service\n";
        cout << "Choose: ";
    }
};

class InputHandler {
public:
    static int getIntInput() {
        int choice = 0;
        cin >> choice;
        return choice;
    }
    
    static double getDoubleInput() {
        double value = 0.0;
        cin >> value;
        return value;
    }
    
    static string getStringInput() {
        string input;
        cin >> input;
        return input;
    }
    
    static string getLineInput() {
        string input;
        getline(cin, input);
        return input;
    }
    
    static string selectGender() {
        cout << "Select Gender:\n1) M\n2) F\nChoice: ";
        int gopt = getIntInput();
        return (gopt == 1 ? "M" : (gopt == 2 ? "F" : ""));
    }
    
    static string selectLocality() {
        cout << "Select locality:\n";
        cout << "1) Moghalrajpuram\n2) Bhavanipuram\n3) Patamata\n";
        cout << "4) Gayatri Nagar\n5) Benz Circle\n6) SN Puram\n";
        cout << "Choice: ";
        int loc = getIntInput();
        switch (loc) {
            case 1: return "Moghalrajpuram";
            case 2: return "Bhavanipuram";
            case 3: return "Patamata";
            case 4: return "Gayatri Nagar";
            case 5: return "Benz Circle";
            case 6: return "SN Puram";
            default: return "";
        }
    }
    
    static string selectGenderPreference() {
        cout << "Gender preference?\n0) No Preference\n1) Male\n2) Female\nChoice: ";
        int gp = getIntInput();
        return (gp == 1 ? "M" : (gp == 2 ? "F" : "NP"));
    }
};

class ServiceDisplay {
public:
    static void printServiceSummary(const Service& s) {
        cout << "\n--- Service ID: " << s.id << " ---\n";
        cout << "Status: " << (s.status == 0 ? "Pending" : (s.status == 1 ? "Completed" : "Rejected")) << "\n";
        cout << "Type: " << s.typeStr() << " | Plan: " << s.plan << "\n";
        cout << "Booking: " << s.bookingDate << " " << s.bookingTime << "\n";
        cout << "Requested Services: ";
        for (auto &rs : s.requestedServices) cout << rs << "; ";
        cout << "\n";
        if (s.workDate) cout << "Work Date: " << *s.workDate;
        if (s.workStartTime) cout << " Start: " << *s.workStartTime;
        if (s.workDate || s.workStartTime) cout << "\n";
        if (s.reason) cout << "Rejection Reason: " << *s.reason << "\n";
    }
    
    static void printPaymentInfo(const optional<Payment>& p) {
        if (p) {
            cout << "Payment Status: " << (p->paid ? "PAID" : "UNPAID") << "\n";
            cout << "Amount: ₹" << p->amountDue << "\n";
        }
    }
};

class PaymentHandler {
private:
    PaymentService& paymentService;
    IServiceRepository& serviceRepo;
    
    bool attemptPayment(int serviceID, double amountDue) {
        cout << "\nEnter exact amount to pay (₹" << amountDue << "): ";
        double amt = InputHandler::getDoubleInput();
        
        if (paymentService.processPayment(serviceID, amt)) {
            cout << "\nPayment Successful!\n";
            cout << "Service request submitted and payment completed.\n";
            return true;
        } else {
            cout << "\nPayment FAILED! Amount must be exactly ₹" << amountDue << "\n";
            return false;
        }
    }
    
public:
    PaymentHandler(PaymentService& ps, IServiceRepository& sr) 
        : paymentService(ps), serviceRepo(sr) {}
    
    bool handlePaymentFlow(const Service& service) {
        auto payment = paymentService.generatePayment(service);
        cout << "\n=== PAYMENT REQUIRED ===\n";
        cout << "Total Bill: ₹" << payment.amountDue << "\n";
        
        while (true) {
            MenuDisplay::showPaymentMethods();
            int payMethod = InputHandler::getIntInput();
            
            if (payMethod == 4) {
                // Cancel service
                cout << "\nService booking cancelled. Payment not processed.\n";
                // Optionally delete the service from repository
                return false;
            }
            
            if (payMethod >= 1 && payMethod <= 3) {
                string methodName = (payMethod == 1 ? "UPI" : 
                                   (payMethod == 2 ? "Card" : "Net Banking"));
                cout << "\nProcessing via " << methodName << "...\n";
                
                if (attemptPayment(service.id, payment.amountDue)) {
                    return true;
                }
                // Loop back to payment methods if failed
            } else {
                cout << "Invalid payment method!\n";
            }
        }
    }
};

class Work {
public:
    int id;
    string name;
    string category;
};

inline void from_json(const json& j, Work& w) {
    j.at("id").get_to(w.id);
    j.at("name").get_to(w.name);
    j.at("category").get_to(w.category);
}

class Package {
public:
    int id;
    string name;
    string description;
    vector<int> workIds;
};

inline void from_json(const json& j, Package& p) {
    j.at("id").get_to(p.id);
    j.at("name").get_to(p.name);
    j.at("description").get_to(p.description);
    j.at("workIds").get_to(p.workIds);
}


class WorkConfigurationRepository {
private:
    string configPath_;
    vector<Work> works_;
    vector<Package> packages_;
    
    void loadConfiguration() {
        ifstream in(configPath_);
        if (!in) {
            cerr << "Warning: Configuration file not found. Using defaults.\n";
            loadDefaults();
            return;
        }
        
        try {
            json config;
            in >> config;
            
            if (config.contains("works")) {
                works_ = config["works"].get<vector<Work>>();
            }
            if (config.contains("packages")) {
                packages_ = config["packages"].get<vector<Package>>();
            }
        } catch (const exception& e) {
            cerr << "Error loading config: " << e.what() << "\n";
            loadDefaults();
        }
    }
    
    void loadDefaults() {
        // Fallback to hardcoded defaults if config fails
        works_ = {
            {1, "Window Cleaning", "house"},
            {2, "Mopping", "house"},
            {3, "Sweeping", "house"},
            {4, "Fan Cleaning", "house"},
            {5, "Bathroom Cleaning", "house"},
            {6, "Mowing", "garden"},
            {7, "Pruning", "garden"},
            {8, "Washing", "laundry"},
            {9, "Drying", "laundry"},
            {10, "Ironing", "laundry"}
        };
        
        packages_ = {
            {1, "House Cleaning", "Complete house cleaning", {1,2,3,4,5}},
            {2, "Garden Cleaning", "Garden maintenance", {6,7}},
            {3, "Laundry", "Complete laundry services", {8,9,10}}
        };
    }
    
public:
    WorkConfigurationRepository(const string& path = "works_config.json") 
        : configPath_(path) {
        loadConfiguration();
    }
    
    const vector<Work>& getWorks() const { return works_; }
    const vector<Package>& getPackages() const { return packages_; }
    
    optional<Work> getWorkById(int id) const {
        auto it = find_if(works_.begin(), works_.end(), 
                         [id](const Work& w) { return w.id == id; });
        return it != works_.end() ? optional<Work>(*it) : nullopt;
    }
    
    vector<string> getWorkNamesByIds(const vector<int>& ids) const {
        vector<string> names;
        for (int id : ids) {
            auto work = getWorkById(id);
            if (work) names.push_back(work->name);
        }
        return names;
    }
};

class ServiceRequestHandler {
private:
    BookingService& bookingService;
    CustomerService& customerService;
    PaymentHandler& paymentHandler;
    WorkConfigurationRepository& workConfig; 
    
    vector<string> selectServices(int planID) {
        vector<string> requested;
        const auto& works = workConfig.getWorks();
        const auto& packages = workConfig.getPackages();
        
        if (planID == 1 || planID == 2) {
            int selectionCount = (planID == 1 ? 1 : 3);
            cout << "\nSelect " << selectionCount << " service(s):\n";
            
            for (size_t i = 0; i < works.size(); ++i) {
                cout << works[i].id << ") " << works[i].name << "\n";
            }
            cout << "Enter " << selectionCount << " IDs: ";
            
            vector<int> picks;
            for (int i = 0; i < selectionCount; ++i) {
                int p = InputHandler::getIntInput();
                auto work = workConfig.getWorkById(p);
                if (!work) {
                    cout << "Invalid selection!\n";
                    return {};
                }
                if (find(picks.begin(), picks.end(), p) == picks.end()) {
                    picks.push_back(p);
                    requested.push_back(work->name);
                } else {
                    cout << "Duplicate selection!\n";
                    return {};
                }
            }
            
        } else { // Premium
            cout << "\nPremium Plan:\n1) Select 1 Package\n2) Select 5 Individual Services\nChoice: ";
            int pkgOrSvc = InputHandler::getIntInput();
            
            if (pkgOrSvc == 1) {
                cout << "\nSelect package:\n";
                for (size_t i = 0; i < packages.size(); ++i) {
                    cout << packages[i].id << ") " << packages[i].name 
                         << " - " << packages[i].description << "\n";
                }
                cout << "Choice: ";
                int pc = InputHandler::getIntInput();
                
                if (pc < 1 || pc > (int)packages.size()) {
                    cout << "Invalid package!\n";
                    return {};
                }
                
                requested = workConfig.getWorkNamesByIds(packages[pc-1].workIds);
                
            } else if (pkgOrSvc == 2) {
                cout << "\nSelect 5 services:\n";
                for (size_t i = 0; i < works.size(); ++i) {
                    cout << works[i].id << ") " << works[i].name << "\n";
                }
                cout << "Enter 5 IDs: ";
                
                vector<int> picks;
                for (int i = 0; i < 5; ++i) {
                    int p = InputHandler::getIntInput();
                    auto work = workConfig.getWorkById(p);
                    if (!work) {
                        cout << "Invalid selection!\n";
                        return {};
                    }
                    if (find(picks.begin(), picks.end(), p) == picks.end()) {
                        picks.push_back(p);
                        requested.push_back(work->name);
                    } else {
                        cout << "Duplicate selection!\n";
                        return {};
                    }
                }
            } else {
                cout << "Invalid choice!\n";
                return {};
            }
        }
        
        return requested;
    }
    
public:
    ServiceRequestHandler(BookingService& bs, CustomerService& cs, 
                         PaymentHandler& ph, WorkConfigurationRepository& wc)
        : bookingService(bs), customerService(cs), paymentHandler(ph), workConfig(wc) {}
    
    void handleServiceRequest(Customer& customer) {
        // Service type
        cout << "\nService Type:\n1) Immediate\n2) Scheduling\nChoice: ";
        int typeID = InputHandler::getIntInput();
        if (typeID < 1 || typeID > 2) {
            cout << "Invalid type!\n";
            return;
        }
        
        // Plan selection
        cout << "\nSelect Plan:\n1) Basic (1 service)\n2) Intermediate (3 services)\n3) Premium (5 services or 1 package)\nChoice: ";
        int planID = InputHandler::getIntInput();
        if (planID < 1 || planID > 3) {
            cout << "Invalid plan!\n";
            return;
        }
        string planStr = (planID == 1 ? "Basic" : (planID == 2 ? "Intermediate" : "Premium"));
        
        // Service selection
        vector<string> requested = selectServices(planID);
        if (requested.empty()) {
            cout << "Service selection failed!\n";
            return;
        }
        
        // Gender preference
        string gpStr = InputHandler::selectGenderPreference();
        
        // Create service based on type
        Service s;
        if (typeID == 1) { // Immediate
            s = bookingService.createImmediate(planStr, customer.locality, customer.id, 
                                              customer.gender, customer.address, requested, gpStr);
        } else { // Scheduling
            string date, time;
            cout << "Enter scheduled date (YYYY-MM-DD): ";
            date = InputHandler::getStringInput();
            if (!Helper::isValidDate(date)) {
                cout << "Invalid date format!\n";
                return;
            }
            cout << "Enter scheduled time (HH:MM:SS): ";
            time = InputHandler::getStringInput();
            if (!Helper::isValidTime(time)) {
                cout << "Invalid time format!\n";
                return;
            }
            
            // Basic future check
            string curD = Helper::currentDate();
            string curT = Helper::currentTime();
            if (date < curD || (date == curD && time <= curT)) {
                cout << "Scheduled time must be in the future!\n";
                return;
            }
            
            s = bookingService.createScheduling(planStr, customer.locality, customer.id, 
                                               customer.gender, customer.address, requested, 
                                               gpStr, date, time);
        }
        
        // Handle payment
        if (paymentHandler.handlePaymentFlow(s)) {
            customerService.addBookingToCustomer(customer, s.id);
        }
    }
};

class CustomerController {
private:
    CustomerService& customerService;
    BookingService& bookingService;
    PaymentService& paymentService;
    IServiceRepository& serviceRepo;
    WorkConfigurationRepository& workConfig; 
    
public:
    CustomerController(CustomerService& cs, BookingService& bs, PaymentService& ps, 
                      IServiceRepository& sr, WorkConfigurationRepository& wc)
        : customerService(cs), bookingService(bs), paymentService(ps), 
          serviceRepo(sr), workConfig(wc) {} 
    
    void handleSignUp() {
        Customer c;
        cout << "\n=== SIGN UP ===\n";

        string id;
        while (true) {
            cout << "Enter ID: ";
            id = InputHandler::getStringInput();

            if (customerService.idExists(id)) { 
                cout << "ID already exists! Try another.\n";
            } else {
                break;
            }
        }

        c.id = id;
        
        cout << "Password: ";
        c.password = InputHandler::getStringInput();
        cin.ignore();
        
        cout << "Enter Name: ";
        c.name = InputHandler::getLineInput();
        
        c.gender = InputHandler::selectGender();
        if (c.gender.empty()) {
            cout << "Invalid gender selection!\n";
            return;
        }
        
        c.locality = InputHandler::selectLocality();
        if (c.locality.empty()) {
            cout << "Invalid locality selection!\n";
            return;
        }
        cin.ignore(); 
        
        cout << "Enter Address: ";
        c.address = InputHandler::getLineInput();
        
        if (customerService.registerCustomer(c)) {
            cout << "\nRegistration successful!\n";
        } else {
            cout << "\nRegistration failed!\n";
        }
    }
    
    void handleCustomerSession() {
        string id, pass;
        cout << "\n=== LOGIN ===\n";
        cout << "ID: ";
        id = InputHandler::getStringInput();
        cout << "Password: ";
        pass = InputHandler::getStringInput();
        
        auto oc = customerService.authenticate(id, pass);
        if (!oc) {
            cout << "\nInvalid credentials!\n";
            return;
        }
        
        Customer customer = *oc;
        cout << "\nWelcome, " << customer.name << "!\n";
        
        PaymentHandler paymentHandler(paymentService, serviceRepo);
        ServiceRequestHandler requestHandler(bookingService, customerService, 
                                            paymentHandler, workConfig);
        
        
        int opt = 0;
        while (opt != 8) {
            MenuDisplay::showCustomerMenu();
            opt = InputHandler::getIntInput();
            
            switch (opt) {
                case 1: // Request Service
                    requestHandler.handleServiceRequest(customer);
                    // Refresh customer data
                    oc = customerService.authenticate(customer.id, customer.password);
                    if (oc) customer = *oc;
                    break;
                    
                case 2: { // Show Current Bookings
                    auto bookings = customerService.viewCustomerBookings(customer.id);
                    cout << "\n=== CURRENT BOOKINGS ===\n";
                    bool found = false;
                    for (auto &b : bookings) {
                        if (b.status == 0 || b.status == 1) {
                            ServiceDisplay::printServiceSummary(b);
                            ServiceDisplay::printPaymentInfo(paymentService.getPayment(b.id));
                            found = true;
                        }
                    }
                    if (!found) cout << "No current bookings.\n";
                    break;
                }
                
                case 3: { // Show History
                    auto bookings = customerService.viewCustomerBookings(customer.id);
                    cout << "\n=== SERVICE HISTORY (COMPLETED) ===\n";
                    bool found = false;
                    for (auto &b : bookings) {
                        if (b.status == 1) {
                            ServiceDisplay::printServiceSummary(b);
                            ServiceDisplay::printPaymentInfo(paymentService.getPayment(b.id));
                            found = true;
                        }
                    }
                    if (!found) cout << "No completed services.\n";
                    break;
                }
                
                case 4: { // Show Rejected
                    auto bookings = customerService.viewCustomerBookings(customer.id);
                    cout << "\n=== REJECTED SERVICES ===\n";
                    bool found = false;
                    for (auto &b : bookings) {
                        if (b.status == -1) {
                            ServiceDisplay::printServiceSummary(b);
                            ServiceDisplay::printPaymentInfo(paymentService.getPayment(b.id));
                            found = true;
                        }
                    }
                    if (!found) cout << "No rejected services.\n";
                    break;
                }
                
                case 5: { // Rebook from History
                    auto bookings = customerService.viewCustomerBookings(customer.id);
                    vector<Service> completed;
                    for (auto &b : bookings) {
                        if (b.status == 1) completed.push_back(b);
                    }
                    
                    if (completed.empty()) {
                        cout << "\nNo completed services to rebook.\n";
                        break;
                    }
                    
                    cout << "\n=== COMPLETED SERVICES ===\n";
                    for (auto &b : completed) {
                        cout << "ID: " << b.id << " | " << b.plan << " | " << b.bookingDate << "\n";
                    }
                    
                    cout << "\nEnter Service ID to rebook: ";
                    int rid = InputHandler::getIntInput();
                    
                    auto it = find_if(completed.begin(), completed.end(), 
                                     [&](const Service& s){ return s.id == rid; });
                    if (it == completed.end()) {
                        cout << "Service ID not found!\n";
                        break;
                    }
                    
                    cout << "\nRebook as:\n1) Immediate\n2) Scheduling\nChoice: ";
                    int rt = InputHandler::getIntInput();
                    
                    Service newService;
                    if (rt == 1) {
                        newService = bookingService.createImmediate(it->plan, customer.locality, customer.id, 
                                                                    customer.gender, customer.address, 
                                                                    it->requestedServices, it->genderPref);
                    } else if (rt == 2) {
                        string d, t;
                        string curD = Helper::currentDate();
                        string curT = Helper::currentTime();
                        cout << "Date (YYYY-MM-DD): ";
                        d = InputHandler::getStringInput();
                        if (!Helper::isValidDate(d) || d < curD) {
                            cout << "Invalid date!\n";
                            break;
                        }
                        cout << "Time (HH:MM:SS): ";
                        t = InputHandler::getStringInput();
                        if (!Helper::isValidTime(t)|| (d == curD && t <= curT)) {
                            cout << "Invalid time!\n";
                            break;
                        }
                        newService = bookingService.createScheduling(it->plan, customer.locality, customer.id, 
                                                                     customer.gender, customer.address, 
                                                                     it->requestedServices, it->genderPref, d, t);
                    } else {
                        cout << "Invalid choice!\n";
                        break;
                    }
                    
                    PaymentHandler rebookPaymentHandler(paymentService, serviceRepo);
                    if (rebookPaymentHandler.handlePaymentFlow(newService)) {
                        customerService.addBookingToCustomer(customer, newService.id);
                    }
                    
                    // Refresh customer data
                    oc = customerService.authenticate(customer.id, customer.password);
                    if (oc) customer = *oc;
                    break;
                }
                
                case 6: // Check Profile
                    cout << "\n=== PROFILE DETAILS ===\n";
                    cout << "ID: " << customer.id << "\n";
                    cout << "Name: " << customer.name << "\n";
                    cout << "Gender: " << customer.gender << "\n";
                    cout << "Locality: " << customer.locality << "\n";
                    cout << "Address: " << customer.address << "\n";
                    break;
                    
                case 7: { // Edit Profile
                    cout << "\n=== EDIT PROFILE ===\n";
                    cout << "1) Password\n2) Locality\n3) Address\nChoice: ";
                    int e = InputHandler::getIntInput();
                    
                    if (e == 1) {
                        string oldp, newp;
                        cout << "Old Password: ";
                        oldp = InputHandler::getStringInput();
                        if (oldp != customer.password) {
                            cout << "Incorrect old password!\n";
                            break;
                        }
                        cout << "New Password: ";
                        newp = InputHandler::getStringInput();
                        customer.password = newp;
                        customerService.updateCustomer(customer);  
                        cout << "Password updated!\n";
                    } else if (e == 2) {
                        string newLoc = InputHandler::selectLocality();
                        if (!newLoc.empty()) {
                            customer.locality = newLoc;
                            customerService.updateCustomer(customer);  
                            cout << "Locality updated!\n";
                        } else {
                            cout << "Invalid locality!\n";
                        }
                    } else if (e == 3) {
                        cin.ignore();
                        cout << "New Address: ";
                        string a = InputHandler::getLineInput();
                        customer.address = a;
                        customerService.updateCustomer(customer);  
                        cout << "Address updated!\n";
                    } else {
                        cout << "Invalid choice!\n";
                    }
                    break;
                }
                
                case 8:
                    cout << "\nLogging out...\n";
                    break;
                    
                default:
                    cout << "Invalid option!\n";
            }
        }
    }
};

// Main Application 
int main() {
    // Initialize repositories
    JsonCustomerRepository custRepo("customers.json");
    JsonServiceRepository servRepo("services.json");
    JsonPaymentRepository paymentRepo("payments.json");
    WorkConfigurationRepository workConfig("works_config.json"); // NEW
    
    // Initialize services
    CustomerService customerService(custRepo, servRepo);
    BookingService bookingService(servRepo);
    PaymentService paymentService(paymentRepo);
    
    // Initialize controller
    CustomerController controller(customerService, bookingService, 
                                 paymentService, servRepo, workConfig);
    

    cout << "WORKER MANAGEMENT SYSTEM - CUSTOMER\n";

    
    while (true) {
        MenuDisplay::showMainMenu();
        int choice = InputHandler::getIntInput();
        
        switch (choice) {
            case 1:
                controller.handleSignUp();
                break;
                
            case 2:
                controller.handleCustomerSession();
                break;
                
            case 3:
                cout << "\nThank you for using our service. Goodbye!\n";
                return 0;
                
            default:
                cout << "Invalid option! Please try again.\n";
        }
    }
    
    return 0;
}