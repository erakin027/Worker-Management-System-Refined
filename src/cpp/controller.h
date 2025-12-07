#pragma once

#include "ui.h"


class ServiceRequestHandler {
private:
    BookingService& bookingService;
    CustomerService& customerService;
    PaymentHandler& paymentHandler;
    WorkConfigurationRepository& workConfig; 
    
    pair<vector<string>, vector<int>> selectServices(int planID) {
        vector<string> requested;
        vector<int> requestedIds;  // NEW: Track IDs
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
                    return {{}, {}};  // Return empty pair
                }
                if (find(picks.begin(), picks.end(), p) == picks.end()) {
                    picks.push_back(p);
                    requested.push_back(work->name);
                    requestedIds.push_back(p);  // NEW
                } else {
                    cout << "Duplicate selection!\n";
                    return {{}, {}};
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
                    return {{}, {}};
                }
                
                requestedIds = packages[pc-1].workIds;  // NEW
                requested = workConfig.getWorkNamesByIds(requestedIds);
                
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
                        return {{}, {}};
                    }
                    if (find(picks.begin(), picks.end(), p) == picks.end()) {
                        picks.push_back(p);
                        requested.push_back(work->name);
                        requestedIds.push_back(p);  // NEW
                    } else {
                        cout << "Duplicate selection!\n";
                        return {{}, {}};
                    }
                }
            } else {
                cout << "Invalid choice!\n";
                return {{}, {}};
            }
        }
        
        return {requested, requestedIds};  // Return both
    }

public:
    ServiceRequestHandler(BookingService& bs, CustomerService& cs, 
                         PaymentHandler& ph, WorkConfigurationRepository& wc)
        : bookingService(bs), customerService(cs), paymentHandler(ph), workConfig(wc) {}
    
    void handleServiceRequest(Customer& customer) {
        // Service type with validation loop
        int typeID;
        while (true) {
            cout << "\nService Type:\n1) Immediate\n2) Scheduling\nChoice: ";
            typeID = InputHandler::getIntInput();
            if (typeID >= 1 && typeID <= 2) break;
            cout << "Invalid type! Please try again.\n";
        }
        
        // Plan selection with validation loop
        int planID;
        while (true) {
            cout << "\nSelect Plan:\n1) Basic (1 service)\n2) Intermediate (3 services)\n3) Premium (5 services or 1 package)\nChoice: ";
            planID = InputHandler::getIntInput();
            if (planID >= 1 && planID <= 3) break;
            cout << "Invalid plan! Please try again.\n";
        }
        string planStr = (planID == 1 ? "Basic" : (planID == 2 ? "Intermediate" : "Premium"));
        
        // Service selection with retry
        vector<string> requested;
        vector<int> requestedIds;
        while (true) {
            auto [names, ids] = selectServices(planID); 
            if (!names.empty()) {
                requested = names;
                requestedIds = ids;
                break;
            }
            cout << "Service selection failed! Please try again.\n";
        }
        
        // Gender preference (already has internal loop)
        string gpStr = InputHandler::selectGenderPreference();

        
    
        // Create service based on type
        Service s;
        if (typeID == 1) { // Immediate
            s = bookingService.createImmediate(planStr, customer.locality, customer.id, 
                                            customer.gender, customer.address, requested, gpStr);
        } else { // Scheduling
            string date, time;
            
            // Date with validation loop
            while (true) {
                cout << "Enter scheduled date (YYYY-MM-DD): ";
                date = InputHandler::getStringInput();
                if (Helper::isValidDate(date)) {
                    string curD = Helper::currentDate();
                    if (date >= curD) break;
                    cout << "Date must be today or in the future!\n";
                } else {
                    cout << "Invalid date format! Use YYYY-MM-DD\n";
                }
            }
            
            // Time with validation loop
            while (true) {
                cout << "Enter scheduled time (HH:MM:SS): ";
                time = InputHandler::getStringInput();
                if (Helper::isValidTime(time)) {
                    string curD = Helper::currentDate();
                    string curT = Helper::currentTime();
                    if (date > curD || (date == curD && time > curT)) break;
                    cout << "Time must be in the future!\n";
                } else {
                    cout << "Invalid time format! Use HH:MM:SS\n";
                }
            }
            
            s = bookingService.createScheduling(planStr, customer.locality, customer.id, 
                                            customer.gender, customer.address, requested, 
                                            gpStr, date, time);
        }

        
        double calculatedPrice = workConfig.getTotalPriceByIds(requestedIds);
        cout << "\nTotal Service Price: ₹" << calculatedPrice << "\n";    
        // Handle payment
        paymentHandler.handlePaymentFlow(s, requestedIds);

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

        // Get unique ID
        while (true) {
            cout << "Enter ID: ";
            c.id = InputHandler::getStringInput();

            if (customerService.idExists(c.id)) { 
                cout << "ID already exists! Try another.\n\n";
            } else {
                break;
            }
        }
        
        cout << "Password: ";
        c.password = InputHandler::getStringInput();
        cin.ignore();
        
        cout << "Enter Name: ";
        c.name = InputHandler::getLineInput();
        
        // Gender with loop (already handles retry internally)
        c.gender = InputHandler::selectGender();
        
        // Locality with loop (already handles retry internally)
        c.locality = InputHandler::selectLocality();
        
        cin.ignore();

        while (true) {
            cout << "Enter Address: ";
            c.address = InputHandler::getLineInput();

            if (c.address.empty()) { 
                cout << "Address can't be empty! Try again.\n\n";
            } else {
                break;
            }
        }
        if (customerService.registerCustomer(c)) {
            cout << "\nRegistration successful!\n";
        } else {
            cout << "\n✗ Registration failed!\n";
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
                        if (b.status == 2) {
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
                        if (b.status == 2) completed.push_back(b);
                    }
                    
                    if (completed.empty()) {
                        cout << "\nNo completed services to rebook.\n";
                        break;
                    }
                    
                    cout << "\n=== COMPLETED SERVICES ===\n";
                    for (auto &b : completed) {
                        cout << "ID: " << b.id << " | " << b.plan << " | " << b.bookingDate << "\n";
                    }
                    
                    // Service ID selection with validation
                    int rid;
                    Service* selectedService = nullptr;
                    while (true) {
                        cout << "\nEnter Service ID to rebook (0 to cancel): ";
                        rid = InputHandler::getIntInput();
                        
                        if (rid == 0) {
                            cout << "Rebooking cancelled.\n";
                            break;
                        }
                        
                        auto it = find_if(completed.begin(), completed.end(), 
                                        [&](const Service& s){ return s.id == rid; });
                        if (it != completed.end()) {
                            selectedService = &(*it);
                            break;
                        }
                        cout << "Service ID not found! Please try again.\n";
                    }
                    
                    if (!selectedService) break; // User cancelled
                    
                    // Rebook type selection with validation
                    int rt;
                    while (true) {
                        cout << "\nRebook as:\n1) Immediate\n2) Scheduling\n0) Cancel\nChoice: ";
                        rt = InputHandler::getIntInput();
                        if (rt >= 0 && rt <= 2) break;
                        cout << "Invalid choice! Please try again.\n";
                    }
                    
                    if (rt == 0) {
                        cout << "Rebooking cancelled.\n";
                        break;
                    }
                    
                    Service newService;
                    if (rt == 1) {
                        newService = bookingService.createImmediate(
                            selectedService->plan, customer.locality, customer.id, 
                            customer.gender, customer.address, 
                            selectedService->requestedServices, selectedService->genderPref);
                    } else { // rt == 2
                        string d, t;
                        string curD = Helper::currentDate();
                        string curT = Helper::currentTime();
                        
                        // Date validation loop
                        while (true) {
                            cout << "Date (YYYY-MM-DD): ";
                            d = InputHandler::getStringInput();
                            if (Helper::isValidDate(d) && d >= curD) break;
                            cout << "Invalid or past date! Please try again.\n";
                        }
                        
                        // Time validation loop
                        while (true) {
                            cout << "Time (HH:MM:SS): ";
                            t = InputHandler::getStringInput();
                            if (Helper::isValidTime(t) && (d > curD || (d == curD && t > curT))) break;
                            cout << "Invalid or past time! Please try again.\n";
                        }
                        
                        newService = bookingService.createScheduling(
                            selectedService->plan, customer.locality, customer.id, 
                            customer.gender, customer.address, 
                            selectedService->requestedServices, selectedService->genderPref, d, t);
                    }
                    
                    PaymentHandler rebookPaymentHandler(paymentService, serviceRepo);
                    vector<int> workIds = workConfig.getIdsByNames(selectedService->requestedServices);
                    rebookPaymentHandler.handlePaymentFlow(newService, workIds);

                    
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
                    int e;
                    while (true) {
                        cout << "\n=== EDIT PROFILE ===\n";
                        cout << "1) Password\n2) Locality\n3) Address\n0) Back\nChoice: ";
                        e = InputHandler::getIntInput();
                        if (e >= 0 && e <= 3) break;
                        cout << "Invalid choice! Please try again.\n";
                    }
                    
                    if (e == 0) break; // User cancelled
                    
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
                        customer.locality = newLoc;
                        customerService.updateCustomer(customer);  
                        cout << "Locality updated!\n";
                    } else if (e == 3) {
                        cin.ignore();
                        while (true) {
                            cout << "New Address: ";
                            string a = InputHandler::getLineInput();
                            if (!a.empty()) {
                                customer.address = a;
                                customerService.updateCustomer(customer);  
                                cout << "Address updated!\n";
                                break;
                            }
                            cout << "Address cannot be empty! Please try again.\n";
                        }
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
