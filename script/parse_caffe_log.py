import sys
import re

float_pattern = '[-+]?[0-9]*\.?[0-9]+(?:[eE][-+]?[0-9]+)?'
itnum_pattern = 'Iteration (\\d+)'
train_output_pattern = 'Train net output #(\\d+): (\S+) = ({0})'.format(float_pattern)
test_output_pattern = 'Test net output #(\\d+): (\S+) = ({0})'.format(float_pattern)
patterns = [train_output_pattern, test_output_pattern]

if __name__ == "__main__":
    input_file = sys.argv[1]
    output_file = sys.argv[2]

    fin = open(input_file, "r")
    fout = open(output_file, "w")
    while True:
        line = fin.readline()
        #print line
        if len(line) == 0:
            break

        match = re.search(itnum_pattern, line)
        if match:
            itnum = match.groups()[0]
            itnum = int(itnum)  

        for i in range(2):
            match = re.search(patterns[i], line)
            if match:
                output_num, output_name, output = match.groups()
                output = float(output)
                output_num = int(output_num)

                if i == 0:                
                    fout.write("train %d %d %s %f" % (itnum, output_num, output_name, output))
                else:
                    fout.write("val %d %d %s %f" % (itnum, output_num, output_name, output))
    fin.close()
    fout.close()