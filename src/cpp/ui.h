#pragma once

#include "services.h"

// UI Components  

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
        if (cin.fail()) {
            cin.clear();
            cin.ignore(10000, '\n');
            return -1;
        }
        return choice;
    }
    
    static double getDoubleInput() {
        double value = 0.0;
        cin >> value;
        if (cin.fail()) {
            cin.clear();
            cin.ignore(10000, '\n');
            return -1.0;
        }
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
        while (true) {
            cout << "Select Gender:\n1) M\n2) F\nChoice: ";
            int gopt = getIntInput();
            if (gopt == 1) return "M";
            if (gopt == 2) return "F";
            cout << "Invalid choice! Please try again.\n\n";
        }
    }
    
    static string selectLocality() {
        while (true) {
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
                default: 
                    cout << "Invalid choice! Please try again.\n\n";
                    continue;
            }
        }
    }
    
    static string selectGenderPreference() {
        while (true) {
            cout << "Gender preference?\n0) No Preference\n1) Male\n2) Female\nChoice: ";
            int gp = getIntInput();
            if (gp == 0) return "NP";
            if (gp == 1) return "M";
            if (gp == 2) return "F";
            cout << "Invalid choice! Please try again.\n\n";
        }
    }
};

class ServiceDisplay {
public:
    static void printServiceSummary(const Service& s) {
        cout << "\n--- Service ID: " << s.id << " ---\n";
        string statusStr;
        switch (s.status) {
            case 0: statusStr = "Pending"; break;
            case 1: statusStr = "Worker Assigned"; break;
            case 2: statusStr = "Completed"; break;
            case -1: statusStr = "Rejected"; break;
            default: statusStr = "Unknown"; break;
        }
        cout << "Status: " << statusStr << "\n";
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
            return true;
        } else {
            cout << "\nPayment FAILED! Amount must be exactly ₹" << amountDue << "\n";
            return false;
        }
    }
    
public:
    PaymentHandler(PaymentService& ps, IServiceRepository& sr) 
        : paymentService(ps), serviceRepo(sr) {}

    bool handlePaymentFlow(const Service& service, const vector<int>& workIds){
        auto payment = paymentService.generatePayment(service, workIds);
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
                    Service updated = service; 
                    updated.price = payment.amountDue; 
                    serviceRepo.save(updated);  
                    cout << "Service request submitted and payment completed.\n";
                    return true;
                }
                // Loop back to payment methods if failed
            } else {
                cout << "Invalid payment method!\n";
            }
        }
    }
};

