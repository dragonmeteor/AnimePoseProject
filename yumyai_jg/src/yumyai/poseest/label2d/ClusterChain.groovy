package yumyai.poseest.label2d

import groovy.json.JsonBuilder
import groovy.json.JsonSlurper

import javax.imageio.ImageIO
import javax.vecmath.Vector2d
import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Line2D
import java.awt.image.BufferedImage

class ClusterChain {
    public static double distance(ArrayList<Vector2d> p, ArrayList<Vector2d> q) {
        double sum = 0;
        for (int i = 0; i < p.size(); i++) {
            double dx = p[i].x - q[i].x;
            double dy = p[i].y - q[i].y;
            sum += dx*dx + dy*dy;
        }
        return Math.sqrt(sum);
    }

    public static void main(String[] _args) {
        if (_args.length < 1) {
            println("Usage: java yumyai.poseest.label2d.ClusterChain <args-file>");
            System.exit(0);
        }

        def args = new JsonSlurper().parse(new File(_args[0]));
        def dataList = new JsonSlurper().parse(new File(args["data_list_file"]));
        def chain = args["chain"];
        String outputFilePrintf = args["output_file_printf"];
        int chainLength = chain.size();
        long randomSeed = args["random_seed"] as long;
        int kmeansIterations = args["kmeans_iterations"];
        int clusterCount = args["cluster_count"];
        String clusterInfoFileName = args["cluster_info_file"];

        Vector2d[] chainPoints = new Vector2d[chainLength];
        for (int i = 0; i < chainLength; i++) {
            chainPoints[i] = new Vector2d();
        }

        for (int i = 0; i < dataList.size(); i++) {
            def item = dataList[i];
            def points = new JsonSlurper().parse(new File(item[1]))["points_2d"];
            ArrayList<Vector2d> coords = new ArrayList<Vector2d>();
            def p0 = points[chain[0]];
            Vector2d center = new Vector2d(p0[0] as double, p0[1] as double);
            double sum = 0.0;
            for (int j = 1; j < chainLength; j++) {
                def p = points[chain[j]];
                Vector2d pp = new Vector2d(p[0] as double, p[1] as double);
                pp.sub(center);
                coords.add(pp);
                sum += pp.lengthSquared();
            }
            sum /= chainLength-1;
            double factor = Math.sqrt(sum);
            for (int j = 0; j < chainLength-1; j++) {
                coords[j].scale(1.0 / factor);
            }
            item.add(coords);

            if ((i+1) % 100 == 0)
                println("Read ${i+1} files ...");
        }

        // Find the centers, randomly.
        ArrayList<ArrayList<Vector2d>> centers = new ArrayList<ArrayList<Vector2d>>();
        ArrayList<Integer> clusterSize = new ArrayList<Integer>();
        Random random = new Random();
        random.setSeed(randomSeed);
        int current = dataList.size()-1;
        for (int i = 0; i < clusterCount; i++) {
            clusterSize.add(0);

            int index = random.nextInt(current+1);
            def temp = dataList[index];
            dataList[index] = dataList[current];
            dataList[current] = temp;

            ArrayList<Vector2d> newCenter = new ArrayList<Vector2d>();
            for (int j = 0; j < chainLength - 1; j++) {
                newCenter.add(dataList[current][2][j].clone());
            }
            centers.add(newCenter);

            current--;
        }

        ArrayList<Integer> clusterIndex = new ArrayList<Integer>();

        for (int i = 0; i < dataList.size(); i++) {
            clusterIndex.add(0);
        }


        // K-means
        for (int iterIndex = 0; iterIndex < kmeansIterations; iterIndex++) {
            // Identify the closest cluster
            for (int i = 0; i < dataList.size(); i++) {
                double minDistance = Double.MAX_VALUE;
                int minIndex = 0;
                for (int j = 0; j < clusterCount; j++) {
                    double d = distance(centers[j], dataList[i][2]);
                    if (minDistance > d) {
                        minDistance = d;
                        minIndex = j;
                    }
                }
                clusterIndex[i] = minIndex;
            }

            // Find the cluster means.
            for (int i = 0; i < clusterCount; i++) {
                clusterSize[i] = 0;
                for (int j = 0; j < chainLength - 1; j++) {
                    centers[i][j].set(0.0, 0.0);
                }
            }
            for (int i = 0; i < dataList.size(); i++) {
                ArrayList<Vector2d> p = dataList[i][2];
                int index = clusterIndex[i];
                clusterSize[index]++;
                for (int j = 0; j < chainLength - 1; j++) {
                    centers[index][j].add(p[j]);
                }
            }
            for (int i = 0; i < clusterCount; i++) {
                for (int j = 0; j < chainLength - 1; j++) {
                    centers[i][j].scale(1.0 / clusterSize[i]);
                }
            }

            println("Completed ${iterIndex} iterations of K-means ...");
        }

        for (int cluster = 0; cluster < clusterCount; cluster++) {
            String outputFileName = String.format(outputFilePrintf, cluster);
            File dir = new File(outputFileName).parentFile;
            dir.mkdirs();

            def output = []
            for (int i = 0; i < dataList.size(); i++) {
                if (clusterIndex[i] == cluster) {
                    output.add([dataList[i][0], dataList[i][1]]);
                }
            }

            print("Writing to ${outputFileName} ... ");
            new File(outputFileName).withWriter({ fout ->
                fout.write(new JsonBuilder(output).toPrettyString());
            });
            println("DONE");
        }

        println(clusterInfoFileName);
        def output = []
        for (int i = 0; i < clusterCount; i++) {
            output.add([:])
            output[i]["index"] = i;
            output[i]["size"] = clusterSize[i];
            output[i]["center"] = []
            for (int j = 0; j < chainLength - 1; j++) {
                output[i]["center"].add([centers[i][j].x, centers[i][j].y]);
            }
        }
        new File(clusterInfoFileName).withWriter({ fout ->
            fout.write(new JsonBuilder(output).toPrettyString());
        })
    }
}
