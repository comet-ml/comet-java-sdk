package ml.comet.experiment.impl.rest;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GpuStaticInfoRest {
    private Integer gpuIndex;
    private String name;
    private String uuid;
    private Long totalMemory;
    private Integer powerLimit;
}
