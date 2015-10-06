import sys
import numpy as np
import cv2 as cv
import json
caffe_root = "/opt/caffe/"
sys.path.insert(0, caffe_root + "python")
import caffe

if __name__ == "__main__":
	with open(sys.argv[1]) as the_file:
		args = json.load(the_file)

	deploy_file = args["deploy_file"]
	caffemodel_file = args["caffemodel_file"]
	mean_file = args["mean_file"]
	image_width = args["image_width"]
	image_height = args["image_height"]	
	with open(args["file_list"]) as the_file:
		file_list = json.load(the_file)
	output_file = args["output_file"]		

	net = caffe.Net(str(deploy_file), str(caffemodel_file), caffe.TEST)
	mean = np.load(args["mean_file"])

	output = []	
	
	count = 0	
	for filename in file_list:		
		img = cv.imread(filename, 1)		
		tmp = np.copy(img.swapaxes(0, 2).swapaxes(1, 2))
		tmp = tmp.astype('float64')			
		tmp -= mean

		net.blobs['data'].data[:, :, :, :] = tmp
		joints = net.forward().values()[0].flatten()
		joints = np.asarray(joints)
		
		width = np.shape(joints)[0]
		output.append(joints.tolist())
		
		count += 1
		if count % 10 == 0:
			print count, "files processed ..."
	
	with open(output_file, "w") as the_file:
		json.dump(output, the_file)