require 'rubygems'
require 'bundler/setup'
require 'fileutils'
require 'json'
require File.expand_path(File.dirname(__FILE__) + "/../lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../java_lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/mmd_data_lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/manual_2d_labeling_lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/pose_2d_configs.rb") 
require File.expand_path(File.dirname(__FILE__) + "/example_transform_lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../caffe/caffe_pose_data_lib.rb")

MmdCharacterPool.new("mmd_pool_test_chars", "data/mmd_pool_test/chars",
	:source_dir => "data/mmd_pool_test/chars/raw")

mmd_chars = MmdCharacterPool.new("mmd_chars", "data/mmd_chars",
	:source_dir => "data/mmd_chars/raw")

MmdMotionPool.new("mmd_pool_test_motions", "data/mmd_pool_test/motions",
	:source_dir => "data/mmd_pool_test/motions/raw",
	:default_model => "data/pmd/youmu_swim/youmu_swim_green.pmd")

mmd_motions = MmdMotionPool.new("mmd_motions", "data/mmd_motions",
	:source_dir => "data/mmd_motions/data")

mmd_backgrounds = MmdBackgroundPool.new("mmd_backgrounds", "data/mmd_backgrounds",
	:source_dir => "data/mmd_backgrounds/raw")

mmd_split_01_chars = MmdCharacterSplitPool.new("mmd_split_01_chars", "data/mmd_split/01/chars",
	:source_file => mmd_chars.selected_char_info_file_name)
mmd_split_01_chars_old = MmdCharacterSplitPool.new("mmd_split_01_chars_old", "data/mmd_split/01/chars_old",
	:source_file => mmd_chars.selected_char_info_file_name)
mmd_split_01_motions = MmdMotionSplitPool.new("mmd_split_01_motions", "data/mmd_split/01/motions",
	:source_file => mmd_motions.motion_info_file_name)
mmd_split_01_backgrounds = MmdBackgroundSplitPool.new("mmd_split_01_backgrounds", "data/mmd_split/01/backgrounds",
	:source_file => mmd_backgrounds.background_list_file_name)

mmd_split_prog_chars = MmdCharacterSplitPool.new("mmd_split_prog_chars", "data/mmd_split/prog_chars",
	:source_file => mmd_split_01_chars.char_info_file_name(0))

=begin
MmdTrainingExampleTasks.new("mmd_examples_test", "data/mmd_examples/test",
	:count => 100,
	:model_file => "data/mmd_chars/selected/char_info.txt",
	:motion_file => "data/mmd_motions/used_motions_frames_and_bones.txt",
	:background_file => "data/mmd_backgrounds/background_list.txt",
	:view_settings => [1.0, 1.0, 1.0, 1.0, 1.0, 1.0])
=end

3.times do |i|
	count = [1000,200,200]
	mmbg_tasks = MmdTrainingExampleParamsTasks.new("mmd_examples_01_#{i}", "data/mmd_examples/01/#{i}",
		:count => count[i],
		:model_file => mmd_split_01_chars.char_info_file_name(i),
		:motion_file => mmd_split_01_motions.motion_info_file_name(i),
		:background_file => mmd_split_01_backgrounds.background_list_file_name(i))
	data_tasks = MmdTrainingExampleTasks.new("mmd_examples_01_#{i}_yr", "data/mmd_examples/01/#{i}/yr",
		:mmbg_tasks => mmbg_tasks,
		:image_width => 400,
		:image_height => 400,
		:view_settings => [1.0, 1.0, 1.0, 1.0, 1.0, 1.0],
		:render_joint_pos => false)
	data_tasks = MmdTrainingExampleTasks.new("mmd_examples_01_#{i}_im227", "data/mmd_examples/01/#{i}/im227",
		:mmbg_tasks => mmbg_tasks,
		:image_width => 227,
		:image_height => 227,
		:view_settings => [1.0, 1.0, 1.0, 1.0, 1.0, 1.0],
		:render_joint_pos => false)
end

MmdNegativeExamplePoolTasks.new("mmd_examples_01_neg", "data/mmd_examples/01/neg",
	:source_file => mmd_split_01_backgrounds.background_list_file_name(0))

3.times do |i|
	count = [1000000,200000,200000]
	mmbg_tasks = MmdTrainingExampleParamsTasks.new("mmd_examples_big_#{i}", "data/mmd_examples/big/#{i}",
		:count => count[i],
		:model_file => mmd_split_01_chars.char_info_file_name(i),
		:motion_file => mmd_split_01_motions.motion_info_file_name(i),
		:background_file => mmd_split_01_backgrounds.background_list_file_name(i),
		:limit => 1000)
	data_tasks = MmdTrainingExampleTasks.new("mmd_examples_big_#{i}_im227", "data/mmd_examples/big/#{i}/im227",
		:mmbg_tasks => mmbg_tasks,
		:image_width => 227,
		:image_height => 227,
		:view_settings => [1.0, 1.0, 1.0, 1.0, 1.0, 1.0],
		:render_joint_pos => false)
end

3.times do |i|
	count = [100000,2000,2000]
	mmbg_tasks = MmdTrainingExampleParamsTasks.new("mmd_examples_02_#{i}", "data/mmd_examples/02/#{i}",
		:count => count[i],
		:model_file => mmd_split_01_chars.char_info_file_name(i),
		:motion_file => mmd_split_01_motions.motion_info_file_name(i),
		:background_file => mmd_split_01_backgrounds.background_list_file_name(i),
		:limit => 1000)
	data_tasks = MmdTrainingExampleTasks.new("mmd_examples_02_#{i}_im227", "data/mmd_examples/02/#{i}/im227",
		:mmbg_tasks => mmbg_tasks,
		:image_width => 227,
		:image_height => 227,
		:view_settings => [1.0, 1.0, 1.0, 1.0, 1.0, 1.0],
		:render_joint_pos => false)
end

3.times do |i|
	count = [100000,2000,2000]
	mmbg_tasks = MmdTrainingExampleParamsTasks.new("mmd_examples_03_#{i}", 
		"data/mmd_examples/03/#{i}",
		:count => count[i],
		:model_file => mmd_split_01_chars.char_info_file_name(i),
		:motion_file => mmd_split_01_motions.motion_info_file_name(i),
		:background_file => mmd_split_01_backgrounds.background_list_file_name(i),
		:limit => 1000)
	data_tasks = MmdTrainingExampleTasks.new("mmd_examples_03_#{i}_im227", 
		"data/mmd_examples/03/#{i}/im227",
		:mmbg_tasks => mmbg_tasks,
		:image_width => 227,
		:image_height => 227,
		:view_settings => [1.0, 1.0, 1.0, 1.0, 1.0, 1.0], 
		:render_joint_pos => false)
	im224_tasks = CropExampleTasks.new("mmd_examples_03_#{i}_im224",
		"data/mmd_examples/03/#{i}/im224",
		:source_data_list => data_tasks.data_list_file_name,
		:source_data_count => data_tasks.data_count_file_name,
		:limit => 1000,
		:source_width => 227,
		:source_height => 227,
		:target_width => 224,
		:target_height => 224,)
end

3.times do |i|
	count = [1000000,2000,2000]
	mmbg_tasks = MmdTrainingExampleParamsTasks.new("mmd_examples_big2_#{i}", 
		"data/mmd_examples/big2/#{i}",
		:count => count[i],
		:model_file => mmd_split_01_chars.char_info_file_name(i),
		:motion_file => mmd_split_01_motions.motion_info_file_name(i),
		:background_file => mmd_split_01_backgrounds.background_list_file_name(i),
		:limit => 1000)
	data_tasks = MmdTrainingExampleTasks.new("mmd_examples_big2_#{i}_im227", 
		"data/mmd_examples/big2/#{i}/im227",
		:mmbg_tasks => mmbg_tasks,
		:image_width => 227,
		:image_height => 227,
		:view_settings => [1.0, 1.0, 1.0, 1.0, 1.0, 1.0],
		:render_joint_pos => false)
	im224_tasks = CropExampleTasks.new("mmd_examples_big2_#{i}_im224",
		"data/mmd_examples/big2/#{i}/im224",
		:source_data_list => data_tasks.data_list_file_name,
		:source_data_count => data_tasks.data_count_file_name,
		:limit => 1000,
		:source_width => 227,
		:source_height => 227,
		:target_width => 224,
		:target_height => 224,)
end

[1000, 10000].each do |count|
	mmbg_tasks = MmdTrainingExampleParamsTasks.new("mmd_examples_var_#{count}", 
		"data/mmd_examples/var/#{count}",
		:count => count,
		:model_file => mmd_split_01_chars.char_info_file_name(0),
		:motion_file => mmd_split_01_motions.motion_info_file_name(0),
		:background_file => mmd_split_01_backgrounds.background_list_file_name(0),
		:limit => 1000)
	data_tasks = MmdTrainingExampleTasks.new("mmd_examples_var_#{count}_im224", 
		"data/mmd_examples/var/#{count}/im224",
		:mmbg_tasks => mmbg_tasks,
		:image_width => 224,
		:image_height => 224,
		:view_settings => [1.0, 1.0, 1.0, 1.0, 1.0, 1.0],
		:render_joint_pos => false)
end

#################################
# Standing pose image for paper #
#################################

mmd_split_miku_only = MmdCharacterSplitPool.new("mmd_split_miku_only", "data/mmd_split/miku_only",
	:source_file => mmd_chars.selected_char_info_file_name)
mmd_split_stand_only = MmdMotionSplitPool.new("mmd_split_stand_only", "data/mmd_split/stand_only",
	:source_file => mmd_motions.motion_info_file_name)
white_background = MmdBackgroundPool.new("white_background", "data/mmd_backgrounds/white",
	:source_dir => "data/mmd_backgrounds/white")

begin
	mmbg_tasks = MmdTrainingExampleParamsTasks.new("mmd_standing_pose", "data/mmd_examples/standing_pose",
		:count => 1,
		:model_file => mmd_split_miku_only.char_info_file_name(0),
		:motion_file => mmd_split_stand_only.motion_info_file_name(0),
		:background_file => white_background.background_list_file_name,
		:limit => 1000)
	data_tasks = MmdTrainingExampleTasks.new("mmd_standing_pose_data", "data/mmd_examples/standing_pose",
		:mmbg_tasks => mmbg_tasks,
		:image_width => 1024,
		:image_height => 1024,
		:view_settings => [1.0, 1.0, 1.0, 1.0, 1.0, 1.0],
		:render_joint_pos => true,
		:camera_theta_min => 90,
		:camera_theta_max => 90,
		:camera_phi_min => 60,
		:camera_phi_max => 60,
		:camera_rotation_min => 0,
		:camera_rotation_max => 0,
		:camera_shift_x_min => 0,
		:camera_shift_x_max => 0,
		:camera_shift_y_min => 0,
		:camera_shift_y_max => 0,)
	pose_tasks = CaffePoseData.new("mmd_standing_pose_mmd_21", "data/mmd_examples/standing_pose/mmd_21",
		:data_list_file => data_tasks.data_list_file_name,
		:pose_2d_config => $p2dc_mmd_21,
		:pose_xform => $p2xf_project_mmd_21,
		:bone_weights => nil,			
		:image_width => 1024,
		:image_height => 1024,
		:limit => 10000,
		:random_seed => 47841321987,
		:caffe_root => "/opt/caffe",
		:check_image => true,)
	pose_tasks = CaffePoseData.new("mmd_standing_pose_mmd_21_midlimbs", 
		"data/mmd_examples/standing_pose/mmd_21_midlimbs",
		:data_list_file => data_tasks.data_list_file_name,
		:pose_2d_config => $p2dc_mmd_21_midlimbs,
		:pose_xform => $p2xf_mmd_21_add_midlimbs,
		:bone_weights => nil,			
		:image_width => 1024,
		:image_height => 1024,
		:limit => 10000,
		:random_seed => 47841321987,
		:caffe_root => "/opt/caffe",
		:check_image => true,)

end

########################
# Variety of apperance #
########################
3.times do |index|
	mmbg_tasks = MmdTrainingExampleParamsTasks.new("mmd_examples_appear_#{index}", 
		"data/mmd_examples/appear/#{index}",
		:count => 100000,
		:model_file => mmd_split_prog_chars.char_info_file_name(index),
		:motion_file => mmd_split_01_motions.motion_info_file_name(0),
		:background_file => mmd_split_01_backgrounds.background_list_file_name(0),
		:limit => 1000)
	data_tasks = MmdTrainingExampleTasks.new("mmd_examples_appear_#{index}_im224", 
		"data/mmd_examples/appear/#{index}/im224",
		:mmbg_tasks => mmbg_tasks,
		:image_width => 224,
		:image_height => 224,
		:view_settings => [1.0, 1.0, 1.0, 1.0, 1.0, 1.0],
		:render_joint_pos => false)
end

#############
# For paper #
#############
begin
	mmbg_tasks = MmdTrainingExampleParamsTasks.new("mmd_examples_paper", 
		"data/mmd_examples/paper",
		:count => 100,
		:model_file => mmd_split_01_chars.char_info_file_name(0),
		:motion_file => mmd_split_01_motions.motion_info_file_name(0),
		:background_file => mmd_split_01_backgrounds.background_list_file_name(0),
		:limit => 1000)
	data_tasks = MmdTrainingExampleTasks.new("mmd_examples_paper_im512", 
		"data/mmd_examples/paper/im512",
		:mmbg_tasks => mmbg_tasks,
		:image_width => 512,
		:image_height => 512,
		:view_settings => [1.0, 1.0, 1.0, 1.0, 1.0, 1.0],
		:render_joint_pos => false)
end

####################
# Variety of poses #
####################
pose_pool = MmdConsecutivelyLargerMotionPool.new("mmd_split_prog_poses", 
	"data/mmd_split/prog_poses",
	:source_file => mmd_split_01_motions.motion_info_file_name(0),
	:counts => [1000, 10000, 100000])

3.times do |index|
	mmbg_tasks = MmdTrainingExampleParamsTasks.new("mmd_examples_pose_#{index}", 
		"data/mmd_examples/pose/#{index}",
		:count => 100000,
		:model_file => mmd_split_01_chars.char_info_file_name(0),
		:motion_file => pose_pool.motion_info_file_name(0),
		:background_file => mmd_split_01_backgrounds.background_list_file_name(0),
		:limit => 1000)
	data_tasks = MmdTrainingExampleTasks.new("mmd_examples_pose_#{index}_im224", 
		"data/mmd_examples/pose/#{index}/im224",
		:mmbg_tasks => mmbg_tasks,
		:image_width => 224,
		:image_height => 224,
		:view_settings => [1.0, 1.0, 1.0, 1.0, 1.0, 1.0],
		:render_joint_pos => false)
end