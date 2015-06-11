package yumyai.poseest.mmd.character

class CopyAndConvertMmdModelTextureToPngFromList {
    public static void main(String[] args) {
        if (args.length < 3) {
            println("Usage: java yumyai.poseest.mmd.character.CopyAndConvertMmdModelTextureToPngFromList <list-file> " +
                    "<old-prefix> <new-prefix>")
            System.exit(0);
        }

        final def fileNames = new ArrayList<String>()
        final def fileFlags = new ArrayList<Integer>()
        new File(args[0]).withReader("UTF-8") { fin ->
            def lines = fin.readLines()
            for (int i = 0; i < lines.size(); i++) {
                if (lines[i].length() > 0) {
                    fileNames.add(lines[i].substring(2).trim());
                    fileFlags.add(Integer.valueOf(lines[i].substring(0, 1)))
                }
            }
        }

        String oldPrefix = args[1]
        String newPrefix = args[2]

        long start = System.nanoTime()
        for (int i = 0; i < fileNames.size(); i++) {
            if (fileFlags[i] != 1) {
                String sourceFile = fileNames[i]
                String destFile = newPrefix + sourceFile.substring(oldPrefix.length());
                if (new File(destFile).exists()) {
                    println("File " + destFile + " already exists.")
                    continue
                }
                String[] a = new String[2]
                a[0] = sourceFile
                a[1] = destFile
                new CopyAndConvertMmdModelTextureToPng().run(a)
                println()
            }

            println("Done ${i+1} out of ${fileNames.size()}")
            println("Elasped time = ${(System.nanoTime() - start)*1e-9} seconds")
            println()
        }
    }
}
