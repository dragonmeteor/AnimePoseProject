import sys
import lmdb
import json
import cv2 as cv
from pprint import pprint
from pose_lib import permute_data_list
import datetime

if __name__ == "__main__":
    with open(sys.argv[1]) as the_file:
        args = json.load(the_file)
    with open(args["data_list_file"]) as the_file:
        data_list = json.load(the_file)

    if "random_seed" in args:
        permute_data_list(data_list, args["random_seed"])
    for i in xrange(10):
        print data_list[i][0]    