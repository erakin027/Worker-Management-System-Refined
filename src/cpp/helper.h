# pragma once

#include <iostream>
#include <string>
#include<chrono>
#include<regex>
using namespace std;

// Helper utilities 
class Helper {
public:
    static string currentDate(){
        time_t now = time(0);
        tm *ltm = localtime(&now);
        // Get current date as string
        string currentDate = to_string(1900 + ltm->tm_year) + "-" + 
                            (ltm->tm_mon + 1 < 10 ? "0" : "") + to_string(1 + ltm->tm_mon) + "-" +
                            (ltm->tm_mday < 10 ? "0" : "") + to_string(ltm->tm_mday);
        return currentDate;
    }

    static string currentTime(){
        time_t now = time(0);
        tm *ltm = localtime(&now);
        // Get time as string
        string currentTime = (ltm->tm_hour < 10 ? "0" : "") + to_string(ltm->tm_hour) + ":" +
                            (ltm->tm_min < 10 ? "0" : "") + to_string(ltm->tm_min) + ":" +
                            (ltm->tm_sec < 10 ? "0" : "") + to_string(ltm->tm_sec);
        return currentTime;
    }

    // Function to validate date format and logic
    static bool isValidDate(const string& date) {
        // Regular expression for YYYY-MM-DD format
        regex dateRegex(R"(\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01]))");

        if (!regex_match(date, dateRegex)) {
            return false;
        }

        // Extract year, month, and day
        int year = stoi(date.substr(0, 4));
        int month = stoi(date.substr(5, 2));
        int day = stoi(date.substr(8, 2));

        // Check for valid day ranges in each month
        int daysInMonth[] = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

        // Leap year adjustment for February
        if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
            daysInMonth[1] = 29;
        }

        return day <= daysInMonth[month - 1];
    }

    // Function to validate time format
    static bool isValidTime(const string& time) {
        // Regular expression for HH:MM:SS format
        regex timeRegex(R"((0[0-9]|1[0-9]|2[0-3]):([0-5][0-9]):([0-5][0-9]))");
        return regex_match(time, timeRegex);
    }
};

