import sys
import re
import os.path

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
    fout = open(filename, "w")
    fout.write("%d\n" % len(loss))
    for item in loss:
        fout.write("%d %d %s %f\n" % (item[0], item[1], item[2], item[3]))
    fout.close()

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
    dir_name = sys.argv[1]
    snapshot_iter = int(sys.argv[2])
    snapshot_count = int(sys.argv[3])
    train_loss_file_name = sys.argv[4]
    val_loss_file_name = sys.argv[5]

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