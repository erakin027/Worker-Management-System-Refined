#pragma once

#include "repos.h"


class IPricingStrategy {
public:
    virtual ~IPricingStrategy() = default;
    virtual double calculatePrice(const vector<int>& workIds) const = 0;
};

class BasicPricing : public IPricingStrategy {
private:
    const WorkConfigurationRepository& workConfig;

public:
    BasicPricing(const WorkConfigurationRepository& wc) : workConfig(wc) {}

    double calculatePrice(const vector<int>& workIds) const override {
        return workConfig.getTotalPriceByIds(workIds); // NO DISCOUNT
    }
};

class IntermediatePricing : public IPricingStrategy {
private:
    const WorkConfigurationRepository& workConfig;

public:
    IntermediatePricing(const WorkConfigurationRepository& wc) : workConfig(wc) {}

    double calculatePrice(const vector<int>& workIds) const override {
        double base = workConfig.getTotalPriceByIds(workIds);
        return base * 0.90; // 10% OFF
    }
};

class PremiumPricing : public IPricingStrategy {
private:
    const WorkConfigurationRepository& workConfig;

public:
    PremiumPricing(const WorkConfigurationRepository& wc) : workConfig(wc) {}

    double calculatePrice(const vector<int>& workIds) const override {
        double base = workConfig.getTotalPriceByIds(workIds);
        return base * 0.80; // 20% OFF
    }
};
