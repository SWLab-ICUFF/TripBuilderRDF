package br.ic.uff.swlab.maketrajetorydataset;

import java.util.Date;

public class Photo {

    public String id;
    public String userid;
    public Date datetaken;
    public long dateupload;
    public double latitude;
    public double longitude;

    public Photo(String userid, String id, Date datetaken, long dateupload, double latitude, double longitude) {
        this.id = id;
        this.userid = userid;
        this.datetaken = datetaken;
        this.dateupload = dateupload;
        this.latitude = latitude;
        this.longitude = longitude;
    }

}
