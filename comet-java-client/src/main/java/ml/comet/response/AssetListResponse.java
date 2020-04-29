package ml.comet.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AssetListResponse {
    List<AssetInfo> assets;
}
