package com.caching.exchange_rate_service_caching.dto;

import java.math.BigDecimal;
import java.util.Map;

public record FrankfurterLatestResponse(
        String amount,
        String base,
        String date,
        Map<String, BigDecimal> rates
) {
}
