caffe_root = "/opt/caffe/"
import sys
sys.path.insert(0, caffe_root + "python")

import lmdb
import caffe
import json
import cv2 as cv
from pprint import pprint

def get_image_size(image_fn):
    img = cv.imread(image_fn, cv.IMREAD_COLOR)    
    height,width,depth = img.shape
    return height

def get_img_datum(image_fn):
    img = cv.imread(image_fn, cv.IMREAD_COLOR)    
    img = img.swapaxes(0, 2).swapaxes(1, 2)
    datum = caffe.io.array_to_datum(img, 0)
    return datum

def get_jnt_datum(joint_fn, bone_names, image_size):
    with open(joint_fn) as data_file:
        data = json.load(data_file)
    data = data["points_2d"]
    datum = caffe.io.caffe_pb2.Datum()
    datum.channels = len(bone_names)*2
    datum.height = 1
    datum.width = 1
    data_list = []
    for bone_name in bone_names:
        data_list.append(data[bone_name][0] * 1.0 / image_size -0.5)
        data_list.append(data[bone_name][1] * 1.0 / image_size -0.5)
    datum.float_data.extend(data_list)
    return datum

if __name__ == "__main__":
    src_dir = sys.argv[1]
    data_count = int(sys.argv[2])
    limit = int(sys.argv[3])
    bones_file_name = sys.argv[4]
    image_lmdb_file_name = sys.argv[5]
    bone_lmdb_file_name = sys.argv[6]

    print "source dir:", src_dir
    print "data file count:", data_count
    print "per file limit:", limit
    print "bones file name:", bones_file_name
    print "image lmdb file name:", image_lmdb_file_name
    print "bone lmdb file name:", bone_lmdb_file_name    

    bone_names = []
    with open(bones_file_name) as fin:
        bone_count = int(fin.readline())
        for i in xrange(bone_count):
            bone_name = fin.readline().strip()
            bone_names.append(bone_name)

    img_env = lmdb.Environment(image_lmdb_file_name, map_size=1099511627776)
    img_txn = img_env.begin(write=True, buffers=True)   
    jnt_env = lmdb.Environment(bone_lmdb_file_name, map_size=1099511627776)
    jnt_txn = jnt_env.begin(write=True, buffers=True)

    image_size = 0
    skipped = []
    for i in xrange(data_count):        
        dir_index = i / limit       
        image_file = "%s/data-%05d/data_%08d.png" % (src_dir, dir_index, i)
        bone_file = "%s/data-%05d/data_%08d_data.txt" % (src_dir, dir_index, i)

        if i == 0:
            image_size = get_image_size(image_file)
            print "image size: ", image_size

        #img_datum = get_img_datum(image_file)
        #jnt_datum = get_jnt_datum(bone_file, bone_names, image_size)
        #key = "%010d" % i        

        #img_txn.put(key, img_datum.SerializeToString())
        #jnt_txn.put(key, jnt_datum.SerializeToString())

        try:
            img_datum = get_img_datum(image_file)
            jnt_datum = get_jnt_datum(bone_file, bone_names, image_size)
            key = "%010d" % i        

            img_txn.put(key, img_datum.SerializeToString())
            jnt_txn.put(key, jnt_datum.SerializeToString())
        except:
            skipped.append(i)
            print "Skipped example", i, "because of some error."            

        if i % 100 == 0:
            img_txn.commit()
            jnt_txn.commit()
            jnt_txn = jnt_env.begin(write=True, buffers=True)
            img_txn = img_env.begin(write=True, buffers=True)
            print "Committed %d entries ..." % i

    img_txn.commit()
    jnt_txn.commit()
    img_env.close()
    jnt_env.close()

    print skipped