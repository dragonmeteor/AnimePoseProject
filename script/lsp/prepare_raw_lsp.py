import json
import scipy
import scipy.io
import numpy

joints_orig = scipy.io.loadmat("data/raw/lsp/lsp_dataset/joints.mat")["joints"]
joints_extended = scipy.io.loadmat("data/raw/lsp/lspet_dataset/joints.mat")["joints"]

joint_names = [
    'right_ankle',
	'right_knee',
	'right_hip',
	'left_hip',
	'left_knee',
	'left_ankle',
	'right_wrist',
	'right_elbow',
	'right_shoulder',
	'left_shoulder',
	'left_elbow',
	'left_wrist',
	'neck',
	'head_top'
]

train_data = []
for i in xrange(1000):
	points = joints_orig[:,:,i]
	filename = "data/raw/lsp/lsp_dataset/images/im%04d.jpg" % (i+1,)
	points_2d = {}
	for j in xrange(14):
		if points[2,j] < 0.5:
			points_2d[joint_names[j]] = [points[0,j], points[1,j]]
		else:
			points_2d[joint_names[j]] = None
	labeling = {}
	labeling["file_name"] = filename
	labeling["points"] = points_2d
	train_data.append(labeling)

test_data = []
for i in xrange(1000,2000):
	points = joints_orig[:,:,i]
	filename = "data/raw/lsp/lsp_dataset/images/im%04d.jpg" % (i+1,)
	points_2d = {}
	for j in xrange(14):		
		points_2d[joint_names[j]] = [points[0,j], points[1,j]]		
	labeling = {}
	labeling["file_name"] = filename
	labeling["points"] = points_2d
	test_data.append(labeling)	

with open("data/raw/lsp/test.json", "w") as fout:
	fout.write(json.dumps(test_data, indent=4, separators=(',', ': ')))

val_data = []
for i in xrange(10000):
	points = joints_extended[:,:,i]
	points = numpy.transpose(points)
	filename = "data/raw/lsp/lspet_dataset/images/im%05d.jpg" % (i+1,)
	points_2d = {}
	for j in xrange(14):
		if points[2,j] > 0.5:
			points_2d[joint_names[j]] = [points[0,j], points[1,j]]
		else:
			points_2d[joint_names[j]] = None
	labeling = {}
	labeling["file_name"] = filename
	labeling["points"] = points_2d
	if i < 9000:
		train_data.append(labeling)
	else:
		val_data.append(labeling)

with open("data/raw/lsp/train.json", "w") as fout:
	fout.write(json.dumps(train_data, indent=4, separators=(',', ': ')))

with open("data/raw/lsp/val.json", "w") as fout:
	fout.write(json.dumps(val_data, indent=4, separators=(',', ': ')))
