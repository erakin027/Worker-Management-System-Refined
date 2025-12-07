#pragma once

#include <fstream>
#include "entities.h"


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
        {1, "Window Cleaning", "house", 80, 600},
        {2, "Mopping", "house", 40, 300},
        {3, "Sweeping", "house", 30, 200},
        {4, "Fan Cleaning", "house", 40, 400},
        {5, "Bathroom Cleaning", "house", 60, 500},
        {6, "Mowing", "garden", 80, 700},
        {7, "Pruning", "garden", 120, 900},
        {8, "Washing", "laundry", 40, 300},
        {9, "Drying", "laundry", 30, 200},
        {10, "Ironing", "laundry", 40, 200}
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

    vector<int> getIdsByNames(const vector<string>& names) const {
        vector<int> ids;
        for (auto &nm : names) {
            for (auto &w : works_) {
                if (w.name == nm) {
                    ids.push_back(w.id);
                    break;
                }
            }
        }
        return ids;
    }

    double getTotalPriceByIds(const vector<int>& ids) const {
        double total = 0.0;
        for (int id : ids) {
            auto work = getWorkById(id);
            if (work) total += work->price;
        }
        return total;
    }

    // this will be taken care of by the java worker/admin side

    // int getTotalTimeByIds(const vector<int>& ids) const {
    //     int total = 0;
    //     for (int id : ids) {
    //         auto work = getWorkById(id);
    //         if (work) total += work->timeMinutes;
    //     }
    //     return total;
    // }
};