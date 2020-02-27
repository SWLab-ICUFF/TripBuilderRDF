package tripbuilder.make;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Scanner;
import java.util.zip.GZIPOutputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

public class Main_IJGIS {

    public static void main(String[] args) throws IOException {

        String[] directories = {"./src/main/resources/TripBuilderRawData/florence",
            "./src/main/resources/TripBuilderRawData/rome",
            "./src/main/resources/TripBuilderRawData/pisa"
        };
        Map<String, POI> pois = new HashMap<>();
        Map<String, Photo> photos = new HashMap<>();
        Map<String, Cluster> clusters = new HashMap<>();
        List<Trajectory> trajectories = new ArrayList<>();

        for (String dir : directories) {
            long counter = 0;
            loadPOIs(dir, pois);
            loadPhotos(dir, photos);
            loadClusters(dir, pois, clusters);
            loadTrajectories(counter, dir, trajectories, clusters);
        }
        Model model = ModelFactory.createDefaultModel();
        triplificationProcess(model, trajectories);
        try (GZIPOutputStream out = new GZIPOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream("./src/main/resources/trajectory_ijgis.ttl.gz")))) {
            RDFDataMgr.write(out, model, RDFFormat.TURTLE);
        }
    }

    private static void triplificationProcess(Model model, List<Trajectory> trajectories) {
        String resourceNS = "http://localhost:8080/resource/";
        String vocabNS = "http://localhost:8080/vocab/";

        model.setNsPrefix("voc", vocabNS);
        model.setNsPrefix("rsc", resourceNS);
        model.setNsPrefix("rdf", RDF.uri);
        model.setNsPrefix("rdfs", RDFS.uri);

        List<Resource> transportationResource = createTransportationResouces(model, resourceNS, vocabNS);

        for (Trajectory t : trajectories) {
            Resource st = model.createResource(resourceNS + t.id);
            st.addProperty(model.createProperty(vocabNS + "begins"), model.createResource(resourceNS + t.id + "_" + t.getBegin().cluster.id));
            st.addProperty(model.createProperty(vocabNS + "ends"), model.createResource(resourceNS + t.id + "_" + t.getEnd().cluster.id));
            st.addProperty(model.createProperty(vocabNS + "length"), model.createTypedLiteral(t.stops.length));

            for (int i = 0; i < t.stops.length; i++) {
                Stop s = t.stops[i];
                Resource stop = model.createResource(resourceNS + t.id + "_" + s.cluster.id);
                stop.addProperty(model.createProperty(vocabNS + "latitude"), model.createTypedLiteral(s.cluster.latitude));
                stop.addProperty(model.createProperty(vocabNS + "longitude"), model.createTypedLiteral(s.cluster.longitude));

                for (POI poi : s.cluster.pois) {
                    Resource p = model.createResource(resourceNS + poi.id);
                    p.addProperty(model.createProperty(vocabNS + "latitude"), model.createTypedLiteral(poi.latitude));
                    p.addProperty(RDFS.label, poi.name);
                    p.addProperty(model.createProperty(vocabNS + "longitude"), model.createTypedLiteral(poi.longitude));
                    p.addProperty(model.createProperty(vocabNS + "locatedIn"), t.location);

                    for (String category : s.cluster.categories) {
                        p.addProperty(model.createProperty(vocabNS + "category"), category);
                    }
                    model.add(p, RDF.type, model.createResource(vocabNS + "POI", RDFS.Class));

                    stop.addProperty(model.createProperty(vocabNS + "enrichedBy"), p);
                }
                if (i + 1 < t.stops.length) {
                    Stop next = t.stops[i + 1];
                    Resource to = model.createResource(resourceNS + t.id + "_" + next.cluster.id);
                    stop.addProperty(model.createProperty(vocabNS + "next"), to);

                    Resource move = model.createResource(resourceNS + t.id + "_" + s.cluster.id + "_" + next.cluster.id);
                    move.addProperty(model.createProperty(vocabNS + "from"), stop);
                    move.addProperty(model.createProperty(vocabNS + "to"), to);
                    move.addProperty(model.createProperty(vocabNS + "move_number"), model.createTypedLiteral(i + 1));
                    move.addProperty(model.createProperty(vocabNS + "enrichedBy"), getRandomlyTransportation(transportationResource));
                    model.add(move, RDF.type, model.createResource(vocabNS + "Move", RDFS.Class));
                    st.addProperty(model.createProperty(vocabNS + "has"), move);
                }
                st.addProperty(model.createProperty(vocabNS + "has"), stop);
                model.add(stop, RDF.type, model.createResource(vocabNS + "Stop", RDFS.Class));
            }
            model.add(st, RDF.type, model.createResource(vocabNS + "Trajectory", RDFS.Class));
        }
    }

    private static void loadTrajectories(long counter, String dir, List<Trajectory> trajectories, Map<String, Cluster> clusters) {
        File file = new File(dir + "/trajectories.txt");
        try (Scanner scanner = new Scanner(new FileInputStream(file))) {
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                String[] fields = line.split("\t");
                String discriminator = "";
                String location = file.getParentFile().getName();
                switch (location) {
                    case "pisa":
                        discriminator = "P";
                        break;
                    case "florence":
                        discriminator = "F";
                        break;
                    case "rome":
                        discriminator = "R";
                        break;
                }
                String userId = "U" + discriminator + fields[0];
                List<Stop> stops = new ArrayList<>();
                for (int i = 1; i < fields.length; i++) {
                    String[] subfields = fields[i].split(";");

                    Stop stop = new Stop(clusters.get("S" + discriminator + Long.parseLong(subfields[0])),
                            Integer.parseInt(subfields[1]),
                            Long.parseLong(subfields[2]),
                            Long.parseLong(subfields[3]));
                    stops.add(stop);
                }
                Trajectory current = new Trajectory("T" + discriminator + ++counter, location, userId, stops.toArray(new Stop[0]));
                current.setBegin(stops.get(0));
                current.setEnd(stops.get(stops.size() - 1));
                trajectories.add(current);
            }
        } catch (Throwable ex) {
            System.err.println(ex.getMessage());
        }
    }

    private static void loadClusters(String dir, Map<String, POI> pois, Map<String, Cluster> clusters) {
        File file = new File(dir + "/pois-clusters.txt");
        try (Scanner scanner = new Scanner(new FileInputStream(file))) {
            if (scanner.hasNext()) {
                scanner.nextLine();
            }
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                String[] fields = line.split(",");
                String discriminator = "";
                String location = file.getParentFile().getName();
                switch (location) {
                    case "pisa":
                        discriminator = "P";
                        break;
                    case "florence":
                        discriminator = "F";
                        break;
                    case "rome":
                        discriminator = "R";
                        break;
                }

                for (int i = 0; i < fields.length; i++) {
                    fields[i] = fields[i].trim();
                }

                String[] subfields1 = fields[1].split(";");
                List<POI> pois_ = new ArrayList<>();
                for (String poiName : subfields1) {
                    if (pois.containsKey(poiName)) {
                        pois_.add(pois.get(poiName));
                    } else {
                        System.out.println(poiName);
                    }
                }

                Cluster cluster = new Cluster("S" + discriminator + fields[0],
                        pois_.toArray(new POI[0]),
                        Double.parseDouble(fields[2]),
                        Double.parseDouble(fields[3]),
                        Double.parseDouble(fields[4]),
                        fields[5].split(";"));
                clusters.put(cluster.id, cluster);
            }

        } catch (Throwable ex) {
            System.err.println(ex.getMessage());
        }
    }

    private static void loadPhotos(String dir, Map<String, Photo> photos) {
        File file = new File(dir + "/photos.txt");
        try (Scanner scanner = new Scanner(new FileInputStream(file))) {
            if (scanner.hasNext()) {
                scanner.nextLine();
            }
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                String[] fields = line.split(";");
                for (int i = 0; i < fields.length; i++) {
                    fields[i] = fields[i].trim();
                }
                String discriminator = "";
                String location = file.getParentFile().getName();
                switch (location) {
                    case "pisa":
                        discriminator = "P";
                        break;
                    case "florence":
                        discriminator = "F";
                        break;
                    case "rome":
                        discriminator = "R";
                        break;
                }
                String userId = "U" + discriminator + fields[0];
                String photoId = "PH" + Long.parseLong(fields[1]);
                Date dateTaken = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(fields[2]);
                Long dateUploaded = Long.parseLong(fields[3]);
                Double latitude = Double.parseDouble(fields[4]);
                Double longitude = Double.parseDouble(fields[5]);
                Photo photo = new Photo(userId, photoId, dateTaken, dateUploaded, latitude, longitude);
                photos.put(photo.id, photo);
            }

        } catch (Throwable ex) {
            System.err.println(ex.getMessage());
        }
    }

    private static void loadPOIs(String dir, Map<String, POI> pois) {
        File file = new File(dir + "/pois.txt");
        try (Scanner scanner = new Scanner(new FileInputStream(file))) {
            if (scanner.hasNext()) {
                scanner.nextLine();
            }
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                String[] fields = line.split(";");
                for (int i = 0; i < fields.length; i++) {
                    fields[i] = fields[i].trim();
                }
                String discriminator = "";
                String location = file.getParentFile().getName();
                if (location.equals("rome")) {
                    discriminator = "R";
                }
                for (int i = 0; i < fields.length; i++) {
                    fields[i] = fields[i].trim();
                }
                POI poi = new POI("P" + discriminator + fields[0],
                        fields[1],
                        Double.parseDouble(fields[2]),
                        Double.parseDouble(fields[3]));
                pois.put(poi.name, poi);
            }

        } catch (Throwable ex) {
            System.err.println(ex.getMessage());
        }
    }

    private static List<Resource> createTransportationResouces(Model model, String resourceNS, String vocabNS) {
        String[] transportationWays = new String[]{"Walk", "Bus", "Taxi", "Subway"};
        List<Resource> transportation = new ArrayList<>();
        for (String s : transportationWays) {
            Resource r = model.createResource(resourceNS + s);
            r.addProperty(RDFS.label, s.toLowerCase());
            model.add(r, RDF.type, model.createResource(vocabNS + "Transportation", RDFS.Class));
            transportation.add(r);
        }
        return transportation;
    }

    private static Resource getRandomlyTransportation(List<Resource> transportation) {
        Random r = new Random();
        int randomIndex = r.nextInt(transportation.size());
        return transportation.get(randomIndex);
    }

}
