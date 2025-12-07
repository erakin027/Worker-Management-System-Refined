#include "controller.h"

// Main Application 
int main() {
    // Initialize repositories
    JsonCustomerRepository custRepo("../data/customers.json");
    JsonServiceRepository servRepo("../data/services.json");
    JsonPaymentRepository paymentRepo("../data/payments.json");
    WorkConfigurationRepository workConfig("../data/works_config.json"); 
    
    // Initialize services
    CustomerService customerService(custRepo, servRepo);
    BookingService bookingService(servRepo);
    PaymentService paymentService(paymentRepo);
    paymentService.attachWorkConfig(workConfig);

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
