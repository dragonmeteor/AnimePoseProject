import sys
import json
from pose_lib import transform_pose

if __name__ == "__main__":
    with open(sys.argv[1]) as data_file:
        args = json.load(data_file)
    with open(args["data_list_file"]) as data_file:
        data_list = json.load(data_file)
    with open(args["pose_2d_config_file"]) as data_file:
        pose_config = json.load(data_file)
    with open(args["pose_xform_file"]) as data_file:
        pose_xform = json.load(data_file)
    output_file = args["output_file"]
    if "limit" in args:
        limit = args["limit"]

    output = []
    count = 0
    for item in data_list:
        image_file = item[0]
        pose_file = item[1]
        with open(pose_file) as data_file:
            pose = json.load(data_file)
        pose = pose["points_2d"]        
        pose = transform_pose(pose, pose_xform["bone_xforms"], pose_config["bone_names"])
        output_item = {
            "file_name": image_file,
            "points": pose
        }
        output.append(output_item)

        count += 1
        if count >= limit:
            break

    with open(output_file, 'w') as outfile:        
        json.dump(output, outfile, sort_keys=True, indent=1, separators=(',', ': '))