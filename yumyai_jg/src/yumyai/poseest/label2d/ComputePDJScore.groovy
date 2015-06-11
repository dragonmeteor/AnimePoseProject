package yumyai.poseest.label2d

import groovy.json.JsonBuilder
import org.apache.commons.lang3.tuple.Pair
import yondoko.util.ArgumentProcessor

import javax.vecmath.Point2f

class ComputePDJScore {
    public static void main(String[] args) {
        if (args.length < 0) {
            print("java yumyai.poseest.label2d.ComputePDFScore <prediction-file> <answer-file> " +
                    "<pose-config> <torso-config> <output-file> [start] [end] [count]");
            System.exit(0);
        }

        ArgumentProcessor argProc = new ArgumentProcessor(args);
        String predictionFileName = argProc.getString();
        String answerFileName = argProc.getString();
        String poseConfigFileName = argProc.getString();
        String torsoConfigFileName = argProc.getString();
        String outputFileName = argProc.getString();
        float startThreshold = 0;
        float endThreshold = 0.2;
        int thresholdCount = 40;

        ArrayList<PoseLabeling2D> predictions = PoseLabeling2D.load(predictionFileName);
        ArrayList<PoseLabeling2D> answers = PoseLabeling2D.load(answerFileName);
        Pose2DConfig poseConfig = Pose2DConfig.load(poseConfigFileName);

        String torsoBone0, torsoBone1;
        new File(torsoConfigFileName).withReader({ fin ->
            torsoBone0 = fin.readLine().trim();
            torsoBone1 = fin.readLine().trim();
        })

        if (argProc.hasArguments()) {
            startThreshold = argProc.getFloat();
        }
        if (argProc.hasArguments()) {
            endThreshold = argProc.getFloat();
        }
        if (argProc.hasArguments()) {
            thresholdCount = argProc.getInt();
        }

        ArrayList<Float> thresholds = new ArrayList<Float>();
        for (int i = 0; i <= thresholdCount; i++) {
            thresholds.add(startThreshold + (endThreshold - startThreshold) * 1.0f * i / thresholdCount);
        }

        HashMap<String, ArrayList<Float>> pdj = new HashMap<String, ArrayList<Float>>();
        for (String boneName : poseConfig.boneNames) {
            pdj[boneName] = new ArrayList<Float>();
            for (int i = 0; i <= thresholdCount; i++) {
                pdj[boneName].add(0);
            }
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

            float torsoLength = answer.points[torsoBone0].distance(answer.points[torsoBone1]);

            for (String boneName : poseConfig.boneNames) {
                Point2f actual = prediction.points[boneName]
                Point2f expected = answer.points[boneName];
                float d = actual.distance(expected);

                for (int i = 0; i < thresholds.size(); i++) {
                    if (d < thresholds[i]*torsoLength) {
                        pdj[boneName][i] += 1;
                    }
                }
            }
        }

        for (String boneName : poseConfig.boneNames) {
            for (int i = 0; i < thresholds.size(); i++) {
                pdj[boneName][i] /= exampleCount;
            }
        }

        def output = [:]
        for (String boneName : poseConfig.boneNames) {
            def item = []
            output[boneName] = item
            for (int i = 0; i < thresholds.size(); i++) {
                item.add([thresholds[i], pdj[boneName][i]])
            }
        }
        new File(outputFileName).write(new JsonBuilder(output).toPrettyString());
    }
}
