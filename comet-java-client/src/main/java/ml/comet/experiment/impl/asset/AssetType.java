package ml.comet.experiment.impl.asset;

/**
 * Represents known types of the assets.
 */
public enum AssetType {
    /**
     * Represents all/any asset types.
     */
    ALL("all"),
    /**
     * Represents unknown asset type.
     */
    UNKNOWN("unknown"),
    /**
     * Represents general asset type.
     */
    ASSET("asset"),
    /**
     * Represents source code asset.
     */
    SOURCE_CODE("source_code"),
    /**
     * Represents asset type for the 3d points and bounding boxes as an asset.
     */
    POINTS_3D("3d-points"),
    /**
     * Represents asset type for the data structure holding embedding template info.
     */
    EMBEDDINGS("embeddings"),
    /**
     * Represents asset type for tabular data, including data, csv files, tsv files, and Pandas dataframes.
     */
    DATAFRAME("dataframe"),
    /**
     * Represents asset type for a pandas DataFrame profile.
     */
    DATAFRAME_PROFILE("dataframe-profile"),
    /**
     * Represents asset type for the histogram of values for a 3D chart.
     */
    HISTOGRAM3D("histogram3d"),
    /**
     * Represents confusion matrix asset type.
     */
    CONFUSION_MATRIX("confusion-matrix"),
    /**
     * Represents simple x/y curve.
     */
    CURVE("curve"),
    /**
     * Represents iPython, Jupyter or similar notebook asset.
     */
    NOTEBOOK("notebook"),
    /**
     * Represents asset type for the NN model elements.
     */
    MODEL_ELEMENT("model-element"),
    /**
     * Represents asset type for text samples.
     */
    TEXT_SAMPLE("text-sample");


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
