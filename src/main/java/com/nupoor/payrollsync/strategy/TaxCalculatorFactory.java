package com.nupoor.payrollsync.strategy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TaxCalculatorFactory {

    private final Map<String, TaxCalculatorStrategy> strategyMap;
    private final GenericEUTaxCalculator fallbackCalculator;

    public TaxCalculatorFactory(List<TaxCalculatorStrategy> strategies, GenericEUTaxCalculator fallbackCalculator) {
        this.strategyMap = strategies.stream()
                .collect(Collectors.toMap(
                        s -> s.getCountryCode().toUpperCase(),
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
        this.fallbackCalculator = fallbackCalculator;
    }

    public TaxCalculatorStrategy getStrategy(String countryCode) {
        if (countryCode == null || countryCode.isBlank()) {
            return fallbackCalculator;
        }
        return strategyMap.getOrDefault(countryCode.toUpperCase().trim(), fallbackCalculator);
    }
}
