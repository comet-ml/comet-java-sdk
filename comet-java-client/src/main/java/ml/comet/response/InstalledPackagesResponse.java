package ml.comet.response;

import lombok.Data;

import java.util.List;

@Data
public class InstalledPackagesResponse {
    private List<String> packages;
}
