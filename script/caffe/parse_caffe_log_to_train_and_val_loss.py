import sys
import re
import os.path
import json

def parse_caffe_log(filename, train, val):  
    float_pattern = '[-+]?[0-9]*\.?[0-9]+(?:[eE][-+]?[0-9]+)?'
    itnum_pattern = 'Iteration (\\d+)'
    train_output_pattern = 'Train net output #(\\d+): (\S+) = ({0})'.format(float_pattern)
    test_output_pattern = 'Test net output #(\\d+): (\S+) = ({0})'.format(float_pattern)
    patterns = [train_output_pattern, test_output_pattern]

    fin = open(filename, "r")
    while True:
        line = fin.readline()
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
                    train.append([itnum, output_num, output_name, output])
                else:
                    val.append([itnum, output_num, output_name, output])
    fin.close()    

def write_loss_file(filename, loss):
    with open(filename, "w") as the_file:
        json.dump(loss, the_file, indent=1, separators=(',', ':'))    

def remove_duplicates(loss):
    sorted_loss = sorted(loss, key=lambda x: x[0])
    result = []
    for i in xrange(len(sorted_loss)):
        if i > 0:
            if sorted_loss[i][0] != sorted_loss[i-1][0]:
                result.append(sorted_loss[i])
        else:
            result.append(sorted_loss[i])
    return result


if __name__ == "__main__":
    with open(sys.argv[1]) as the_file:
        args = json.load(the_file)  
    
    dir_name = args["dir_name"]
    snapshot_iter = args["snapshot_iter"]
    snapshot_count = args["snapshot_count"]
    train_loss_file_name = args["train_loss_file_name"]
    val_loss_file_name = args["val_loss_file_name"]
    
    train = []
    val = []
    for i in xrange(snapshot_count):
        filename = "%s/log_iter_%d.txt" % (dir_name, (i+1)*snapshot_iter)
        if os.path.isfile(filename):
            parse_caffe_log(filename, train, val)

    train = remove_duplicates(train)
    val = remove_duplicates(val)
    write_loss_file(train_loss_file_name, train)
    write_loss_file(val_loss_file_name, val)