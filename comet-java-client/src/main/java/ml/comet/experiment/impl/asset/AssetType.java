package ml.comet.experiment.impl.asset;

/**
 * Represents known types of the assets.
 */
public enum AssetType {
    /**
     * Represents all/any asset types.
     */
    ASSET_TYPE_ALL("all"),
    /**
     * Represents unknown asset type.
     */
    ASSET_TYPE_UNKNOWN("unknown"),
    /**
     * Represents general asset type.
     */
    ASSET_TYPE_ASSET("asset"),
    /**
     * Represents source code asset.
     */
    ASSET_TYPE_SOURCE_CODE("source_code"),
    /**
     * Represents asset type for the 3d points and bounding boxes as an asset.
     */
    ASSET_TYPE_3D_POINTS("3d-points"),
    /**
     * Represents asset type for the data structure holding embedding template info.
     */
    ASSET_TYPE_EMBEDDINGS("embeddings"),
    /**
     * Represents asset type for tabular data, including data, csv files, tsv files, and Pandas dataframes.
     */
    ASSET_TYPE_DATAFRAME("dataframe"),
    /**
     * Represents asset type for a pandas DataFrame profile.
     */
    ASSET_TYPE_DATAFRAME_PROFILE("dataframe-profile"),
    /**
     * Represents asset type for the histogram of values for a 3D chart.
     */
    ASSET_TYPE_HISTOGRAM_3D("histogram3d"),
    /**
     * Represents confusion matrix asset type.
     */
    ASSET_TYPE_CONFUSION_MATRIX("confusion-matrix"),
    /**
     * Represents simple x/y curve.
     */
    ASSET_TYPE_CURVE("curve"),
    /**
     * Represents iPython, Jupyter or similar notebook asset.
     */
    ASSET_TYPE_NOTEBOOK("notebook"),
    /**
     * Represents asset type for the NN model elements.
     */
    ASSET_TYPE_MODEL_ELEMENT("model-element"),
    /**
     * Represents asset type for text samples.
     */
    ASSET_TYPE_TEXT_SAMPLE("text-sample");


    private final String assetType;

    AssetType(String assetType) {
        this.assetType = assetType;
    }

    /**
     * Returns type of the asset as string.
     *
     * @return the type of the asset as string.
     */
    public String type() {
        return this.assetType;
    }

    @Override
    public String toString() {
        return this.assetType;
    }
}
