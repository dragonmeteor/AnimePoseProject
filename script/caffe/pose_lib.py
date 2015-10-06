def transform_pose(in_pose, pose_xform, bone_names):
    out_pose = {}    
    for bone_name in bone_names:
        bone_xform = pose_xform[bone_name]        
        out_pos = [0,0]
        there_is_None = False
        for item in bone_xform:
            weight = item[0]
            name = item[1]
            in_pos = in_pose[name]
            if in_pos is not None:
                out_pos[0] += weight*in_pos[0]
                out_pos[1] += weight*in_pos[1]
            else:
                there_is_None = True
        if there_is_None:
            out_pose[bone_name] = None
        else:
            out_pose[bone_name] = out_pos
    return out_pose

def permute_data_list(data_list, random_seed):    
    print "Randomly permuting the data list with random_seed =", random_seed
    import random
    random.seed(random_seed)
    n = len(data_list)
    while n > 1:
        j = n-1
        i = random.randint(0, n-1)
        temp = data_list[i]
        data_list[i] = data_list[j]
        data_list[j] = temp
        n -= 1
    print "Done permuting!"
