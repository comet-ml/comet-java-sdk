package com.comet.response;

import lombok.Data;

import java.util.List;

@Data
public class MetricsResponse {
    private List<ValueSummary> results;
}
