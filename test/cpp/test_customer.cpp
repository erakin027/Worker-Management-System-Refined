#include <gtest/gtest.h>
#include <fstream>
#include <filesystem>
#include "controller.h"

namespace fs = std::filesystem;

// Test Fixtures
class CustomerSystemTest : public ::testing::Test {
protected:
    JsonCustomerRepository* custRepo;
    JsonServiceRepository* servRepo;
    JsonPaymentRepository* payRepo;
    WorkConfigurationRepository* workConfig;
    
    void SetUp() override {
        // Create test files
        custRepo = new JsonCustomerRepository("../test_customers.json");
        servRepo = new JsonServiceRepository("../test_services.json");
        payRepo = new JsonPaymentRepository("../test_payments.json");
        workConfig = new WorkConfigurationRepository("../works_config.json");
    }
    
    void TearDown() override {
        delete custRepo;
        delete servRepo;
        delete payRepo;
        delete workConfig;
        
        // Clean up test files
        fs::remove("../test_customers.json");
        fs::remove("../test_services.json");
        fs::remove("../test_payments.json");
    }
};

//  Helper Tests 

TEST(HelperTest, CurrentDateFormat) {
    string date = Helper::currentDate();
    EXPECT_EQ(date.length(), 10);
    EXPECT_EQ(date[4], '-');
    EXPECT_EQ(date[7], '-');
}

TEST(HelperTest, CurrentTimeFormat) {
    string time = Helper::currentTime();
    EXPECT_EQ(time.length(), 8);
    EXPECT_EQ(time[2], ':');
    EXPECT_EQ(time[5], ':');
}

TEST(HelperTest, ValidDateValidation) {
    EXPECT_TRUE(Helper::isValidDate("2024-12-25"));
    EXPECT_FALSE(Helper::isValidDate("2024-13-25"));
    EXPECT_FALSE(Helper::isValidDate("24-12-25"));
    EXPECT_FALSE(Helper::isValidDate("2024/12/25"));
}

TEST(HelperTest, ValidTimeValidation) {
    EXPECT_TRUE(Helper::isValidTime("14:30:45"));
    EXPECT_FALSE(Helper::isValidTime("25:30:45"));
    EXPECT_FALSE(Helper::isValidTime("14:30"));
    EXPECT_FALSE(Helper::isValidTime("14-30-45"));
}

//  Customer Tests 

TEST_F(CustomerSystemTest, RegisterNewCustomer) {
    Customer c;
    c.id = "test001";
    c.password = "pass123";
    c.name = "Test User";
    c.gender = "M";
    c.locality = "Benz Circle";
    c.address = "123 Test St";
    
    CustomerService cs(*custRepo, *servRepo);
    EXPECT_TRUE(cs.registerCustomer(c));
}

TEST_F(CustomerSystemTest, RegisterDuplicateCustomer) {
    Customer c;
    c.id = "test001";
    c.password = "pass123";
    c.name = "Test User";
    c.gender = "M";
    c.locality = "Benz Circle";
    c.address = "123 Test St";
    
    CustomerService cs(*custRepo, *servRepo);
    EXPECT_TRUE(cs.registerCustomer(c));
    EXPECT_FALSE(cs.registerCustomer(c));  // Duplicate
}

TEST_F(CustomerSystemTest, AuthenticateValidCredentials) {
    Customer c;
    c.id = "test001";
    c.password = "pass123";
    c.name = "Test User";
    c.gender = "M";
    c.locality = "Benz Circle";
    c.address = "123 Test St";
    
    CustomerService cs(*custRepo, *servRepo);
    cs.registerCustomer(c);
    
    auto result = cs.authenticate("test001", "pass123");
    EXPECT_TRUE(result.has_value());
    EXPECT_EQ(result->name, "Test User");
}

TEST_F(CustomerSystemTest, AuthenticateInvalidCredentials) {
    Customer c;
    c.id = "test001";
    c.password = "pass123";
    c.name = "Test User";
    c.gender = "M";
    c.locality = "Benz Circle";
    c.address = "123 Test St";
    
    CustomerService cs(*custRepo, *servRepo);
    cs.registerCustomer(c);
    
    auto result = cs.authenticate("test001", "wrongpass");
    EXPECT_FALSE(result.has_value());
}

TEST_F(CustomerSystemTest, UpdateCustomer) {
    Customer c;
    c.id = "test001";
    c.password = "pass123";
    c.name = "Test User";
    c.gender = "M";
    c.locality = "Benz Circle";
    c.address = "123 Test St";
    
    CustomerService cs(*custRepo, *servRepo);
    cs.registerCustomer(c);
    
    c.address = "456 New St";
    EXPECT_TRUE(cs.updateCustomer(c));
    
    auto updated = custRepo->findById("test001");
    EXPECT_EQ(updated->address, "456 New St");
}

//  Service Tests 

TEST_F(CustomerSystemTest, CreateImmediateService) {
    BookingService bs(*servRepo);
    
    vector<string> services = {"Window Cleaning", "Mopping"};
    Service s = bs.createImmediate(
        "Basic", "Benz Circle", "test001", "M", 
        "123 Test St", services, "NP"
    );
    
    EXPECT_GT(s.id, 0);
    EXPECT_EQ(s.status, 0);
    EXPECT_EQ(s.type, Service::Type::Immediate);
}

TEST_F(CustomerSystemTest, CreateSchedulingService) {
    BookingService bs(*servRepo);
    
    vector<string> services = {"Washing", "Drying"};
    Service s = bs.createScheduling(
        "Intermediate", "Patamata", "test001", "F", 
        "456 Test Ave", services, "M",
        "2025-12-20", "14:00:00"
    );
    
    EXPECT_GT(s.id, 0);
    EXPECT_EQ(s.status, 0);
    EXPECT_EQ(s.type, Service::Type::Scheduling);
    EXPECT_EQ(*s.workDate, "2025-12-20");
}

TEST_F(CustomerSystemTest, ServiceStatusTransitions) {
    BookingService bs(*servRepo);
    
    vector<string> services = {"Sweeping"};
    Service s = bs.createImmediate(
        "Basic", "Benz Circle", "test001", "M", 
        "123 Test St", services, "NP"
    );
    
    // Initial status
    EXPECT_EQ(s.status, 0);
    
    // Update to worker assigned
    s.status = 1;
    servRepo->save(s);
    auto updated = servRepo->findByCustomer("test001");
    EXPECT_EQ(updated[0].status, 1);
    
    // Update to completed
    s.status = 2;
    servRepo->save(s);
    updated = servRepo->findByCustomer("test001");
    EXPECT_EQ(updated[0].status, 2);
}

// Repository Tests

TEST(JsonRepoTest, CustomerSaveAndLoad) {
    JsonCustomerRepository repo("../test_customers.json");

    Customer c;
    c.id = "C100";
    c.password = "p1";
    c.name = "Keerthi";
    c.gender = "F";
    c.locality = "Patamata";
    c.address = "Lane 12";

    repo.save(c);

    auto out = repo.findById("C100");
    ASSERT_TRUE(out.has_value());
    EXPECT_EQ(out->name, "Keerthi");

    fs::remove("../test_customers.json");
}

TEST(JsonRepoTest, ServiceMultipleForSameCustomer) {
    JsonServiceRepository repo("../test_services.json");

    Service s1; s1.id = 1; s1.customerID = "C101";
    Service s2; s2.id = 2; s2.customerID = "C101";

    repo.save(s1);
    repo.save(s2);

    auto list = repo.findByCustomer("C101");
    ASSERT_EQ(list.size(), 2u);

    fs::remove("../test_services.json");
}

TEST(JsonRepoTest, ExistsMethod) {
    JsonCustomerRepository repo("../test_customer_exists.json");

    Customer c;
    c.id = "X1"; c.password = "p"; c.name = "Test";
    repo.save(c);

    EXPECT_TRUE(repo.exists("X1"));
    EXPECT_FALSE(repo.exists("NOPE"));

    fs::remove("../test_customer_exists.json");
}

//  Work Configuration Tests 

TEST(WorkConfigTest, LoadDefaultWorks) {
    WorkConfigurationRepository wc("../nonexistent.json");
    
    auto works = wc.getWorks();
    EXPECT_EQ(works.size(), 10);
    EXPECT_EQ(works[0].name, "Window Cleaning");
    EXPECT_EQ(works[0].price, 600);
    EXPECT_EQ(works[0].timeMinutes, 80);
}

TEST(WorkConfigTest, GetWorkById) {
    WorkConfigurationRepository wc("../works_config.json");
    
    auto work = wc.getWorkById(1);
    ASSERT_TRUE(work.has_value());
    EXPECT_EQ(work->name, "Window Cleaning");
    EXPECT_EQ(work->price, 600);
}

TEST(WorkConfigTest, GetTotalPriceByIds) {
    WorkConfigurationRepository wc("../works_config.json");
    
    vector<int> ids = {1, 2, 3};  // Window + Mopping + Sweeping
    double total = wc.getTotalPriceByIds(ids);
    EXPECT_EQ(total, 1100);  // 600 + 300 + 200
}

TEST(WorkConfigTest, GetWorkNamesByIds) {
    WorkConfigurationRepository wc("../works_config.json");
    
    vector<int> ids = {1, 2};
    auto names = wc.getWorkNamesByIds(ids);
    EXPECT_EQ(names.size(), 2);
    EXPECT_EQ(names[0], "Window Cleaning");
    EXPECT_EQ(names[1], "Mopping");
}

//  Pricing Tests 

TEST(PricingTest, BasicPricing) {
    WorkConfigurationRepository wc("../works_config.json");
    BasicPricing pricing(wc);
    
    vector<int> ids = {1, 2};  // 600 + 300
    double price = pricing.calculatePrice(ids);
    EXPECT_EQ(price, 900);
}

TEST(PricingTest, IntermediatePricing) {
    WorkConfigurationRepository wc("../works_config.json");
    IntermediatePricing pricing(wc);
    
    vector<int> ids = {1, 2, 3};  // 600 + 300 + 200 = 1100
    double price = pricing.calculatePrice(ids);
    EXPECT_DOUBLE_EQ(price, 990);  // 10% off
}

TEST(PricingTest, PremiumPricing) {
    WorkConfigurationRepository wc("../works_config.json");
    PremiumPricing pricing(wc);
    
    vector<int> ids = {1, 2, 3, 4, 5};  // 600+300+200+400+500 = 2000
    double price = pricing.calculatePrice(ids);
    EXPECT_DOUBLE_EQ(price, 1600);  // 20% off
}

//  Payment Tests 

TEST_F(CustomerSystemTest, GeneratePayment) {
    PaymentService ps(*payRepo);
    ps.attachWorkConfig(*workConfig);
    
    BookingService bs(*servRepo);
    vector<string> services = {"Window Cleaning"};
    Service s = bs.createImmediate(
        "Basic", "Benz Circle", "test001", "M", 
        "123 Test St", services, "NP"
    );
    
    vector<int> workIds = {1};
    Payment p = ps.generatePayment(s, workIds);
    
    EXPECT_EQ(p.serviceID, s.id);
    EXPECT_EQ(p.amountDue, 600);
    EXPECT_FALSE(p.paid);
}

TEST_F(CustomerSystemTest, ProcessPaymentSuccess) {
    PaymentService ps(*payRepo);
    ps.attachWorkConfig(*workConfig);
    
    BookingService bs(*servRepo);
    vector<string> services = {"Window Cleaning"};
    Service s = bs.createImmediate(
        "Basic", "Benz Circle", "test001", "M", 
        "123 Test St", services, "NP"
    );
    
    vector<int> workIds = {1};
    Payment p = ps.generatePayment(s, workIds);
    
    EXPECT_TRUE(ps.processPayment(s.id, 600));
    
    auto updated = ps.getPayment(s.id);
    EXPECT_TRUE(updated->paid);
}

TEST_F(CustomerSystemTest, ProcessPaymentWrongAmount) {
    PaymentService ps(*payRepo);
    ps.attachWorkConfig(*workConfig);
    
    BookingService bs(*servRepo);
    vector<string> services = {"Window Cleaning"};
    Service s = bs.createImmediate(
        "Basic", "Benz Circle", "test001", "M", 
        "123 Test St", services, "NP"
    );
    
    vector<int> workIds = {1};
    ps.generatePayment(s, workIds);
    
    EXPECT_FALSE(ps.processPayment(s.id, 500));  // Wrong amount
}

// Payment Handler Tests
TEST_F(CustomerSystemTest, CancelPaymentFlow) {
    PaymentService ps(*payRepo);
    ps.attachWorkConfig(*workConfig);

    Service s;
    s.id = 99;
    s.plan = "Basic";

    PaymentHandler handler(ps, *servRepo);

    std::istringstream fake(
        "4\n"   // choose cancel from payment menu
    );
    std::cin.rdbuf(fake.rdbuf());

    bool result = handler.handlePaymentFlow(s, {1});
    EXPECT_FALSE(result);  // cancelled
}

// Service Request Handler Tests
TEST(ControllerTests, ImmediateBasicSuccessfulPayment) {
    JsonCustomerRepository cust("../tcus.json");
    JsonServiceRepository serv("../tser.json");
    JsonPaymentRepository pay("../tpay.json");

    CustomerService cs(cust, serv);
    BookingService bs(serv);
    PaymentService ps(pay);

    WorkConfigurationRepository wc("../works_config.json");
    ps.attachWorkConfig(wc);

    PaymentHandler ph(ps, serv);
    ServiceRequestHandler handler(bs, cs, ph, wc);

    Customer c;
    c.id = "C10";
    c.password = "p";
    c.name = "User";
    c.gender = "M";
    c.locality = "SN Puram";
    c.address = "Addr";

    cust.save(c);

    // Simulate user input:
    // 1 → Immediate
    // 1 → Basic
    // 1 → Pick service 1
    // 0 → No gender preference
    // 1 - choose upi
    // 600 → enter amount exactly
    std::istringstream fake(
        "1\n1\n1\n0\n1\n600\n"
    );
    std::cin.rdbuf(fake.rdbuf());

    EXPECT_NO_THROW(handler.handleServiceRequest(c));

    // Check if service was saved and payment processed
    auto services = serv.findByCustomer("C10");
    ASSERT_FALSE(services.empty());
    EXPECT_EQ(services[0].price, 600);

    auto payment = pay.findByService(services[0].id);
    ASSERT_TRUE(payment.has_value());
    EXPECT_TRUE(payment->paid);

    fs::remove("../tcus.json");
    fs::remove("../tser.json");
    fs::remove("../tpay.json");
}



//  Main Test Runner 

int main(int argc, char **argv) {
    ::testing::InitGoogleTest(&argc, argv);
    return RUN_ALL_TESTS();
}