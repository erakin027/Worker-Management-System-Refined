#pragma once
#include <iostream>
#include <string>
#include <vector>
#include <optional>
#include <nlohmann/json.hpp>

using json = nlohmann::json;
using namespace std;

class Service {
public:
    enum class Type {Immediate, Scheduling};

    int id = 0;
    int status = 0; // 0=Pending (Requested), 1=Worker Assigned, 2=Completed, -1=Rejected
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
    double price;

    optional<string> workDate;
    optional<string> workStartTime;
    optional<string> workEndTime;
    optional<string> assignedWorkerIDs;
    optional<string> reason;

    string typeStr() const { return type == Type::Immediate ? "Immediate" : "Scheduling"; }
};

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
        {"genderPref", s.genderPref},
        {"price", s.price}
    };
    if (s.workDate) j["workDate"] = *s.workDate;
    if (s.workStartTime) j["workStartTime"] = *s.workStartTime;
    if (s.workEndTime) j["workEndTime"] = *s.workEndTime;
    if (s.assignedWorkerIDs) j["assignedWorkerIDs"] = *s.assignedWorkerIDs;
    if (s.reason) j["reason"] = *s.reason;
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
    // vector<int> bookingsIDs;
};

inline void to_json(json& j, const Customer& c) {
    j = json{
        {"id", c.id},
        {"password", c.password},
        {"name", c.name},
        {"gender", c.gender},
        {"locality", c.locality},
        {"address", c.address}
        // {"bookingsIDs", c.bookingsIDs}
    };
}

inline void from_json(const json& j, Customer& c) {
    j.at("id").get_to(c.id);
    j.at("password").get_to(c.password);
    j.at("name").get_to(c.name);
    j.at("gender").get_to(c.gender);
    j.at("locality").get_to(c.locality);
    j.at("address").get_to(c.address);
    // if (j.contains("bookingsIDs")) j.at("bookingsIDs").get_to(c.bookingsIDs);
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

class Work {
public:
    int id;
    string name;
    string category;
    int timeMinutes;  
    double price; 
};

inline void from_json(const json& j, Work& w) {
    j.at("id").get_to(w.id);
    j.at("name").get_to(w.name);
    j.at("category").get_to(w.category);
    j.at("timeMinutes").get_to(w.timeMinutes);  
    j.at("price").get_to(w.price);              
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
