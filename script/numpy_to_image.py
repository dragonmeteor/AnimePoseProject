import cv2
from cv2 import cv
import numpy as np
import sys

if __name__ == "__main__":
	mean_file = sys.argv[1]
	output_file = sys.argv[2]
	
	mean = np.load(mean_file)
	mean = np.swapaxes(mean,0,2)
	mean = mean.astype('float32')
	cv2.imwrite(output_file, mean)	