import sys
import numpy as np
import cv2 as cv
caffe_root = "/opt/caffe/"
sys.path.insert(0, caffe_root + "python")
import caffe

if __name__ == "__main__":
	model_file = sys.argv[1]
	caffemodel_file = sys.argv[2]
	mean_file = sys.argv[3]
	image_width = int(sys.argv[4])	
	image_height = int(sys.argv[5])
	file_list = sys.argv[6]
	output_file = sys.argv[7]

	net = caffe.Net(model_file, caffemodel_file, caffe.TEST)

	mean = np.load(mean_file)	
	#mean = np.swapaxes(mean, 1, 2)
	#mean_temp = np.copy(mean)
	#mean[0,:,:] = mean_temp[2,:,:]	
	#mean[2,:,:] = mean_temp[0,:,:]
	
	if True:
		fin = open(file_list, "r")
		lines = fin.readlines()	
		count = 0
		fout = open(output_file, "wt")	
		for line in lines:	
			filename = line.strip()
			if len(filename) > 0:
				img = cv.imread(filename)
				img = cv.resize(img, (227, 227))				
				tmp = np.copy(img.swapaxes(0, 2).swapaxes(1, 2))
				tmp = tmp.astype('float64')			
				tmp -= mean

				net.blobs['data'].data[:, :, :, :] = tmp
				joints = net.forward().values()[0].flatten()
				joints = np.asarray(joints)
				
				width = np.shape(joints)[0]

				for j in xrange(width):
					fout.write("%f " % joints[j])
				fout.write("\n")

				count += 1
				if count % 100 == 0:
					print count, "files processed ..."
		fout.close()