import sys
import lmdb
import json
import cv2 as cv
from pprint import pprint
from pose_lib import transform_pose, permute_data_list
import datetime

def get_img_datum(image_fn):
    #img = cv.imread(image_fn, cv.IMREAD_COLOR)    
    img = cv.imread(image_fn, 1)
    img = img.swapaxes(0, 2).swapaxes(1, 2)
    datum = caffe.io.array_to_datum(img, 0)
    return datum

def get_jnt_datum(joint_fn, bone_names, image_width, image_height, pose_xform, bone_weights):
    with open(joint_fn) as data_file:
        data = json.load(data_file)
    data = data["points_2d"]
    data = transform_pose(data, pose_xform, bone_names)
    
    pose_datum = caffe.io.caffe_pb2.Datum()
    pose_datum.channels = len(bone_names)*2
    pose_datum.height = 1
    pose_datum.width = 1

    weight_datum = caffe.io.caffe_pb2.Datum()
    weight_datum.channels = len(bone_names)*2
    weight_datum.height = 1
    weight_datum.width = 1

    data_list = []
    weight_list = []
    for bone_name in bone_names:
        if data[bone_name] is not None:
            data_list.append(data[bone_name][0] * 1.0 / image_width -0.5)
            data_list.append(data[bone_name][1] * 1.0 / image_height -0.5)            
            if bone_weights is None:
                weight_list.append(1.0)
                weight_list.append(1.0)
            else:                
                weight_list.append(bone_weights[bone_name])
                weight_list.append(bone_weights[bone_name])
        else:
            data_list.append(-10.0)
            data_list.append(-10.0)
            weight_list.append(0.0)
            weight_list.append(0.0)
    
    pose_datum.float_data.extend(data_list)
    weight_datum.float_data.extend(weight_list)

    return [pose_datum, weight_datum]

if __name__ == "__main__":
    with open(sys.argv[1]) as the_file:
        args = json.load(the_file)
    with open(args["data_list_file"]) as the_file:
        data_list = json.load(the_file)
    with open(args["pose_2d_config_file"]) as the_file:
        pose_config = json.load(the_file)
    with open(args["pose_xform_file"]) as the_file:
        pose_xform = json.load(the_file)["bone_xforms"]
    image_width = int(args["image_width"])
    image_height = int(args["image_height"])
    pose_lmdb_file_name = args["pose_lmdb_file"]
    weight_lmdb_file_name = args["weight_lmdb_file"]
    check_image = args["check_image"]
    bone_weights = args["bone_weights"]

    if "random_seed" in args:
        permute_data_list(data_list, args["random_seed"])
    for i in xrange(10):
        print data_list[i][0]

    if "caffe_root" in args:
        caffe_root = args["caffe_root"]
    else:
        caffe_root = "/opt/caffe"
    sys.path.insert(0, caffe_root + "/python")
    import caffe

    bone_names = pose_config["bone_names"]
    data_count = len(data_list)

    jnt_env = lmdb.Environment(pose_lmdb_file_name, map_size=1099511627776)
    jnt_txn = jnt_env.begin(write=True, buffers=True)

    weight_env = lmdb.Environment(weight_lmdb_file_name, map_size=1099511627776)
    weight_txn = weight_env.begin(write=True, buffers=True)
    
    skipped = []
    last_time = datetime.datetime.now()
    for i in xrange(data_count):                
        image_file = data_list[i][0]
        bone_file = data_list[i][1]
        key = "%010d" % i
    
        try:
            if check_image:
                img_datum = get_img_datum(image_file)
            
            [jnt_datum, weight_datum] = get_jnt_datum(bone_file, bone_names, image_width, image_height, pose_xform, bone_weights)
            jnt_txn.put(key, jnt_datum.SerializeToString())
            weight_txn.put(key, weight_datum.SerializeToString())
        except:
            print "Skipped example", i, "because of some error."
            skipped.append(i)        

        if i % 100 == 0:
            delta = datetime.datetime.now()- last_time 
            last_time = datetime.datetime.now()
            print "Processed %d entries ..." % i, "elasped time =", delta

        if i % 1000 == 0:            
            start = datetime.datetime.now()
            
            jnt_txn.commit()
            jnt_txn = jnt_env.begin(write=True, buffers=True)            

            weight_txn.commit()
            weight_txn = weight_env.begin(write=True, buffers=True)

            delta =  datetime.datetime.now() - start
            last_time = datetime.datetime.now()
            print "Committed %d entries ..." % i, "elasped time =", delta
    
    jnt_txn.commit()
    jnt_env.close()

    weight_txn.commit()
    weight_env.close()

    print skipped