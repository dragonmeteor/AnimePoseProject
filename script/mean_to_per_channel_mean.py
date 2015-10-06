import numpy as np
import sys

if __name__ == "__main__":
	mean_file = sys.argv[1]
	per_channel_file = sys.argv[2]

	mean = np.load(mean_file)
	c0 = np.mean(mean[0,:,:])
	c1 = np.mean(mean[1,:,:])
	c2 = np.mean(mean[2,:,:])
	mean[0,:,:] = c0
	mean[1,:,:] = c1
	mean[2,:,:] = c2
	print c0, c1, c2
	np.save(per_channel_file, mean)
