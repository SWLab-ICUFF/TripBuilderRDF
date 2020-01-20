package br.ic.uff.swlab.maketrajetorydataset;

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
import java.util.Scanner;
import java.util.zip.GZIPOutputStream;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;

public class Main {

    public static void main(String[] args) throws IOException {
        long counter = 0;
        String[] directories = {"./src/main/resources/TripBuilderDataset/florence",
            "./src/main/resources/TripBuilderDataset/pisa",
            "./src/main/resources/TripBuilderDataset/rome"};
        Map<String, POI> pois = new HashMap<>();
        Map<String, Photo> photos = new HashMap<>();
        Map<String, Cluster> clusters = new HashMap<>();
        List<Trajectory> trajectories = new ArrayList<>();

        for (String dir : directories) {
            loadPOIs(dir, pois);
            loadPhotos(dir, photos);
            loadClusters(dir, pois, clusters);
            loadTrajectories(counter, dir, trajectories, clusters);
        }

        String resourceNS = "http://localhost:8080/resource/";
        String vocabNS = "http://localhost:8080/vocab/";
        Model model = ModelFactory.createDefaultModel();
        model.setNsPrefix("voc", vocabNS);
        model.setNsPrefix("rsc", resourceNS);
        model.setNsPrefix("rdf", RDF.uri);
        model.setNsPrefix("rdfs", RDFS.uri);
        for (Trajectory t : trajectories) {
            model.createResource(resourceNS + t.userId,
                    model.createResource(vocabNS + "Human", RDFS.Class)
                            .addProperty(RDFS.subClassOf,
                                    model.createResource(vocabNS + "MovingObject", RDFS.Class)))
                    .addProperty(model.createProperty(vocabNS + "produces"),
                            model.createResource(resourceNS + "R" + t.id, model.createResource(vocabNS + "RawTrajectory"))
                                    .addProperty(model.createProperty(vocabNS + "isPartitionedInto"),
                                            model.createResource(resourceNS + t.id, model.createResource(vocabNS + "SegmentedTrajectory", RDFS.Class))
                                                    .addProperty(RDFS.comment, t.location)));

            Resource st = model.createResource(resourceNS + t.id);
            for (Stop s : t.stops) {
                st.addProperty(model.createProperty(vocabNS + "isComposedOf"),
                        model.createResource(resourceNS + s.cluster.id,
                                model.createResource(vocabNS + "Stop", RDFS.Class)
                                        .addProperty(RDFS.subClassOf, model.createResource(vocabNS + "Segment", RDFS.Class)))
                                .addProperty(model.createProperty(vocabNS + "latitude"), model.createTypedLiteral(s.cluster.latitude))
                                .addProperty(model.createProperty(vocabNS + "longitude"), model.createTypedLiteral(s.cluster.longitude)));
                Resource stop = model.createResource(resourceNS + s.cluster.id);
                for (POI poi : s.cluster.pois)
                    stop.addProperty(model.createProperty(vocabNS + "near"),
                            model.createResource(resourceNS + poi.id, model.createResource(vocabNS + "POI", RDFS.Class))
                                    .addProperty(model.createProperty(vocabNS + "latitude"), model.createTypedLiteral(poi.latitude))
                                    .addProperty(model.createProperty(vocabNS + "longitude"), model.createTypedLiteral(poi.longitude))
                                    .addProperty(RDFS.label, poi.name));
            }
        }

        try (GZIPOutputStream out = new GZIPOutputStream(
                new BufferedOutputStream(
                        new FileOutputStream("./src/main/resources/trajectory.ttl.gz")))) {
            RDFDataMgr.write(out, model, RDFFormat.TURTLE);
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
                trajectories.add(new Trajectory("T" + discriminator + counter++, location, userId, stops.toArray(new Stop[0])));
            }
        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private static void loadClusters(String dir, Map<String, POI> pois, Map<String, Cluster> clusters) {
        File file = new File(dir + "/pois-clusters.txt");
        try (Scanner scanner = new Scanner(new FileInputStream(file))) {
            if (scanner.hasNext())
                scanner.nextLine();
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

                for (int i = 0; i < fields.length; i++)
                    fields[i] = fields[i].trim();

                String[] subfields1 = fields[1].split(";");
                List<POI> pois_ = new ArrayList<>();
                for (String poiName : subfields1)
                    if (pois.containsKey(poiName))
                        pois_.add(pois.get(poiName));
                    else
                        System.out.println(poiName);

                Cluster cluster = new Cluster("S" + discriminator + fields[0],
                        pois_.toArray(new POI[0]),
                        Double.parseDouble(fields[2]),
                        Double.parseDouble(fields[3]),
                        Double.parseDouble(fields[4]),
                        fields[5].split(";"));
                clusters.put(cluster.id, cluster);
            }

        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }

    private static void loadPhotos(String dir, Map<String, Photo> photos) {
        File file = new File(dir + "/photos.txt");
        try (Scanner scanner = new Scanner(new FileInputStream(file))) {
            if (scanner.hasNext())
                scanner.nextLine();
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                String[] fields = line.split(";");
                for (int i = 0; i < fields.length; i++)
                    fields[i] = fields[i].trim();
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
            ex.printStackTrace();
        }
    }

    private static void loadPOIs(String dir, Map<String, POI> pois) {
        File file = new File(dir + "/pois.txt");
        try (Scanner scanner = new Scanner(new FileInputStream(file))) {
            if (scanner.hasNext())
                scanner.nextLine();
            while (scanner.hasNext()) {
                String line = scanner.nextLine();
                String[] fields = line.split(";");
                for (int i = 0; i < fields.length; i++)
                    fields[i] = fields[i].trim();
                String discriminator = "";
                String location = file.getParentFile().getName();
                if (location.equals("rome"))
                    discriminator = "R";
                for (int i = 0; i < fields.length; i++)
                    fields[i] = fields[i].trim();
                POI poi = new POI("P" + discriminator + fields[0],
                        fields[1],
                        Double.parseDouble(fields[2]),
                        Double.parseDouble(fields[3]));
                pois.put(poi.name, poi);
            }

        } catch (Throwable ex) {
            ex.printStackTrace();
        }
    }
}
