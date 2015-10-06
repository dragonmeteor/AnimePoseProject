import numpy
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as pyplot

import sys

if __name__ == "__main__":
	input_file = sys.argv[1]
	output_file = sys.argv[2]

	x = []
	y = []
	fin = open(input_file, "r")
	count = int(fin.readline())
	for i in xrange(count):
		line = fin.readline()
		comps = line.split()		
		x.append(float(comps[0]))
		y.append(float(comps[3]))

	pyplot.plot(x, y)
	pyplot.savefig(output_file)