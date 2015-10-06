
import json
import sys

def get_jnt_datum(joint_fn, bone_names, image_size):    
    with open(joint_fn) as data_file:
        data = json.load(data_file)
    data = data["points_2d"]        
    for bone_name in bone_names:
        x = data[bone_name][0] * 1.0 / image_size -0.5
        y = data[bone_name][1] * 1.0 / image_size -0.5
        data[bone_name] = [x,y]
    return data

if __name__ == "__main__":
    src_dir = sys.argv[1]
    data_count = int(sys.argv[2])
    limit = int(sys.argv[3])
    bones_file_name = sys.argv[4]
    image_size = int(sys.argv[5])
    output_file = sys.argv[6]

    print "source dir:", src_dir
    print "data file count:", data_count
    print "per file limit:", limit
    print "bones file name:", bones_file_name
    print "image size:", image_size   
    print "output file name:", output_file

    bone_names = []
    avgPos = {}
    with open(bones_file_name) as fin:
        bone_count = int(fin.readline())
        for i in xrange(bone_count):
            bone_name = fin.readline().strip()
            bone_names.append(bone_name)
            avgPos[bone_name] = [0.0,0.0]

    print "Computing the sum ..."
    count = 0
    for i in xrange(data_count):
        dir_index = i / limit
        bone_file = "%s/data-%05d/data_%08d_data.txt" % (src_dir, dir_index, i)
        try:
            datum = get_jnt_datum(bone_file, bone_names, image_size)            
            for bone_name in bone_names:
                sumPos = avgPos[bone_name]
                pos = datum[bone_name]
                sumPos[0] += pos[0]
                sumPos[1] += pos[1]         
        except ValueError:
            print "Skip example", i, "due to some error"

        count += 1
        if count % 100 == 0:
            print "Processed", count, "examples"
    
    for bone_name in bone_names:
        avgPos[bone_name][0] /= count * 1.0
        avgPos[bone_name][1] /= count * 1.0

    print "Computing the loss ..."
    count = 0
    error = 0
    for i in xrange(data_count):
        dir_index = i / limit
        bone_file = "%s/data-%05d/data_%08d_data.txt" % (src_dir, dir_index, i)
        try:
            datum = get_jnt_datum(bone_file, bone_names, image_size)
            for bone_name in bone_names:
                pos = datum[bone_name]                
                dx = avgPos[bone_name][0] - pos[0]
                dy = avgPos[bone_name][1] - pos[1]
                error += dx*dx + dy*dy
        except ValueError:
            print "Skip example", i, "due to some error"

        count += 1
        if count % 100 == 0:
            print "Processed", count, "examples"

    error /= count * 2
    fout = open(output_file, "w")
    fout.write("%f\n" % error)
    fout.close()