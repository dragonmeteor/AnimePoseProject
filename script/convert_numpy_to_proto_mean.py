caffe_root = "/opt/caffe/"
import sys
sys.path.insert(0, caffe_root + "python")

import caffe
import numpy as np

if __name__=="__main__":
	if len(sys.argv) != 3:
		print "Usage: python convert_numpy_to_proto_mean.py <num.py> <proto.mean>"
		sys.exit()

	mean_file = sys.argv[1]
	proto_file = sys.argv[2]

	mean = np.load(mean_file)
	blob = caffe.proto.caffe_pb2.BlobProto()	
	blob.num = 1
	blob.channels, blob.height, blob.width = mean.shape
	blob.data.extend(mean.astype(float).flat)
	binaryproto_file = open(proto_file, 'wb' )
	binaryproto_file.write(blob.SerializeToString())
	binaryproto_file.close()	