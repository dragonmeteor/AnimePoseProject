package yumyai.poseest.mmd.motion

class RenderAnimationFramesNoPhysicsFromConfig {
    public static void main(String[] args) {
        if (args.length < 1) {
            println("Usage: java yumyai.poseest.mmd.motion.RenderAnimationFramesNoPhysicsFromConfig <config-file>")
            System.exit(0)
        }

        ArrayList<String> arguments = new ArrayList<String>()
        new File(args[0]).withReader("utf-8") { f ->
            String line = f.readLine()
            while (line != null) {
                arguments.add(line)
                line = f.readLine()
            }
        }
        String[] argArray = new String[arguments.size()]
        for (int i = 0; i < arguments.size(); i++) {
            argArray[i] = arguments[i]
        }

        RenderAnimationFramesNoPhysics.main(argArray);
    }
}
