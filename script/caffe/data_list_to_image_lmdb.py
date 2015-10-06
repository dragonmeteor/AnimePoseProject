import sys
import lmdb
import json
import cv2 as cv
from pprint import pprint
from pose_lib import permute_data_list
import datetime

def get_img_datum(image_fn):
    #img = cv.imread(image_fn, cv.IMREAD_COLOR)    
    img = cv.imread(image_fn, 1)
    img = img.swapaxes(0, 2).swapaxes(1, 2)
    datum = caffe.io.array_to_datum(img, 0)
    return datum

if __name__ == "__main__":
    with open(sys.argv[1]) as the_file:
        args = json.load(the_file)
    with open(args["data_list_file"]) as the_file:
        data_list = json.load(the_file)
    image_lmdb_file_name = args["output_file"]

    if "caffe_root" in args:
        caffe_root = args["caffe_root"]
    else:
        caffe_root = "/opt/caffe"
    sys.path.insert(0, caffe_root + "/python")
    import caffe

    if "random_seed" in args:
        permute_data_list(data_list, args["random_seed"])
    for i in xrange(10):
        print data_list[i][0]

    img_env = lmdb.Environment(image_lmdb_file_name, map_size=1099511627776)
    img_txn = img_env.begin(write=True, buffers=True)       

    image_size = 0
    skipped = []
    data_count = len(data_list)
    last_time = datetime.datetime.now()
    for i in xrange(data_count):
        key = "%010d" % i
        image_file = data_list[i][0]
        pose_file = data_list[i][1]

        if True:
            #print image_file
            img_datum = get_img_datum(image_file)
            with open(pose_file) as the_file:
                pose = json.load(the_file)
            img_txn.put(key, img_datum.SerializeToString())
        else:
            try:
                img_datum = get_img_datum(image_file)
                with open(pose_file) as the_file:
                    pose = json.load(the_file)
                img_txn.put(key, img_datum.SerializeToString())
            except:
                print "Skipped example", i, "because of some error."
                skipped.append(i)
            
        if i % 100 == 0:
            delta = datetime.datetime.now()- last_time 
            last_time = datetime.datetime.now()
            print "Processed %d entries ..." % i, "elasped time =", delta

        if i % 1000 == 0:
            start = datetime.datetime.now()
            img_txn.commit()            
            img_txn = img_env.begin(write=True, buffers=True)
            delta =  datetime.datetime.now() - start
            last_time = datetime.datetime.now()
            print "Committed %d entries ..." % i, "elasped time =", delta

    img_txn.commit()    
    img_env.close()   

    print skipped