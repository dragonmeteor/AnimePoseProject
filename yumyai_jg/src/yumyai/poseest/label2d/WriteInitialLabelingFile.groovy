package yumyai.poseest.label2d

import yondoko.util.JsonUtil

class WriteInitialLabelingFile {
    public static void main(String[] args) {
        if (args.length < 1) {
            println("Usage: java yumyai.poseest.label2d.WriteInitialLabelingFile <args-file>")
            println("Arguments: image_list_file, output_file")
            System.exit(0);
        }

        def arguments = JsonUtil.load(args[0])
        def imageList = JsonUtil.load(arguments["image_list_file"])
        ArrayList<PoseLabeling2D> labelings = new ArrayList<PoseLabeling2D>();
        for (def fileName : imageList) {
            labelings.add(new PoseLabeling2D(fileName));
        }
        PoseLabeling2D.save(arguments["output_file"], labelings);
    }
}
