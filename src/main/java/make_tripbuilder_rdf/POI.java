package br.ic.uff.swlab.make_tripbuilder_rdf;

public class POI {

    public String id;
    public String name;
    public double latitude;
    public double longitude;

    public POI(String id, String name, double latitude, double longitude) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

}
