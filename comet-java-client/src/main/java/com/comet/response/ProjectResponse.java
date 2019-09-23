package com.comet.response;

import lombok.Data;

import java.util.List;

@Data
public class ProjectResponse {
    List<ProjectRest> projects;
}
