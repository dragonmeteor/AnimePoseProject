import numpy
import matplotlib
matplotlib.use('Agg')
from matplotlib import pyplot
import sys
import json

if __name__ == "__main__":
	with open(sys.argv[1]) as the_file:
		args = json.load(the_file)	

	font = {'family' : 'serif',
		'weight' : 'normal',
		'size'   : 18}
	matplotlib.rc('font', **font)
	pyplot.hold(True)
	pyplot.grid(True)
	for datum in args["data"]:
		x = datum["x"]
		y = datum["y"]
		if ("style" in datum) and (datum["style"] is not None):
			if datum["label"] is not None and len(datum["label"]) > 0:
				pyplot.plot(x, y, datum["style"], label=datum["label"])
			else:
				pyplot.plot(x, y, datum["style"])
		else:
			if datum["label"] is not None and len(datum["label"]) > 0:
				pyplot.plot(x, y, label=datum["label"])
			else:
				pyplot.plot(x, y)
	pyplot.title(args["title"])
	pyplot.xlabel(args["x_label"])
	pyplot.ylabel(args["y_label"])
	if "show_legends" in args:
		if args["show_legends"]:
			if "legend_location" in args:
				pyplot.legend(loc=args["legend_location"])
			else:
				pyplot.legend()
	pyplot.savefig(args["output_file"])

