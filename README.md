# Worker-Management-System-Refined
The Worker Management System (WMS) is a modular, terminal-based application using C++ and Java designed to connect customers who need household services with workers who can perform them.  
The system is divided into three independent components that operate on shared JSON data files.

The system is divided into:

- **Customer Application (C++)** – customer registration, login, service booking, and payments.
- **Admin / Worker Application (Java)** – worker/admin operations (implemented separately by project partner).

Both parts share the same `data/` directory.

---

##  Project Components & Features

The Worker Management System (WMS) is a modular, terminal-based application designed to connect customers who need household services with workers who can perform them.  
The system is divided into three independent components that operate on shared JSON data files.

---

### 1. Customer Module (C++ Application)

This is the customer-facing part of the system. It allows customers to register, log in, request services, view history, and handle payments.

#### **Features**
- **Register & Login**  
  Customers can create an account with name, password, gender, locality, and address. They can log in to access their dashboard.

- **Create Service Requests**  
  Customers can choose immediate or scheduled services. They select required tasks, gender preference, and provide timing. Scheduling includes validation (future date/time only).

- **View Current Bookings**  
  Shows all active service request - pending/worker-assigned yet awaiting completion.

- **View Service History**  
  Lists all completed services with full details.

- **View Rejected Requests**  
  Displays services rejected by the admin along with the recorded reason.

- **Rebook Previous Services**  
  Allows rebooking of any previously completed service.

- **Edit Profile**  
  Customers can update their password, locality, or address at any time.

- **Payment Processing**  
  Billing is based on selected service plan (Basic/Intermediate/Premium), work pricing configuration, and applied discounts.

---

### 2. Admin Module (Java Application)

The admin oversees request approval, worker assignment, and database management.

#### **Features**
- **Admin Login**  
  Access to admin functionalities.

- **View Pending Customer Requests**  
  Admin reviews all unprocessed service requests.

- **Filter Workers by Requirements**  
  Admin can filter workers based on:
  - Skill match  
  - Gender preference  
  - Locality  
  - Availability  
  - Calendar conflict checks

- **Assign Workers to Requests**  
  The admin selects suitable workers and assigns them to customers.

- **Reject Requests**  
  If assignment is not possible, the admin rejects the request with a mandatory reason.

---

### 3. Worker Module (Java Application)

Workers manage their daily schedules and job confirmations.

#### **Features**
- **Worker Login**  
  Workers can create an account with name, password, gender, locality, and works. They can log in to access their dashboard.

- **View Assigned Jobs**  
  Workers can see all pending tasks assigned by the admin.

- **View Service History**  
  Lists all completed services with full details.
  
- **Edit Profile**  
  Workers can modify locality, password, or skill sets.

---

## Shared System Components

### **JSON-Based Storage**
All modules share the same data located in the `data/` folder:
- `customers.json`
- `workers.json`
- `services.json`
- `payments.json`
- `works_config.json`

These files act as a lightweight database and synchronize state between the C++ and Java applications.

### **Work Configuration & Pricing**
- Centralized config file defines all available tasks, time required, and pricing.
- Supports three pricing plans:
  - **Basic** - no discount  
  - **Intermediate** - 10% discount  
  - **Premium** - 20% discount  

### **Service Status Workflow**
All service requests follow a consistent lifecycle:

customer requests service - status = 0 (pending) -> 
Admin assigns worker - status = 1 (assigned) -> 
worker completes work and marks as complete - status = 2 (completed)

(Alternative outcomes: admin does not assign worker and marks as rejected (status = -1))

---

## Building & Run Instructions

To run the Customer Application (C++) and the Worker/Admin Application (Java) at the same time, you will need to use two separate terminal windows.
Each terminal runs its own independent module.

**Note:**  You may open as many terminals as you like to simulate multiple customers and multiple workers concurrently. 
The instructions below describe how to launch one instance of each module, but you can repeat the steps to run additional instances.

### C++ customer End
This project uses **CMake**. All headers needed like nholmann/json.hpp and GoogleTest are bundled inside the project, so no installation is required.

From the project root:

```bash
mkdir build
cd build
cmake ..
cmake --build .
```

This generates:

- customerApp – the main customer module

- tests – GoogleTest test suite

**To run the application:**

From inside build/:

```bash
./customerApp
```

**To run the tests:**

From inside build/:

```bash
./tests
```

### Java worker/admin End

**To run the application:**

From the root directory:

```bash
cd src/java
javac Main.java
java Main.java
```

**To run the tests:**

From the root directory:

```bash
***instructions to be added
```

## Team Members

- Aditya KNV             - IMT2023033
- Abhijit Dibbidi        - IMT2023054
- Vishnu Balla           - IMT2023097
- Marudi Praneeth Reddy  - IMT2023555
