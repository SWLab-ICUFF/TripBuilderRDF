package br.ic.uff.swlab.make_tripbuilder_rdf;

public class Stop {

    public Cluster cluster;
    public int numPhotos;
    public long begin;
    public long end;

    public Stop(Cluster clus, int numPhotos, long begin, long end) {
        this.cluster = clus;
        this.numPhotos = numPhotos;
        this.begin = begin;
        this.end = end;
    }

    public String toString() {
        return cluster + ", " + numPhotos + ", " + begin + ", " + end;
    }
}
