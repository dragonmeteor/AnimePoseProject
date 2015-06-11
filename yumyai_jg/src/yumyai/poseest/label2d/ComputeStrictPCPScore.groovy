package yumyai.poseest.label2d

import groovy.json.JsonBuilder
import yondoko.util.ArgumentProcessor
import org.apache.commons.lang3.tuple.Pair
import yondoko.util.JsonUtil

import javax.vecmath.Point2f;


class ComputeStrictPCPScore {
    public static void main(String[] args) {
        if (args.length < 4) {
            println("java yumyai.poseest.label2d.ComputeStrictPCPScore <predict-file> <answer-file> " +
                    "<config-file> <output-file> [threshold]");
            System.exit(0);
        }

        ArgumentProcessor argProc = new ArgumentProcessor(args);
        String predictFileName = argProc.getString();
        String answerFileName = argProc.getString();
        String configFileName = argProc.getString();
        String outputFileName = argProc.getString();
        float threshold = 0.5f;
        if (argProc.getNumRemainingArguments() >= 1) {
            threshold = argProc.getFloat();
        }

        ArrayList<PoseLabeling2D> predictions = PoseLabeling2D.load(predictFileName);
        ArrayList<PoseLabeling2D> answers = PoseLabeling2D.load(answerFileName);
        Pose2DConfig config = Pose2DConfig.load(configFileName);

        predictions.sort { it.fileName };
        answers.sort { it.fileName };

        HashMap<Pair<String, String>, Float> fraction = new HashMap<Pair<String, String>, Float>();
        for (Pair<String,String> edge : config.edges) {
            fraction[edge] = 0.0f;
        }

        int exampleCount = 0;
        int answerIndex = 0;
        for (int predictionIndex = 0; predictionIndex < predictions.size(); predictionIndex++) {
            PoseLabeling2D prediction = predictions[predictionIndex];
            while (answerIndex < answers.size() && !answers[answerIndex].fileName.equals(prediction.fileName)) {
                answerIndex++;
            }
            if (answerIndex >= answers.size()) {
                break;
            }
            PoseLabeling2D answer = answers[answerIndex];
            exampleCount++;

            for (Pair<String,String> edge : config.edges) {
                String parent = edge.left;
                String child = edge.right;
                Point2f a0 = answer.points[parent];
                Point2f a1 = answer.points[child];
                Point2f p0 = prediction.points[parent];
                Point2f p1 = prediction.points[child];

                float boneLength = a0.distance(a1);
                float d0 = p0.distance(a0);
                float d1 = p1.distance(a1);
                if (d0 <= threshold*boneLength && d1 <= threshold*boneLength) {
                    fraction[edge] = fraction[edge] + 1;
                }
            }
        }

        for (Pair<String,String> edge : config.edges) {
            fraction[edge] = fraction[edge] / exampleCount;
        }

        def output = [];
        for (Pair<String,String> edge : config.edges) {
            def item = [:];
            item["edge"] = [edge.left, edge.right];
            item["score"] = fraction[edge];
            output.add(item);
        }
        new File(outputFileName).write(new JsonBuilder(output).toPrettyString())
    }
}
