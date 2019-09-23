package com.comet.response;

import lombok.Data;

import java.util.List;

@Data
public class ParametersResponse {
    private List<ValueSummary> results;
}
