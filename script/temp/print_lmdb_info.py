import sys
import lmdb
caffe_root = "/opt/caffe"
sys.path.insert(0, caffe_root + "/python")
import caffe

filename = "data/poseest/lsp/train/im224/lsp_renamed_weights/pose.lmdb"

img_env = lmdb.open(filename)
img_txn = img_env.begin(write=False, buffers=True)
lmdb_cursor = img_txn.cursor()

count = 0
for key, value in lmdb_cursor:	
	if count == 0:
		print key	
		datum = caffe.io.caffe_pb2.Datum()
		datum.ParseFromString(value)
		print datum
		break
	count += 1


img_env.close()

#print count