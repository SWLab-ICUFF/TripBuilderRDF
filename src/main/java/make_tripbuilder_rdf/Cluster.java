package make_tripbuilder_rdf;

public class Cluster {

    public String id;
    public POI[] pois;
    public double latitude;
    public double longitude;
    public double time;
    public String[] categories;

    public Cluster(String id, POI[] pois, double latitude, double longitude, double time, String[] categories) {
        this.id = id;
        this.pois = pois;
        this.latitude = latitude;
        this.longitude = longitude;
        this.time = time;
        this.categories = categories;
    }

}
