package br.ic.uff.swlab.make_tripbuilder_rdf;

public class Trajectory {

    public String id;
    public String location;
    public String userId;
    public Stop[] stops;
    
    private Stop begin,end;

    public Trajectory(String id, String location, String userId, Stop[] stops) {
        this.id = id;
        this.location = location;
        this.userId = userId;
        this.stops = stops;
    }

    /**
     * @return the begin
     */
    public Stop getBegin() {
        return begin;
    }

    /**
     * @param begin the begin to set
     */
    public void setBegin(Stop begin) {
        this.begin = begin;
    }

    /**
     * @return the end
     */
    public Stop getEnd() {
        return end;
    }

    /**
     * @param end the end to set
     */
    public void setEnd(Stop end) {
        this.end = end;
    }

}
