# Worker-Management-System-Refined
After you download the files form git, For the customer side, run customer.cpp

g++ customer.cpp
./a
If you want the worker/admin side: run Main.java

javac Main.java
java Main.java
Note that there is only one admin, and he is fixed with the following details: Admin user id: Admin Password: admin123

As a worker though, you can freely sign up as a new user too.

Customer.txt and service.txt will automatically be created, even if they are not present at the time of running.
Admin.txt should definitely be present at the time of running as it is fixed
When you are logged in as admin, if there is a scheduling request that needs to be assigned, the code will show you eligible workers based on :

Locality
Preferred Gender
Worker contains at least one requested work Now, you should manually check whether the workers don't have any booked services during the requested time, and assign them.
