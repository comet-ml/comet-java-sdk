package com.comet.response;

import lombok.Data;

@Data
public class ValueSummary {
    private String name;
    private String valueMax;
    private String valueMin;
    private String valueCurrent;
    private Long timestampMax;
    private Long timestampMin;
    private Long timestampCurrent;
    private String runContextMax;
    private String runContextMin;
    private String runContextCurrent;
    private Long stepMax;
    private Long stepMin;
    private Long stepCurrent;
}
