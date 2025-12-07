#pragma once

#include "helper.h"
#include "pricing.h"


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
    
};

class PaymentService {
private:
    IPaymentRepository& payRepo;
    
public:
    PaymentService(IPaymentRepository& repo) : payRepo(repo) {}
    const WorkConfigurationRepository* workConfig;

    void attachWorkConfig(const WorkConfigurationRepository& wc) {
        workConfig = &wc;
    }

    double calculateBill(const Service& s, const vector<int>& workIds, const WorkConfigurationRepository& workConfig) {
        if (s.plan == "Basic") {
            BasicPricing pricing(workConfig);
            return pricing.calculatePrice(workIds);
        }
        if (s.plan == "Intermediate") {
            IntermediatePricing pricing(workConfig);
            return pricing.calculatePrice(workIds);
        }
        if (s.plan == "Premium") {
            PremiumPricing pricing(workConfig);
            return pricing.calculatePrice(workIds);
        }
        return workConfig.getTotalPriceByIds(workIds); // Fallback
    }

    
Payment generatePayment(const Service& s, const vector<int>& workIds){
        Payment p; 
        p.serviceID = s.id; 
        p.amountDue = calculateBill(s, workIds, *workConfig);
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
