require 'rubygems'
require 'bundler/setup'
require File.expand_path(File.dirname(__FILE__) + "/../../caffe/simple_pose_regressor_lib.rb")
require File.expand_path(File.dirname(__FILE__) + "/../../caffe/weighted_pose_regressor_lib.rb")
require File.expand_path(File.dirname(__FILE__) + "/../../caffe/caffe_prediction_lib.rb")
require File.expand_path(File.dirname(__FILE__) + "/../strict_pcp_lib.rb")
require File.expand_path(File.dirname(__FILE__) + "/../pose_2d_configs.rb")
require File.expand_path(File.dirname(__FILE__) + "/../../caffe/caffe_image_data_lib.rb")
require File.expand_path(File.dirname(__FILE__) + "/../../caffe/caffe_pose_data_lib.rb")
require File.expand_path(File.dirname(__FILE__) + "/../../pose_est/pdj_lib.rb")
require File.expand_path(File.dirname(__FILE__) + "/../../caffe/caffe_test.rb")
require File.expand_path(File.dirname(__FILE__) + "/../../caffe/caffe_test_02.rb")
require File.expand_path(File.dirname(__FILE__) + "/../example_transform_lib.rb")
require File.expand_path(File.dirname(__FILE__) + "/../anime_data.rb")
require File.expand_path(File.dirname(__FILE__) + "/../pose_distrib/chain_distrib_lib.rb")
require File.expand_path(File.dirname(__FILE__) + "/../../pose_est/average_loss_lib.rb")

def read_pcp_result(filename)
	pcp = JSON.parse(File.read(filename))
	output = {}
	["torso", "head", "upper_arms", "lower_arms", "upper_legs", "lower_legs"].each do |part_name|
		if pcp.include?(part_name)
			output[part_name] = pcp[part_name]["score"]
		else
			output[part_name] = nil
		end
	end	
	output
end

def read_pdj_result(filename)
	pdj = JSON.parse(File.read(filename))
	output = {}
	["body", "head", "nose_root", "elbows", "wrists", "knees", "ankles"].each do |joint_name|
		if pdj.include?(joint_name)
			output[joint_name] = pdj[joint_name][-1][1] * 1.0 / pdj[joint_name][-1][2]
		else
			output[joint_name] = nil
		end
	end
	output
end

#####################
# mmd big2 data set #
#####################
big2_tasks = {}

[0,1,2].each do |data_set_index|
	big2_tasks[data_set_index] = {}
	[227, 224].each do |image_size|
		big2_tasks[data_set_index][image_size] = {}

		big2_tasks[data_set_index][image_size][:image] = CaffeImageData.new(
			"poseest_mmd_big2_#{data_set_index}_im#{image_size}_image",
			"data/poseest/mmd_big2/#{data_set_index}/im#{image_size}",
			:data_list_file => "data/mmd_examples/big2/#{data_set_index}/im#{image_size}/data_list.txt",
			:image_width => image_size,
			:image_height => image_size
		)

		big2_tasks[data_set_index][image_size][:pose] = {}
		
		big2_tasks[data_set_index][image_size][:pose][:mmd_21_weights] = CaffePoseData.new(
			"poseest_mmd_big2_#{data_set_index}_im#{image_size}_pose_mmd_21_weights",
			"data/poseest/mmd_big2/#{data_set_index}/im#{image_size}/mmd_21_weights",
			:data_list_file => "data/mmd_examples/big2/#{data_set_index}/im#{image_size}/data_list.txt",	
			:pose_2d_config => $p2dc_mmd_21_weights,
			:pose_xform => $p2xf_mmd_21_add_weights,
			:image_width => image_size,
			:image_height => image_size
		)

		big2_tasks[data_set_index][image_size][:pose][:mmd_18] = CaffePoseData.new(
			"poseest_mmd_big2_#{data_set_index}_im#{image_size}_pose_mmd_18",
			"data/poseest/mmd_big2/#{data_set_index}/im#{image_size}/mmd_18",
			:data_list_file => "data/mmd_examples/big2/#{data_set_index}/im#{image_size}/data_list.txt",
			:pose_2d_config => $p2dc_mmd_18,
			:pose_xform => $p2xf_project_mmd_18,
			:image_width => image_size,
			:image_height => image_size
		)

		big2_tasks[data_set_index][image_size][:pose][:mmd_21] = CaffePoseData.new(
			"poseest_mmd_big2_#{data_set_index}_im#{image_size}_pose_mmd_21",
			"data/poseest/mmd_big2/#{data_set_index}/im#{image_size}/mmd_21",
			:data_list_file => "data/mmd_examples/big2/#{data_set_index}/im#{image_size}/data_list.txt",
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21,
			:image_width => image_size,
			:image_height => image_size
		)

		big2_tasks[data_set_index][image_size][:pose][:mmd_21_midlimbs] = CaffePoseData.new(
			"poseest_mmd_big2_#{data_set_index}_im#{image_size}_pose_mmd_21_midlimbs",
			"data/poseest/mmd_big2/#{data_set_index}/im#{image_size}/mmd_21_midlimbs",
			:data_list_file => "data/mmd_examples/big2/#{data_set_index}/im#{image_size}/data_list.txt",
			:pose_2d_config => $p2dc_mmd_21_midlimbs,
			:pose_xform => $p2xf_mmd_21_add_midlimbs,
			:image_width => image_size,
			:image_height => image_size
		)
	end
end

big2_tasks[1][227][:distrib] = {}

big2_tasks[1][227][:distrib][:leg_left] = ChainDistribTasks.new(
	"poseest_mmd_big2_1_im227_distrib_leg_left",
	"data/poseest/mmd_big2/1/im227/distrib/leg_left",
	:data_list_file => "data/mmd_examples/big2/1/im227/data_list.txt",
	:chain => ["leg_left", "knee_left", "ankle_left"],
	:pose_2d_config => $p2dc_mmd_18,	
	:image_width => 227,
	:image_height => 227,
	:colors => [[0,0,1],[0,1,0]])

big2_tasks[1][227][:distrib][:arm_left] = ChainDistribTasks.new(
	"poseest_mmd_big2_1_im227_distrib_arm_left",
	"data/poseest/mmd_big2/1/im227/distrib/arm_left",
	:data_list_file => "data/mmd_examples/big2/1/im227/data_list.txt",
	:chain => ["arm_left", "elbow_left", "wrist_left"],
	:pose_2d_config => $p2dc_mmd_18,	
	:image_width => 227,
	:image_height => 227,
	:colors => [[0,0,1],[0,1,0]])

$poseest_mmd_big2_alexnet = CaffeSimplePoseRegressor.new("poseest_mmd_big2_alexnet",
	"data/poseest/mmd_big2_alexnet", 
	:image_mean_file => $caffe_permuted_mmd_03_0_im227_image.per_channel_image_mean_file_name,
	
	:train_image_lmdb_file => big2_tasks[0][227][:image].image_lmdb_file_name,
	:train_pose_lmdb_file => big2_tasks[0][227][:pose][:mmd_21_weights].pose_lmdb_file_name,
	:val_image_lmdb_file => big2_tasks[1][227][:image].image_lmdb_file_name,
	:val_pose_lmdb_file => big2_tasks[1][227][:pose][:mmd_21_weights].pose_lmdb_file_name,

	:base_lr => 0.001,	
	:snapshot => 100000,

	:test_iter => 200,
	:val_batch_size => 10,
	:test_interval => 1000,	

	:train_batch_size => 50,
	:max_iter => 400000,
	:stepsize => 400000,
	:epoch_iter_count => 2000,

	:display => 100,

	:web_service_port => 9292,

	:finetune_from => "data/caffe/bvlc_alexnet.caffemodel",

	:bone_count => $p2dc_mmd_21_weights.bone_names.count,

	:inner_product_layer_std => 0.001,
	:network_type => :alexnet)

$poseest_mmd_big2_alexnet_old = CaffeSimplePoseRegressor.new("poseest_mmd_big2_alexnet_old",
	"data/poseest/mmd_big2_alexnet_old", 
	:image_mean_file => $caffe_permuted_mmd_03_0_im227_image.per_channel_image_mean_file_name,

	:train_image_lmdb_file => big2_tasks[0][227][:image].image_lmdb_file_name,
	:train_pose_lmdb_file => big2_tasks[0][227][:pose][:mmd_21_weights].pose_lmdb_file_name,
	:val_image_lmdb_file => big2_tasks[1][227][:image].image_lmdb_file_name,
	:val_pose_lmdb_file => big2_tasks[1][227][:pose][:mmd_21_weights].pose_lmdb_file_name,

	:base_lr => 0.001,	
	:snapshot => 100000,

	:test_iter => 200,
	:val_batch_size => 10,
	:test_interval => 1000,	

	:train_batch_size => 50,
	:max_iter => 300000,
	:stepsize => 100000,
	:epoch_iter_count => 2000,

	:display => 100,

	:web_service_port => 9292,

	:finetune_from => "data/caffe/bvlc_alexnet.caffemodel",

	:bone_count => $p2dc_mmd_21_weights.bone_names.count,

	:inner_product_layer_std => 0.001,
	:network_type => :alexnet)

$poseest_mmd_big2_googlenet = CaffeSimplePoseRegressor.new(
	"poseest_mmd_big2_googlenet", 
	"data/poseest/mmd_big2_googlenet", 
	:image_mean_file => $caffe_permuted_mmd_03_0_im224_image.per_channel_image_mean_file_name,

	:train_image_lmdb_file => big2_tasks[0][224][:image].image_lmdb_file_name,
	:train_pose_lmdb_file => big2_tasks[0][224][:pose][:mmd_21_weights].pose_lmdb_file_name,
	:val_image_lmdb_file => big2_tasks[1][224][:image].image_lmdb_file_name,
	:val_pose_lmdb_file => big2_tasks[1][224][:pose][:mmd_21_weights].pose_lmdb_file_name,

	:base_lr => 0.001,	
	:snapshot => 100000,

	:test_iter => 200,
	:val_batch_size => 10,
	:test_interval => 1000,	

	:train_batch_size => 10,
	:max_iter => 400000,
	:stepsize => 100000,
	:epoch_iter_count => 2000,

	:display => 100,

	:web_service_port => 9292,

	:finetune_from => "data/caffe/bvlc_googlenet.caffemodel",

	:bone_count => $p2dc_mmd_21_weights.bone_names.count,

	:gamma => 0.96,
	:momentum => 0.9,
	:weight_decay => 0.0002,
	:inner_product_layer_std => 0.001,
	:network_type => :googlenet)

networks = {
	:mmd_big2 => {
		:alexnet => {
			:name => "mmd_big2_alexnet",
			:pose_2d_config => $p2dc_mmd_21_weights,
			:tasks => $poseest_mmd_big2_alexnet,
			:caffemodel_file => $poseest_mmd_big2_alexnet.caffemodel_file_name(3),
			:image_mean_file => $caffe_permuted_mmd_03_0_im227_image.per_channel_image_mean_npy_file_name,
			:image_width => 227,
			:image_height => 227,
		},
		:googlenet => {
			:name => "mmd_big2_googlenet",
			:pose_2d_config => $p2dc_mmd_21_weights,
			:tasks => $poseest_mmd_big2_googlenet,
			:caffemodel_file => $poseest_mmd_big2_googlenet.caffemodel_file_name(3),
			:image_mean_file => $caffe_permuted_mmd_03_0_im224_image.per_channel_image_mean_npy_file_name,
			:image_width => 224,
			:image_height => 224,
		}
	}
}

[1,2].each do |data_set_index|
	big2_tasks[data_set_index][:predict] = {}
	[:alexnet, :googlenet].each do |network|
		network = networks[:mmd_big2][network]
		network_name = network[:name]
		image_size = network[:image_width]

		big2_tasks[data_set_index][:predict][network_name] = {}

		big2_tasks[data_set_index][:predict][network_name][:predict] = CaffePredictionTasks.new(
			"poseest_mmd_big2_#{data_set_index}_predict_#{network_name}",
			"data/poseest/mmd_big2/#{data_set_index}/predict/#{network_name}",
			:file_list => FileList.new("data/mmd_examples/big2/#{data_set_index}/im#{image_size}/data-*/data_*.png"),
			:pose_2d_config => network[:pose_2d_config],
			:caffe_deploy_file => network[:tasks].deploy_file_name,
			:caffemodel_file => network[:caffemodel_file],
			:image_mean_npy_file => network[:image_mean_file],
			:image_width => network[:image_width],
			:image_height => network[:image_height],
		)

		big2_tasks[data_set_index][:predict][network_name][:pdj] = PDJEvaluation.new(
			"poseest_mmd_big2_#{data_set_index}_predict_#{network_name}_pdj",
			"data/poseest/mmd_big2/#{data_set_index}/predict/#{network_name}/pdj",
			:prediction_file => big2_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => big2_tasks[data_set_index][image_size][:pose][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21
		)		
		
		big2_tasks[data_set_index][:predict][network_name][:pcp] = StrictPCPEvaluation.new(
			"poseest_mmd_big2_#{data_set_index}_predict_#{network_name}_pcp", 
			"data/poseest/mmd_big2/#{data_set_index}/predict/#{network_name}/pcp",
			:prediction_file => big2_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => big2_tasks[data_set_index][image_size][:pose][:mmd_18].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21,
			:threshold => 0.5,
		)
	end
end

PDJComparison.new(
	"poseest_mmd_big2_1_predict_compare_alexnet_googlenet", 
	"data/poseest/mmd_big2/1/predict/compare_alexnet_googlenet",
	:pdj_files => [
		["AlexNet", "data/poseest/mmd_big2/1/predict/mmd_big2_alexnet/pdj/pdj.txt"],
		["GoogleNet", "data/poseest/mmd_big2/1/predict/mmd_big2_googlenet/pdj/pdj.txt"],			
	],
	:pose_2d_config => $p2dc_mmd_18)

PDJComparison.new(
	"poseest_mmd_big2_2_predict_compare_alexnet_googlenet", 
	"data/poseest/mmd_big2/2/predict/compare_alexnet_googlenet",
	:pdj_files => [
		["AlexNet", "data/poseest/mmd_big2/2/predict/mmd_big2_alexnet/pdj/pdj.txt"],
		["GoogleNet", "data/poseest/mmd_big2/2/predict/mmd_big2_googlenet/pdj/pdj.txt"],			
	],
	:pose_2d_config => $p2dc_mmd_18)

task :poseest_mmd_big2_experiment do
	pcp_results = {}
	pdj_results = {}

	["mmd_big2_googlenet"].each do |network_name|
		prediction_task = big2_tasks[2][:predict][network_name][:predict]
		#Rake::Task[prediction_task.name + ":raw_prediction_clean"].invoke
		Rake::Task[prediction_task.name + ":raw_prediction"].invoke
		Rake::Task[prediction_task.name + ":labeling_2d_clean"].invoke
		Rake::Task[prediction_task.name + ":labeling_2d"].invoke
		
		pdj_task = big2_tasks[2][:predict][network_name][:pdj]
		Rake::Task[pdj_task.name + ":pdj_clean"].invoke
		Rake::Task[pdj_task.name + ":pdj"].invoke
		pdj_results[network_name] = read_pdj_result(pdj_task.pdj_file_name)

		pcp_task = big2_tasks[2][:predict][network_name][:pcp]
		Rake::Task[pcp_task.name + ":pcp_clean"].invoke
		Rake::Task[pcp_task.name + ":pcp"].invoke
		pcp_results[network_name] = read_pcp_result(pcp_task.pcp_file_name)
	end	

	Rake::Task["poseest_mmd_big2_2_predict_mmd_big2_googlenet_pdj:pdj_clean"].invoke
	Rake::Task["poseest_mmd_big2_2_predict_mmd_big2_googlenet_pdj:pdj"].invoke

	target_dir = "/home/pramook/pramook/workspace/animepose-paper/images/mmd_big2_googlenet_pdj"
	run("mkdir -p #{target_dir}")
	run("cp -rf data/poseest/anime/test/predict/mmd_big2_googlenet/pdj/* #{target_dir}")
	
	target_file = "/home/pramook/pramook/workspace/animepose-paper/data/mmd_big2_googlenet_pdj.txt"
	File.open(target_file, "w") do |fout|
		paper_names = {
			"mmd_big2_googlenet" => "Synth",			
		}
		["mmd_big2_googlenet"].each do |network_name|
			fout.write("\\textsc{#{paper_names[network_name]}} & ")			
			["body", "head", "nose_root", "elbows", "wrists", "knees", "ankles"].each do |part_name|								
				if pdj_results[network_name][part_name].nil?
					fout.write("--- ")
				else
					fout.write("#{sprintf("%.1f", pdj_results[network_name][part_name]*100.0)} ")
				end
				if part_name == "ankles"
					fout.write("\\\\\n")
				else
					fout.write("& ")
				end
			end
		end
	end
end

#############
# anime set #
#############
anime_tasks = {
	:train => {},
	:test => {},
	:val => {},
}	

[227,224].each do |image_size|		
	anime_tasks[:test][image_size] = {}
	anime_tasks[:test][image_size][:source] = ResizeExampleTasks.new(
		"poseest_anime_test_im#{image_size}_source",
		"data/poseest/anime/test/im#{image_size}/source",
		:source_data_list => "data/poseest/anime/test/raw/data_list.txt",
		:source_data_count => "data/poseest/anime/test/raw/data_count.txt",
		:limit => 1000,	
		:target_width => image_size,
		:target_height => image_size,		
	)

	anime_tasks[:test][image_size][:mmd_18] = CaffePoseData.new(
		"poseest_anime_test_im#{image_size}_pose_mmd_18",
		"data/poseest/anime/test/im#{image_size}/mmd_18",
		:data_list_file => anime_tasks[:test][image_size][:source].data_list_file_name,
		:pose_2d_config => $p2dc_mmd_18,
		:pose_xform => $p2xf_project_mmd_18,
		:image_width => image_size,
		:image_height => image_size
	)			

	anime_tasks[:test][image_size][:mmd_21] = CaffePoseData.new(
		"poseest_anime_test_im#{image_size}_pose_mmd_21",
		"data/poseest/anime/test/im#{image_size}/mmd_21",
		:data_list_file => anime_tasks[:test][image_size][:source].data_list_file_name,
		:pose_2d_config => $p2dc_mmd_21,
		:pose_xform => $p2xf_project_mmd_21,
		:image_width => image_size,
		:image_height => image_size
	)			
end

anime_tasks[:train][:configs] = 
	RotateScaleTranslateTransformConfig.new("poseest_anime_train_configs",
		"data/poseest/anime/train/configs",
		:source_data_list => "data/poseest/anime/train/raw/data_list.txt",
		:source_data_count => "data/poseest/anime/train/raw/data_count.txt",
		:count => 1000000,
		:limit => 1000,)

anime_tasks[:train][:raw_resized] = 
	JointBasedResizeExampleTasks.new("poseest_anime_train_raw_resized",
		"data/poseest/anime/train/raw_resized",
		:source_data_list => "data/poseest/anime/train/raw/data_list.txt",
		:source_data_count => "data/poseest/anime/train/raw/data_count.txt",
		:limit => 1000,
		:target_width => 400,
		:target_height => 400,
		:scale_factor => 1.2,
		:eye_to_head => 0.1,)

anime_tasks[:train][:distrib] = {}

anime_tasks[:train][:distrib][:leg_left] = ChainDistribTasks.new(
	"poseest_anime_train_distrib_leg_left",
	"data/poseest/anime/train/distrib/leg_left",
	:data_list_file => "data/poseest/anime/train/raw_resized/data_list.txt",
	:chain => ["leg_left", "knee_left", "ankle_left"],
	:pose_2d_config => $p2dc_mmd_18,	
	:image_width => 400,
	:image_height => 400,
	:colors => [[0,0,1],[0,1,0]])

anime_tasks[:train][:distrib][:arm_left] = ChainDistribTasks.new(
	"poseest_anime_train_distrib_arm_left",
	"data/poseest/anime/train/distrib/arm_left",
	:data_list_file => "data/poseest/anime/train/raw_resized/data_list.txt",
	:chain => ["arm_left", "elbow_left", "wrist_left"],
	:pose_2d_config => $p2dc_mmd_18,	
	:image_width => 400,
	:image_height => 400,
	:colors => [[0,0,1],[0,1,0]])

anime_tasks[:val][:configs] = 
	RotateScaleTranslateTransformConfig.new("poseest_anime_val_configs",
		"data/poseest/anime/val/configs",
		:source_data_list => "data/poseest/anime/val/raw/data_list.txt",
		:source_data_count => "data/poseest/anime/val/raw/data_count.txt",
		:count => 2000,
		:limit => 1000,)

begin
	anime_tasks[:paper] = {}
	
	anime_tasks[:paper][:configs] = 
		RotateScaleTranslateTransformConfig.new("poseest_anime_paper_configs",
			"data/poseest/anime/paper/configs",
			:source_data_list => "data/poseest/anime/paper/raw/data_list.txt",
			:source_data_count => "data/poseest/anime/paper/raw/data_count.txt",
			:count => 20,
			:limit => 1000,)

	anime_tasks[:paper][:source] = RotateScaleTranslateTransformExample.new(
			"poseest_anime_paper_source",
			"data/poseest/anime/paper/source",
			:config_tasks => anime_tasks[:paper][:configs],			
			:image_width => 512,
			:image_height => 512,
			:rotate_min => -30,			
			:rotate_max => 30,
			:scale_min => 1.05,
			:scale_max => 1.2,
			:check_pose => false,
			:eye_to_head => 0.05)

	anime_tasks[:paper][:source] = CaffePoseData.new(
			"poseest_anime_paper_pose",
			"data/poseest/anime/paper/pose",
			:data_list_file => "data/poseest/anime/paper/source/data_list.txt",
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21,
			:image_width => 512,
			:image_height => 512,
			:check_image => false)
end

[:train, :val].each do |data_set_index|
	[227,224].each do |image_size|
		anime_tasks[data_set_index][image_size] = {}
		anime_tasks[data_set_index][image_size][:source] = RotateScaleTranslateTransformExample.new(
			"poseest_anime_#{data_set_index}_im#{image_size}_source",
			"data/poseest/anime/#{data_set_index}/im#{image_size}/source",
			:config_tasks => anime_tasks[data_set_index][:configs],			
			:image_width => image_size,
			:image_height => image_size,
			:rotate_min => -30,			
			:rotate_max => 30,
			:scale_min => 1.05,
			:scale_max => 1.2,
			:check_pose => false,
			:eye_to_head => 0.05)

		anime_tasks[data_set_index][image_size][:image] = CaffeImageData.new(
			"poseest_anime_#{data_set_index}_im#{image_size}_image",
			"data/poseest/anime/#{data_set_index}/im#{image_size}",
			:data_list_file => "data/poseest/anime/#{data_set_index}/im#{image_size}/source/data_list.txt",
			:image_width => image_size,
			:image_height => image_size
		)
		
		anime_tasks[data_set_index][image_size][:mmd_18] = CaffePoseData.new(
			"poseest_anime_#{data_set_index}_im#{image_size}_pose_mmd_18",
			"data/poseest/anime/#{data_set_index}/im#{image_size}/mmd_18",
			:data_list_file => "data/poseest/anime/#{data_set_index}/im#{image_size}/source/data_list.txt",
			:pose_2d_config => $p2dc_mmd_18,
			:pose_xform => $p2xf_project_mmd_18,
			:image_width => image_size,
			:image_height => image_size,
			:check_image => false,
		)

		anime_tasks[data_set_index][image_size][:mmd_21] = CaffePoseData.new(
			"poseest_anime_#{data_set_index}_im#{image_size}_pose_mmd_21",
			"data/poseest/anime/#{data_set_index}/im#{image_size}/mmd_21",
			:data_list_file => "data/poseest/anime/#{data_set_index}/im#{image_size}/source/data_list.txt",
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21,
			:image_width => image_size,
			:image_height => image_size,
			:check_image => false,
		)

		anime_tasks[data_set_index][image_size][:mmd_21_weights] = CaffePoseData.new(
			"poseest_anime_#{data_set_index}_im#{image_size}_pose_mmd_21_weights",
			"data/poseest/anime/#{data_set_index}/im#{image_size}/mmd_21_weights",
			:data_list_file => "data/poseest/anime/#{data_set_index}/im#{image_size}/source/data_list.txt",
			:pose_2d_config => $p2dc_mmd_21_weights,
			:pose_xform => $p2xf_mmd_21_add_weights,
			:image_width => image_size,
			:image_height => image_size,
			:check_image => false
		)
	end
end

$poseest_anime_alexnet = CaffeSimplePoseRegressor.new("poseest_anime_alexnet",	
	"data/poseest/anime/alexnet", 
	:image_mean_file => anime_tasks[:train][227][:image].per_channel_image_mean_file_name,
	
	:train_image_lmdb_file => anime_tasks[:train][227][:image].image_lmdb_file_name,
	:train_pose_lmdb_file => anime_tasks[:train][227][:mmd_21_weights].pose_lmdb_file_name,
	:val_image_lmdb_file => anime_tasks[:val][227][:image].image_lmdb_file_name,
	:val_pose_lmdb_file => anime_tasks[:val][227][:mmd_21_weights].pose_lmdb_file_name,

	:base_lr => 1e-4,	
	:snapshot => 100000,

	:test_iter => 200,
	:val_batch_size => 10,
	:test_interval => 1000,	

	:train_batch_size => 50,
	:max_iter => 400000,
	:stepsize => 400000,
	:epoch_iter_count => 2000,

	:display => 100,

	:web_service_port => 9292,

	:finetune_from => "data/caffe/bvlc_alexnet.caffemodel",

	:bone_count => $p2dc_mmd_21_weights.bone_names.count,

	:inner_product_layer_std => 0.001,
	:network_type => :alexnet)

CaffeSimpleRegressorTryLearningRates.new("poseest_anime_alexnet_trial", 
	"data/poseest/anime/alexnet_trial", 
	:image_mean_file => anime_tasks[:train][227][:image].per_channel_image_mean_file_name,
	
	:train_image_lmdb_file => anime_tasks[:train][227][:image].image_lmdb_file_name,
	:train_pose_lmdb_file => anime_tasks[:train][227][:mmd_21_weights].pose_lmdb_file_name,
	:val_image_lmdb_file => anime_tasks[:val][227][:image].image_lmdb_file_name,
	:val_pose_lmdb_file => anime_tasks[:val][227][:mmd_21_weights].pose_lmdb_file_name,

	:base_lr => [1e-3, 3e-4, 1e-4, 3e-5, 1e-5],	
	:snapshot => 10000,

	:test_iter => 200,
	:val_batch_size => 10,
	:test_interval => 1000,	

	:train_batch_size => 50,
	:max_iter => 10000,
	:stepsize => 10000,
	:epoch_iter_count => 2000,

	:display => 100,

	:web_service_port => 9292,

	:finetune_from => "data/caffe/bvlc_alexnet.caffemodel",

	:bone_count => $p2dc_mmd_21_weights.bone_names.count,

	:inner_product_layer_std => 0.001,
	:network_type => :alexnet)

$poseest_anime_googlenet = CaffeSimplePoseRegressor.new(
	"poseest_anime_googlenet", 
	"data/poseest/anime/googlenet", 
	:image_mean_file => anime_tasks[:train][224][:image].per_channel_image_mean_file_name,

	:train_image_lmdb_file => anime_tasks[:train][224][:image].image_lmdb_file_name,
	:train_pose_lmdb_file => anime_tasks[:train][224][:mmd_21_weights].pose_lmdb_file_name,
	:val_image_lmdb_file => anime_tasks[:val][224][:image].image_lmdb_file_name,
	:val_pose_lmdb_file => anime_tasks[:val][224][:mmd_21_weights].pose_lmdb_file_name,

	:base_lr => 1e-4,	
	:snapshot => 100000,

	:test_iter => 400,
	:val_batch_size => 5,
	:test_interval => 1000,	

	:train_batch_size => 10,
	:max_iter => 400000,
	:stepsize => 100000,
	:epoch_iter_count => 2000,

	:display => 100,

	:web_service_port => 9292,

	:finetune_from => "data/caffe/bvlc_googlenet.caffemodel",

	:bone_count => $p2dc_mmd_21_weights.bone_names.count,

	:gamma => 0.96,
	:momentum => 0.9,
	:weight_decay => 0.0002,
	:inner_product_layer_std => 0.001,
	:network_type => :googlenet)

$poseest_anime_alexnet_finetune = CaffeSimplePoseRegressor.new("poseest_anime_alexnet_finetune",	
	"data/poseest/anime/alexnet_finetune", 
	:image_mean_file => anime_tasks[:train][227][:image].per_channel_image_mean_file_name,
	
	:train_image_lmdb_file => anime_tasks[:train][227][:image].image_lmdb_file_name,
	:train_pose_lmdb_file => anime_tasks[:train][227][:mmd_21_weights].pose_lmdb_file_name,
	:val_image_lmdb_file => anime_tasks[:val][227][:image].image_lmdb_file_name,
	:val_pose_lmdb_file => anime_tasks[:val][227][:mmd_21_weights].pose_lmdb_file_name,

	:base_lr => 1e-4,	
	:snapshot => 100000,

	:test_iter => 200,
	:val_batch_size => 10,
	:test_interval => 1000,	

	:train_batch_size => 50,
	:max_iter => 400000,
	:stepsize => 400000,
	:epoch_iter_count => 2000,

	:display => 100,

	:web_service_port => 9292,

	:finetune_from => $poseest_mmd_big2_alexnet.caffemodel_file_name(3),

	:bone_count => $p2dc_mmd_21_weights.bone_names.count,

	:inner_product_layer_std => 0.001,
	:network_type => :alexnet)

$poseest_anime_googlenet_finetune = CaffeSimplePoseRegressor.new(
	"poseest_anime_googlenet_finetune", 
	"data/poseest/anime/googlenet_finetune", 
	:image_mean_file => anime_tasks[:train][224][:image].per_channel_image_mean_file_name,

	:train_image_lmdb_file => anime_tasks[:train][224][:image].image_lmdb_file_name,
	:train_pose_lmdb_file => anime_tasks[:train][224][:mmd_21_weights].pose_lmdb_file_name,
	:val_image_lmdb_file => anime_tasks[:val][224][:image].image_lmdb_file_name,
	:val_pose_lmdb_file => anime_tasks[:val][224][:mmd_21_weights].pose_lmdb_file_name,

	:base_lr => 1e-4,	
	:snapshot => 100000,

	:test_iter => 400,
	:val_batch_size => 5,
	:test_interval => 1000,	

	:train_batch_size => 10,
	:max_iter => 400000,
	:stepsize => 100000,
	:epoch_iter_count => 2000,

	:display => 100,

	:web_service_port => 9292,

	:finetune_from => $poseest_mmd_big2_googlenet.caffemodel_file_name(3),

	:bone_count => $p2dc_mmd_21_weights.bone_names.count,

	:gamma => 0.96,
	:momentum => 0.9,
	:weight_decay => 0.0002,
	:inner_product_layer_std => 0.001,
	:network_type => :googlenet)

networks[:anime] = {
	:alexnet => {
		:name => "anime_alexnet",
		:pose_2d_config => $p2dc_mmd_21_weights,
		:tasks => $poseest_anime_alexnet,
		:caffemodel_file => $poseest_anime_alexnet.caffemodel_file_name(2),
		:image_mean_file => anime_tasks[:train][227][:image].per_channel_image_mean_npy_file_name,
		:image_width => 227,
		:image_height => 227,
	},
	:googlenet => {
		:name => "anime_googlenet",
		:pose_2d_config => $p2dc_mmd_21_weights,
		:tasks => $poseest_anime_googlenet,
		:caffemodel_file => $poseest_anime_googlenet.caffemodel_file_name(3),
		:image_mean_file => anime_tasks[:train][224][:image].per_channel_image_mean_npy_file_name,
		:image_width => 224,
		:image_height => 224,
	},
	:alexnet_finetune => {
		:name => "anime_alexnet_finetune",
		:pose_2d_config => $p2dc_mmd_21_weights,
		:tasks => $poseest_anime_alexnet_finetune,
		:caffemodel_file => $poseest_anime_alexnet_finetune.caffemodel_file_name(2),
		:image_mean_file => anime_tasks[:train][227][:image].per_channel_image_mean_npy_file_name,
		:image_width => 227,
		:image_height => 227,
	},
	:googlenet_finetune => {
		:name => "anime_googlenet_finetune",
		:pose_2d_config => $p2dc_mmd_21_weights,
		:tasks => $poseest_anime_googlenet_finetune,
		:caffemodel_file => $poseest_anime_googlenet_finetune.caffemodel_file_name(3),
		:image_mean_file => anime_tasks[:train][224][:image].per_channel_image_mean_npy_file_name,
		:image_width => 224,
		:image_height => 224,
	}
}

# predictions
[:val, :test].each do |data_set_index|
	anime_tasks[data_set_index][:predict] = {}
	[
		networks[:mmd_big2][:alexnet],
		networks[:mmd_big2][:googlenet],
		networks[:anime][:alexnet],
		networks[:anime][:googlenet],
		networks[:anime][:alexnet_finetune],
		networks[:anime][:googlenet_finetune],
	].each do |network|
		network_name = network[:name]
		image_size = network[:image_width]

		anime_tasks[data_set_index][:predict][network_name] = {}

		anime_tasks[data_set_index][:predict][network_name][:predict] = CaffePredictionTasks.new(
			"poseest_anime_#{data_set_index.to_s}_predict_#{network_name}",
			"data/poseest/anime/#{data_set_index.to_s}/predict/#{network_name}",
			:file_list => FileList.new("data/poseest/anime/#{data_set_index.to_s}/im#{image_size}/source/data-*/data_*.png"),
			:pose_2d_config => network[:pose_2d_config],
			:caffe_deploy_file => network[:tasks].deploy_file_name,
			:caffemodel_file => network[:caffemodel_file],
			:image_mean_npy_file => network[:image_mean_file],
			:image_width => network[:image_width],
			:image_height => network[:image_height],
			:original_data_list => "data/poseest/anime/#{data_set_index}/raw/data_list.txt",
			:overlay_pose_2d_config => $p2dc_mmd_21,
			:point_width => 20,
			:line_width => 10,
		)

		anime_tasks[data_set_index][:predict][network_name][:pdj] = PDJEvaluation.new(
			"poseest_anime_#{data_set_index.to_s}_predict_#{network_name}_pdj",
			"data/poseest/anime/#{data_set_index.to_s}/predict/#{network_name}/pdj",
			:prediction_file => anime_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => anime_tasks[data_set_index][image_size][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21
		)		
		
		anime_tasks[data_set_index][:predict][network_name][:pcp] = StrictPCPEvaluation.new(
			"poseest_anime_#{data_set_index}_predict_#{network_name}_pcp", 
			"data/poseest/anime/#{data_set_index}/predict/#{network_name}/pcp",
			:prediction_file => anime_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => anime_tasks[data_set_index][image_size][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21,
			:threshold => 0.5,
		)
	end
end

PDJComparison.new(
	"poseest_anime_val_predict_compare_alexnet_googlenet", 
	"data/poseest/anime/val/predict/compare_alexnet_googlenet",
	:pdj_files => [
		["AlexNet", "data/poseest/anime/val/predict/mmd_big2_alexnet/pdj/pdj.txt"],
		["GoogleNet", "data/poseest/anime/val/predict/mmd_big2_googlenet/pdj/pdj.txt"],			
	],
	:pose_2d_config => $p2dc_mmd_21)

PDJComparison.new(
	"poseest_anime_test_predict_compare_alexnet_googlenet", 
	"data/poseest/anime/test/predict/compare_alexnet_googlenet",
	:pdj_files => [
		["AlexNet", "data/poseest/anime/test/predict/mmd_big2_alexnet/pdj/pdj.txt"],
		["GoogleNet", "data/poseest/anime/test/predict/mmd_big2_googlenet/pdj/pdj.txt"],			
	],
	:pose_2d_config => $p2dc_mmd_21)

PDJComparison.new(
	"poseest_anime_test_predict_compare_all", 
	"data/poseest/anime/test/predict/compare_all",
	:pdj_files => [
		#["MMD AlexNet", "data/poseest/anime/test/predict/mmd_big2_alexnet/pdj/pdj.txt"],
		["Synth", "data/poseest/anime/test/predict/mmd_big2_googlenet/pdj/pdj.txt"],
		#["Anime AlexNet", "data/poseest/anime/test/predict/anime_alexnet/pdj/pdj.txt"],		
		["Draw", "data/poseest/anime/test/predict/anime_googlenet/pdj/pdj.txt"],		
		#["Anime AlexNet (finetune)", "data/poseest/anime/test/predict/anime_alexnet_finetune/pdj/pdj.txt"],		
		["Synth-Draw", "data/poseest/anime/test/predict/anime_googlenet_finetune/pdj/pdj.txt"],		
	],
	:pose_2d_config => $p2dc_mmd_21)

task :poseest_anime_experiment do
	pcp_results = {}
	pdj_results = {}


	list = []
	500.times do |index|
		list << index
	end	
	# remove too sexy pictures
	too_sexy = [4, 105,411, 100, 265, 1, 138, 211, 342, 91, 410, 204, 10, 26, 498, 176, 307,
		316, 344, 295, 326, 24, 205, 395, 391, 349, 276, 458, 330, 83, 51, 443, 278, 141, 92, 41,
		414, 82, 480, 97, 447, 305, 47, 209, 387, 198, 389, 164, 232, 144, 477, 461, 146, 21, 385,
		193, # no contact
		219,
		459, # hp indicates no requests
		431, 78, 244, 299,
		297, # user does not exist
		77,
		234,
		432,
		228, # not too much skin per se, but the post is still risque
		444, 412,
		333, 177, 66, 348, 262,
		11, # user seem inactive since 2011,
		350,
		488, # no source
		129,
		285,
		436, # no info on artist
		430,
		478,
		143, 170, 86, 63, 386, 388, 180, 286, 57, 140
	]
	too_sexy.each do |i|
		list[i] = nil
	end
	list.compact!
	list.shuffle!
	random_examples = list[0..7]
	target_dir = "/home/pramook/pramook/workspace/animepose-paper/images/anime_results"
	FileUtils.mkdir_p(target_dir)
	labelings = JSON.parse(File.read("data/poseest/anime/test/raw/labeling_2d.txt"))
	File.open("#{target_dir}/indices.txt","w") do |fout|
		8.times do |i|
			fout.write(random_examples[i])
			fout.write(" ")
			fout.write(labelings[random_examples[i]]["file_name"])
			fout.write("\n")
		end
	end

	["mmd_big2_googlenet", "anime_googlenet", "anime_googlenet_finetune"].each do |network_name|
		prediction_task = anime_tasks[:test][:predict][network_name][:predict]
		#Rake::Task[prediction_task.name + ":raw_prediction_clean"].invoke
		Rake::Task[prediction_task.name + ":raw_prediction"].invoke
		#Rake::Task[prediction_task.name + ":labeling_2d_clean"].invoke
		Rake::Task[prediction_task.name + ":labeling_2d"].invoke
		#Rake::Task[prediction_task.name + ":original_overlay_clean"].invoke
		Rake::Task[prediction_task.name + ":original_overlay"].invoke

		target_dir = "/home/pramook/pramook/workspace/animepose-paper/images/anime_results/#{network_name}"
		FileUtils.mkdir_p(target_dir)
		count = 0
		
		#random_examples.each do |index|
		[84,377,399,55,147,345,474,372].each do |index|
			source = "data/poseest/anime/test/predict/#{network_name}/original_overlay/data_#{sprintf("%08d",index)}.png"
			run("cp -f #{source} #{target_dir}/#{sprintf("%02d",count)}.png")
			count += 1
		end
		
		pdj_task = anime_tasks[:test][:predict][network_name][:pdj]
		Rake::Task[pdj_task.name + ":pdj_clean"].invoke
		Rake::Task[pdj_task.name + ":pdj"].invoke
		pdj_results[network_name] = read_pdj_result(pdj_task.pdj_file_name)

		pcp_task = anime_tasks[:test][:predict][network_name][:pcp]
		Rake::Task[pcp_task.name + ":pcp_clean"].invoke
		Rake::Task[pcp_task.name + ":pcp"].invoke
		pcp_results[network_name] = read_pcp_result(pcp_task.pcp_file_name)
	end

	Rake::Task["poseest_anime_test_predict_compare_all:graph_clean"].invoke
	Rake::Task["poseest_anime_test_predict_compare_all:graph"].invoke

	target_dir = "/home/pramook/pramook/workspace/animepose-paper/images/anime_experiment_pdj"
	run("mkdir -p #{target_dir}")
	run("cp -rf data/poseest/anime/test/predict/compare_all/* #{target_dir}")

	run("mkdir -p /home/pramook/pramook/workspace/animepose-paper/data")	
	target_file = "/home/pramook/pramook/workspace/animepose-paper/data/anime_experiment_pcp.txt"
	File.open(target_file, "w") do |fout|
		paper_names = {
			"mmd_big2_googlenet" => "Synth",
			"anime_googlenet" => "Draw",
			"anime_googlenet_finetune" => "Synth-Draw",
		}
		["mmd_big2_googlenet", "anime_googlenet", "anime_googlenet_finetune"].each do |network_name|
			fout.write("\\textsc{#{paper_names[network_name]}} & ")
			["torso", "head", "upper_arms", "lower_arms", "upper_legs", "lower_legs"].each do |part_name|
				if pcp_results[network_name][part_name].nil?
					fout.write("--- ")
				else
					fout.write("#{sprintf("%.1f", pcp_results[network_name][part_name]*100.0)} ")
				end
				if part_name == "lower_legs"
					fout.write("\\\\\n")
				else
					fout.write("& ")
				end
			end
		end
	end

	target_file = "/home/pramook/pramook/workspace/animepose-paper/data/anime_experiment_pdj.txt"
	File.open(target_file, "w") do |fout|
		paper_names = {
			"mmd_big2_googlenet" => "Synth",
			"anime_googlenet" => "Draw",
			"anime_googlenet_finetune" => "Synth-Draw",
		}
		["mmd_big2_googlenet", "anime_googlenet", "anime_googlenet_finetune"].each do |network_name|
			fout.write("\\textsc{#{paper_names[network_name]}} & ")			
			["body", "head", "nose_root", "elbows", "wrists", "knees", "ankles"].each do |part_name|								
				if pdj_results[network_name][part_name].nil?
					fout.write("--- ")
				else
					fout.write("#{sprintf("%.1f", pdj_results[network_name][part_name]*100.0)} ")
				end
				if part_name == "ankles"
					fout.write("\\\\\n")
				else
					fout.write("& ")
				end
			end
		end
	end
end


############
# FLIC set #
############
flic_tasks = {
	:train => {},
	:val => {},
	:test => {},
}

[:train, :val, :test].each do |data_set_index|
	flic_tasks[data_set_index][:raw] = CaffePoseData.new(
		"poseest_flic_#{data_set_index}_raw",
		"data/poseest/flic/#{data_set_index}/raw",
		:data_list_file => "data/poseest/raw/FLIC/#{data_set_index}_data_list.txt",
		:pose_2d_config => $p2dc_flic,
		:pose_xform => $p2xf_project_flic
	)

	if data_set_index == :test
		flic_tasks[data_set_index] = {}
		[227, 224].each do |image_size|
			flic_tasks[data_set_index][image_size] = {}

			flic_tasks[data_set_index][image_size][:source] = JointBasedResizeExampleTasks.new(
				"poseest_flic_#{data_set_index}_im#{image_size}_source",
				"data/poseest/flic/#{data_set_index}/im#{image_size}/source",
				:source_data_list => "data/poseest/raw/FLIC/#{data_set_index}_data_list.txt",
				:source_data_count => "data/poseest/raw/FLIC/#{data_set_index}_data_count.txt",
				:limit => 1000,
				:target_width => image_size,
				:target_height => image_size,
				:scale_factor => 1.5,
			)
		end
	else
		if data_set_index == :val
			count = 2000
		else
			count = 100000
		end
		flic_tasks[data_set_index][:configs] = RotateScaleTranslateTransformConfig.new(
			"poseest_flic_#{data_set_index}_configs",
			"data/poseest/flic/#{data_set_index}/configs",
			:source_data_list => "data/poseest/raw/FLIC/#{data_set_index}_data_list.txt",
			:source_data_count => "data/poseest/raw/FLIC/#{data_set_index}_data_count.txt",
			:count => count,
			:limit => 1000,)
		
		[227, 224].each do |image_size|
			flic_tasks[data_set_index][image_size] = {}

			flic_tasks[data_set_index][image_size][:source] = RotateScaleTranslateTransformExample.new(
				"poseest_flic_#{data_set_index}_im#{image_size}_source",
				"data/poseest/flic/#{data_set_index}/im#{image_size}/source",
				:config_tasks => flic_tasks[data_set_index][:configs],
				:image_width => image_size,
				:image_height => image_size,
				:rotate_min => -30,			
				:rotate_max => 30,
				:scale_min => 1.4,
				:scale_max => 1.5)
		end
	end

	[227,224].each do |image_size|
		flic_tasks[data_set_index][image_size][:image] = CaffeImageData.new(
			"poseest_flic_#{data_set_index}_im#{image_size}_image",
			"data/poseest/flic/#{data_set_index}/im#{image_size}",
			:data_list_file => "data/poseest/flic/#{data_set_index}/im#{image_size}/source/data_list.txt",
			:image_width => image_size,
			:image_height => image_size
		)

		flic_tasks[data_set_index][image_size][:flic_renamed] = CaffePoseData.new(
			"poseest_flic_#{data_set_index}_im#{image_size}_pose_lsp_renamed",
			"data/poseest/flic/#{data_set_index}/im#{image_size}/lsp_renamed",
			:data_list_file => "data/poseest/flic/#{data_set_index}/im#{image_size}/source/data_list.txt",
			:pose_2d_config => $p2dc_flic_renamed,
			:pose_xform => $p2xf_rename_flic,	
			:image_width => image_size,
			:image_height => image_size
		)
	end
end

###########
# LSP set #
###########
lsp_tasks = {
	:train => {},
	:val => {},
	:test => {},	
}

[:train, :val, :test].each do |data_set_index|
	lsp_tasks[data_set_index][:raw] = CaffePoseData.new(
		"poseest_lsp_#{data_set_index}_raw",
		"data/poseest/lsp/#{data_set_index}/raw",
		:data_list_file => "data/poseest/raw/leeds/#{data_set_index}_data_list.txt",
		:pose_2d_config => $p2dc_lsp,
		:pose_xform => $p2xf_project_lsp,
	)

	if data_set_index == :test || data_set_index == :val 
		lsp_tasks[data_set_index] = {}
		[227, 224].each do |image_size|
			lsp_tasks[data_set_index][image_size] = {}

			lsp_tasks[data_set_index][image_size][:source] = ResizeExampleTasks.new(
				"poseest_lsp_#{data_set_index}_im#{image_size}_source",
				"data/poseest/lsp/#{data_set_index}/im#{image_size}/source",
				:source_data_list => "data/poseest/raw/leeds/#{data_set_index}_data_list.txt",
				:source_data_count => "data/poseest/raw/leeds/#{data_set_index}_data_count.txt",
				:limit => 1000,
				:target_width => image_size,
				:target_height => image_size,				
			)			
		end
	else
		lsp_tasks[data_set_index][:configs] = RotateScaleTranslateTransformConfig.new(
			"poseest_lsp_#{data_set_index}_configs",
			"data/poseest/lsp/#{data_set_index}/configs",
			:source_data_list => "data/poseest/raw/leeds/#{data_set_index}_data_list.txt",
			:source_data_count => "data/poseest/raw/leeds/#{data_set_index}_data_count.txt",
			:count => 1000000,
			:limit => 1000,)		
		
		[227, 224].each do |image_size|
			lsp_tasks[data_set_index][image_size] = {}

			lsp_tasks[data_set_index][image_size][:source] = RotateScaleTranslateTransformExample.new(
				"poseest_lsp_#{data_set_index}_im#{image_size}_source",
				"data/poseest/lsp/#{data_set_index}/im#{image_size}/source",
				:config_tasks => lsp_tasks[data_set_index][:configs],
				:image_width => image_size,
				:image_height => image_size,
				:rotat_min => -30,			
				:rotate_max => 30,
				:scale_min => 1.2,
				:scale_max => 1.5,
				:check_pose => false,
				:eye_to_head => 0)
		end
	end

	[227,224].each do |image_size|
		lsp_tasks[data_set_index][image_size][:image] = CaffeImageData.new(
			"poseest_lsp_#{data_set_index}_im#{image_size}_image",
			"data/poseest/lsp/#{data_set_index}/im#{image_size}",
			:data_list_file => "data/poseest/lsp/#{data_set_index}/im#{image_size}/source/data_list.txt",
			:image_width => image_size,
			:image_height => image_size
		)

		lsp_tasks[data_set_index][image_size][:lsp_renamed] = CaffePoseData.new(
			"poseest_lsp_#{data_set_index}_im#{image_size}_pose_lsp_renamed",
			"data/poseest/lsp/#{data_set_index}/im#{image_size}/lsp_renamed",
			:data_list_file => "data/poseest/lsp/#{data_set_index}/im#{image_size}/source/data_list.txt",
			:pose_2d_config => $p2dc_lsp_renamed,
			:pose_xform => $p2xf_rename_lsp,	
			:image_width => image_size,
			:image_height => image_size,
			:check_image => false,
		)

		lsp_tasks[data_set_index][image_size][:lsp_renamed_with_hip] = CaffePoseData.new(
			"poseest_lsp_#{data_set_index}_im#{image_size}_pose_lsp_renamed_with_hip",
			"data/poseest/lsp/#{data_set_index}/im#{image_size}/lsp_renamed_with_hip",
			:data_list_file => "data/poseest/lsp/#{data_set_index}/im#{image_size}/source/data_list.txt",
			:pose_2d_config => $p2dc_lsp_renamed_with_hip,
			:pose_xform => $p2xf_rename_lsp_add_hip,				
			:image_width => image_size,
			:image_height => image_size,
			:check_image => false,
		)

		lsp_tasks[data_set_index][image_size][:lsp_renamed_weights] = CaffePoseData.new(
			"poseest_lsp_#{data_set_index}_im#{image_size}_pose_lsp_renamed_weights",
			"data/poseest/lsp/#{data_set_index}/im#{image_size}/lsp_renamed_weights",
			:data_list_file => "data/poseest/lsp/#{data_set_index}/im#{image_size}/source/data_list.txt",
			:pose_2d_config => $p2dc_lsp_renamed_midlimbs,
			:pose_xform => $p2xf_rename_lsp_add_midlimbs,	
			:image_width => image_size,
			:image_height => image_size,
			:check_image => false,
			:bone_weights => {
				'ankle_right' => 10.0,		
				'knee_right' => 10.0,
				'leg_right' => 1.0,
				'leg_left' => 1.0,
				'knee_left' => 10.0,
				'ankle_left' => 10.0,
				'wrist_right' => 10.0,
				'elbow_right' => 10.0,
				'arm_right' => 1.0,
				'arm_left' => 1.0,
				'elbow_left' => 10.0,
				'wrist_left' => 10.0,
				"neck" => 1.0,
				'head_top' => 1.0,
				"mid_upper_arm_left" => 1.0,
				"mid_upper_arm_right" => 1.0,
				"mid_lower_arm_left" => 10.0,
				"mid_lower_arm_right" => 10.0,
				"mid_upper_leg_left" => 1.0,
				"mid_upper_leg_right" => 1.0,
				"mid_lower_leg_left" => 10.0,
				"mid_lower_leg_right" => 10.0,
			}
		)
	end
end

=begin
the_list = FileList.new("data/poseest/lsp/test/im227/source/data-*/data_*.png")	
$poseest_lsp_test_predict_mmd_big2_alexnet = CaffePredictionTasks.new(
	"poseest_lsp_test_predict_mmd_big2_alexnet",
	"data/poseest/lsp/test/predict/mmd_big2_alexnet",
	:file_list => the_list,
	:pose_2d_config => $p2dc_mmd_21_weights,
	:caffe_deploy_file => $poseest_mmd_big2_alexnet.deploy_file_name,
	:caffemodel_file => $poseest_mmd_big2_alexnet.caffemodel_file_name(3),
	:image_mean_npy_file => $caffe_permuted_mmd_03_0_im227_image.per_channel_image_mean_npy_file_name,
	:image_width => 227,
	:image_height => 227,
)

the_list = FileList.new("data/poseest/lsp/test/im224/source/data-*/data_*.png")	
$poseest_lsp_test_predict_mmd_big2_googlenet = CaffePredictionTasks.new(
	"poseest_lsp_test_predict_mmd_big2_googlenet",
	"data/poseest/lsp/test/predict/mmd_big2_googlenet",
	:file_list => the_list,
	:pose_2d_config => $p2dc_mmd_21_weights,
	:caffe_deploy_file => $poseest_mmd_big2_googlenet.deploy_file_name,
	:caffemodel_file => $poseest_mmd_big2_googlenet.caffemodel_file_name(2),
	:image_mean_npy_file => $caffe_permuted_mmd_03_0_im224_image.per_channel_image_mean_npy_file_name,
	:image_width => 224,
	:image_height => 224,
)
=end

$poseest_lsp_alexnet = CaffeWeightedPoseRegressor.new("poseest_lsp_alexnet",	
	"data/poseest/lsp/alexnet", 
	:image_mean_file => lsp_tasks[:train][227][:image].per_channel_image_mean_file_name,
	
	:train_image_lmdb_file => lsp_tasks[:train][227][:image].image_lmdb_file_name,
	:train_pose_lmdb_file => lsp_tasks[:train][227][:lsp_renamed_weights].pose_lmdb_file_name,
	:train_weight_lmdb_file => lsp_tasks[:train][227][:lsp_renamed_weights].weight_lmdb_file_name,
	:val_image_lmdb_file => lsp_tasks[:val][227][:image].image_lmdb_file_name,
	:val_pose_lmdb_file => lsp_tasks[:val][227][:lsp_renamed_weights].pose_lmdb_file_name,
	:val_weight_lmdb_file => lsp_tasks[:val][227][:lsp_renamed_weights].weight_lmdb_file_name,

	:base_lr => 1e-4,	
	:snapshot => 100000,

	:test_iter => 200,
	:val_batch_size => 10,
	:test_interval => 1000,	

	:train_batch_size => 50,
	:max_iter => 400000,
	:stepsize => 400000,
	:epoch_iter_count => 2000,

	:display => 100,

	:web_service_port => 9292,

	:finetune_from => "data/caffe/bvlc_alexnet.caffemodel",

	:bone_count => $p2dc_lsp_renamed_midlimbs.bone_names.count,

	:inner_product_layer_std => 0.001,
	:network_type => :alexnet)


CaffeWeightedPoseRegressorTryLearningRates.new("poseest_lsp_alexnet_trial", 
	"data/poseest/lsp/alexnet_trial", 
	:image_mean_file => lsp_tasks[:train][227][:image].per_channel_image_mean_file_name,
	
	:train_image_lmdb_file => lsp_tasks[:train][227][:image].image_lmdb_file_name,
	:train_pose_lmdb_file => lsp_tasks[:train][227][:lsp_renamed_weights].pose_lmdb_file_name,
	:train_weight_lmdb_file => lsp_tasks[:train][227][:lsp_renamed_weights].weight_lmdb_file_name,
	:val_image_lmdb_file => lsp_tasks[:val][227][:image].image_lmdb_file_name,
	:val_pose_lmdb_file => lsp_tasks[:val][227][:lsp_renamed_weights].pose_lmdb_file_name,
	:val_weight_lmdb_file => lsp_tasks[:val][227][:lsp_renamed_weights].weight_lmdb_file_name,

	:base_lr => [1e-3, 3e-4, 1e-4, 3e-5, 1e-5],	
	:snapshot => 10000,

	:test_iter => 200,
	:val_batch_size => 10,
	:test_interval => 1000,	

	:train_batch_size => 50,
	:max_iter => 10000,
	:stepsize => 10000,
	:epoch_iter_count => 2000,

	:display => 100,

	:web_service_port => 9292,

	:finetune_from => "data/caffe/bvlc_alexnet.caffemodel",

	:bone_count => $p2dc_lsp_renamed_midlimbs.bone_names.count,

	:inner_product_layer_std => 0.001,
	:network_type => :alexnet)

$poseest_lsp_googlenet = CaffeWeightedPoseRegressor.new("poseest_lsp_googlenet",	
	"data/poseest/lsp/googlenet", 
	:image_mean_file => lsp_tasks[:train][224][:image].per_channel_image_mean_file_name,
	
	:train_image_lmdb_file => lsp_tasks[:train][224][:image].image_lmdb_file_name,
	:train_pose_lmdb_file => lsp_tasks[:train][224][:lsp_renamed_weights].pose_lmdb_file_name,
	:train_weight_lmdb_file => lsp_tasks[:train][224][:lsp_renamed_weights].weight_lmdb_file_name,
	:val_image_lmdb_file => lsp_tasks[:val][224][:image].image_lmdb_file_name,
	:val_pose_lmdb_file => lsp_tasks[:val][224][:lsp_renamed_weights].pose_lmdb_file_name,
	:val_weight_lmdb_file => lsp_tasks[:val][224][:lsp_renamed_weights].weight_lmdb_file_name,

	:base_lr => 1e-4,	
	:snapshot => 100000,

	:test_iter => 200,
	:val_batch_size => 5,
	:test_interval => 1000,	

	:train_batch_size => 40,
	:max_iter => 400000,
	:stepsize => 100000,
	:epoch_iter_count => 2000,

	:display => 100,

	:web_service_port => 9292,

	:finetune_from => "data/caffe/bvlc_googlenet.caffemodel",

	:bone_count => $p2dc_lsp_renamed_midlimbs.bone_names.count,

	:gamma => 0.96,
	:momentum => 0.9,
	:weight_decay => 0.0002,
	:inner_product_layer_std => 0.001,
	:network_type => :googlenet)

$poseest_lsp_alexnet_finetune = CaffeWeightedPoseRegressor.new("poseest_lsp_alexnet_finetune",	
	"data/poseest/lsp/alexnet_finetune", 
	:image_mean_file => lsp_tasks[:train][227][:image].per_channel_image_mean_file_name,
	
	:train_image_lmdb_file => lsp_tasks[:train][227][:image].image_lmdb_file_name,
	:train_pose_lmdb_file => lsp_tasks[:train][227][:lsp_renamed_weights].pose_lmdb_file_name,
	:train_weight_lmdb_file => lsp_tasks[:train][227][:lsp_renamed_weights].weight_lmdb_file_name,
	:val_image_lmdb_file => lsp_tasks[:val][227][:image].image_lmdb_file_name,
	:val_pose_lmdb_file => lsp_tasks[:val][227][:lsp_renamed_weights].pose_lmdb_file_name,
	:val_weight_lmdb_file => lsp_tasks[:val][227][:lsp_renamed_weights].weight_lmdb_file_name,

	:base_lr => 1e-4,	
	:snapshot => 100000,

	:test_iter => 200,
	:val_batch_size => 10,
	:test_interval => 1000,	

	:train_batch_size => 40,
	:max_iter => 400000,
	:stepsize => 400000,
	:epoch_iter_count => 2000,

	:display => 100,

	:web_service_port => 9292,

	:finetune_from => $poseest_mmd_big2_alexnet.caffemodel_file_name(3),

	:bone_count => $p2dc_lsp_renamed_midlimbs.bone_names.count,

	:inner_product_layer_std => 0.001,
	:network_type => :alexnet)

$poseest_lsp_googlenet_finetune = CaffeWeightedPoseRegressor.new("poseest_lsp_googlenet_finetune",	
	"data/poseest/lsp/googlenet_finetune", 
	:image_mean_file => lsp_tasks[:train][224][:image].per_channel_image_mean_file_name,
	
	:train_image_lmdb_file => lsp_tasks[:train][224][:image].image_lmdb_file_name,
	:train_pose_lmdb_file => lsp_tasks[:train][224][:lsp_renamed_weights].pose_lmdb_file_name,
	:train_weight_lmdb_file => lsp_tasks[:train][224][:lsp_renamed_weights].weight_lmdb_file_name,
	:val_image_lmdb_file => lsp_tasks[:val][224][:image].image_lmdb_file_name,
	:val_pose_lmdb_file => lsp_tasks[:val][224][:lsp_renamed_weights].pose_lmdb_file_name,
	:val_weight_lmdb_file => lsp_tasks[:val][224][:lsp_renamed_weights].weight_lmdb_file_name,

	:base_lr => 1e-4,	
	:snapshot => 100000,

	:test_iter => 200,
	:val_batch_size => 5,
	:test_interval => 1000,	

	:train_batch_size => 40,
	:max_iter => 400000,
	:stepsize => 100000,
	:epoch_iter_count => 2000,

	:display => 100,

	:web_service_port => 9292,

	:finetune_from => $poseest_mmd_big2_googlenet.caffemodel_file_name(3),

	:bone_count => $p2dc_lsp_renamed_midlimbs.bone_names.count,

	:gamma => 0.96,
	:momentum => 0.9,
	:weight_decay => 0.0002,
	:inner_product_layer_std => 0.001,
	:network_type => :googlenet)

networks[:lsp] = {	
	:googlenet => {
		:name => "lsp_googlenet",
		:pose_2d_config => $p2dc_lsp_renamed_midlimbs,
		:tasks => $poseest_lsp_googlenet,
		:caffemodel_file => $poseest_lsp_googlenet.caffemodel_file_name(3),
		:image_mean_file => lsp_tasks[:train][224][:image].per_channel_image_mean_npy_file_name,
		:image_width => 224,
		:image_height => 224,
	},	
	:googlenet_finetune => {
		:name => "lsp_googlenet_finetune",
		:pose_2d_config => $p2dc_lsp_renamed_midlimbs,
		:tasks => $poseest_lsp_googlenet_finetune,
		:caffemodel_file => $poseest_lsp_googlenet_finetune.caffemodel_file_name(3),
		:image_mean_file => lsp_tasks[:train][224][:image].per_channel_image_mean_npy_file_name,
		:image_width => 224,
		:image_height => 224,
	}
}

# predictions
[:train, :val, :test].each do |data_set_index|	
	lsp_tasks[data_set_index][:predict] = {}

	[				
		networks[:mmd_big2][:googlenet],
		networks[:lsp][:googlenet],
		networks[:lsp][:googlenet_finetune],
	].each do |network|
		network_name = network[:name]
		image_size = network[:image_width]

		lsp_tasks[data_set_index][:predict][network_name] = {}

		if data_set_index == :train
			lsp_tasks[data_set_index][:predict][network_name][:predict] = CaffePredictionTasks.new(
				"poseest_lsp_#{data_set_index.to_s}_predict_#{network_name}",
				"data/poseest/lsp/#{data_set_index.to_s}/predict/#{network_name}",
				:file_list => FileList.new("data/poseest/lsp/#{data_set_index.to_s}/im#{image_size}/source/data-00000/data_*.png"),
				:pose_2d_config => network[:pose_2d_config],
				:caffe_deploy_file => network[:tasks].deploy_file_name,
				:caffemodel_file => network[:caffemodel_file],
				:image_mean_npy_file => network[:image_mean_file],				
				:image_width => network[:image_width],
				:image_height => network[:image_height],				
			)
		else
			lsp_tasks[data_set_index][:predict][network_name][:predict] = CaffePredictionTasks.new(
				"poseest_lsp_#{data_set_index.to_s}_predict_#{network_name}",
				"data/poseest/lsp/#{data_set_index.to_s}/predict/#{network_name}",
				:file_list => FileList.new("data/poseest/lsp/#{data_set_index.to_s}/im#{image_size}/source/data-*/data_*.png"),
				:pose_2d_config => network[:pose_2d_config],
				:caffe_deploy_file => network[:tasks].deploy_file_name,
				:caffemodel_file => network[:caffemodel_file],
				:image_mean_npy_file => network[:image_mean_file],
				#:image_mean_npy_file => lsp_tasks[:train][224][:image].per_channel_image_mean_npy_file_name,
				:image_width => network[:image_width],
				:image_height => network[:image_height],
				:original_data_list => "data/poseest/raw/leeds/#{data_set_index}_data_list.txt",
				:overlay_pose_2d_config => $p2dc_lsp_renamed,
			)
		end

		if network_name == "mmd_big2_googlenet"			
			combo_edges = [
				["upper_arm_left", [
					["arm_left", "elbow_left"]
				]],
				["upper_arm_right", [
					["arm_right", "elbow_right"]
				]],
				["upper_arms", [
					["arm_left", "elbow_left"], 
					["arm_right", "elbow_right"]
				]],
				["lower_arm_left", [
					["elbow_left", "wrist_left"]
				]],
				["lower_arm_right", [
					["elbow_right", "wrist_right"]
				]],
				["lower_arms", [
					["elbow_left", "wrist_left"],
					["elbow_right", "wrist_right"]
				]],
				["upper_leg_left", [
					["leg_left", "knee_left"]
				]],
				["upper_leg_right", [
					["leg_right", "knee_right"]
				]],
				["upper_legs", [
					["leg_left", "knee_left"],
					["leg_right", "knee_right"]
				]],
				["lower_leg_left", [
					["knee_left", "ankle_left"]
				]],
				["lower_leg_right", [
					["knee_right", "ankle_right"]
				]],
				["lower_legs", [
					["knee_left", "ankle_left"],
					["knee_right", "ankle_right"]
				]],							
			]
		else			
			combo_edges = [
				["upper_arm_left", [
					["arm_left", "elbow_left"]
				]],
				["upper_arm_right", [
					["arm_right", "elbow_right"]
				]],
				["upper_arms", [
					["arm_left", "elbow_left"], 
					["arm_right", "elbow_right"]
				]],
				["lower_arm_left", [
					["elbow_left", "wrist_left"]
				]],
				["lower_arm_right", [
					["elbow_right", "wrist_right"]
				]],
				["lower_arms", [
					["elbow_left", "wrist_left"],
					["elbow_right", "wrist_right"]
				]],
				["upper_leg_left", [
					["leg_left", "knee_left"]
				]],
				["upper_leg_right", [
					["leg_right", "knee_right"]
				]],
				["upper_legs", [
					["leg_left", "knee_left"],
					["leg_right", "knee_right"]
				]],
				["lower_leg_left", [
					["knee_left", "ankle_left"]
				]],
				["lower_leg_right", [
					["knee_right", "ankle_right"]
				]],
				["lower_legs", [
					["knee_left", "ankle_left"],
					["knee_right", "ankle_right"]
				]],				
				["head", [
					["neck", "head_top"],
				]],
				["torso", [
					["hip", "neck"],
				]],
			]	
		end

		lsp_tasks[data_set_index][:predict][network_name][:pdj] = PDJEvaluation.new(
			"poseest_lsp_#{data_set_index.to_s}_predict_#{network_name}_pdj",
			"data/poseest/lsp/#{data_set_index.to_s}/predict/#{network_name}/pdj",
			:prediction_file => lsp_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => lsp_tasks[data_set_index][image_size][:lsp_renamed_with_hip].labeling_2d_file_name,
			:pose_2d_config => $p2dc_lsp_renamed_with_hip,
			:pose_xform => $p2xf_project_lsp_add_hip,
			:combo_bones => [
				["shoulders", ["arm_left", "arm_right"]],
				["elbows", ["elbow_left", "elbow_right"]],
				["wrists", ["wrist_left", "wrist_right"]],
				["hips", ["leg_left", "leg_right"]],
				["knees", ["knee_left", "knee_right"]],
				["ankles", ["ankle_left", "ankle_right"]],
				["hip_left", ["leg_left"]],
				["hip_right", ["leg_right"]],
				["shoulder_left", ["arm_left"]],
				["shoulder_right", ["arm_right"]],				
			]
		)		
		
		lsp_tasks[data_set_index][:predict][network_name][:pcp] = StrictPCPEvaluation.new(
			"poseest_lsp_#{data_set_index}_predict_#{network_name}_pcp", 
			"data/poseest/lsp/#{data_set_index}/predict/#{network_name}/pcp",
			:prediction_file => lsp_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => lsp_tasks[data_set_index][image_size][:lsp_renamed_with_hip].labeling_2d_file_name,
			:pose_2d_config => $p2dc_lsp_renamed_with_hip,
			:pose_xform => $p2xf_project_lsp_add_hip,
			:threshold => 0.5,
			:combo_edges => combo_edges
		)
	end
end

=begin
AverageLossTasks.new("poseest_lsp_val_predict_lsp_googlenet_average_loss",
	"data/poseest/lsp/val/predict/lsp_googlenet",
	:prediction_file => lsp_tasks[:val][:predict]["lsp_googlenet"][:predict].labeling_2d_file_name,
	:answer_file => lsp_tasks[:val][224][:lsp_renamed_weights].labeling_2d_file_name,			
	:image_width => 224,
	:image_height => 224,
	:pose_2d_config => $p2dc_lsp_renamed_midlimbs,
	:bone_weights => {
		'ankle_right' => 10.0,		
		'knee_right' => 10.0,
		'leg_right' => 1.0,
		'leg_left' => 1.0,
		'knee_left' => 10.0,
		'ankle_left' => 10.0,
		'wrist_right' => 10.0,
		'elbow_right' => 10.0,
		'arm_right' => 1.0,
		'arm_left' => 1.0,
		'elbow_left' => 10.0,
		'wrist_left' => 10.0,
		"neck" => 1.0,
		'head_top' => 1.0,
		"mid_upper_arm_left" => 1.0,
		"mid_upper_arm_right" => 1.0,
		"mid_lower_arm_left" => 10.0,
		"mid_lower_arm_right" => 10.0,
		"mid_upper_leg_left" => 1.0,
		"mid_upper_leg_right" => 1.0,
		"mid_lower_leg_left" => 10.0,
		"mid_lower_leg_right" => 10.0,
	}
)
=end


PDJComparison.new(
	"poseest_lsp_test_predict_compare_all", 
	"data/poseest/lsp/test/predict/compare_all",
	:pdj_files => [		
		["Synth", "data/poseest/lsp/test/predict/mmd_big2_googlenet/pdj/pdj.txt"],
		["Lsp", "data/poseest/lsp/test/predict/lsp_googlenet/pdj/pdj.txt"],		
		["Synth-Lsp", "data/poseest/lsp/test/predict/lsp_googlenet_finetune/pdj/pdj.txt"],		
	],
	:pose_2d_config => $p2dc_lsp_renamed_with_hip,
	:combo_bones => [
		["shoulders", ["arm_left", "arm_right"]],
		["elbows", ["elbow_left", "elbow_right"]],
		["wrists", ["wrist_left", "wrist_right"]],
		["knees", ["knee_left", "knee_right"]],
		["ankles", ["ankle_left", "ankle_right"]],
		["hips", ["leg_left", "leg_right"]],
		["hip_left", ["leg_left"]],
		["hip_right", ["leg_right"]],
		["shoulder_left", ["arm_left"]],
		["shoulder_right", ["arm_right"]],
	]
)

def read_pdj_result_lsp(filename)
	pdj = JSON.parse(File.read(filename))
	output = {}
	["hips", "neck", "head_top", "elbows", "wrists", "knees", "ankles"].each do |joint_name|
		if pdj.include?(joint_name)
			output[joint_name] = pdj[joint_name][-1][1] * 1.0 / pdj[joint_name][-1][2]
		else
			output[joint_name] = nil
		end
	end
	output
end

task :poseest_lsp_experiment do
	pcp_results = {}
	pdj_results = {}

	list = []
	1000.times do |index|
		list << index
	end
	list.shuffle!
	random_examples = list[0..11]


	["mmd_big2_googlenet", "lsp_googlenet", "lsp_googlenet_finetune"].each do |network_name|
		prediction_task = lsp_tasks[:test][:predict][network_name][:predict]
		
		#Rake::Task[prediction_task.name + ":raw_prediction_clean"].invoke
		if !File.exists?(prediction_task.raw_prediction_file_name)
			Rake::Task[prediction_task.name + ":raw_prediction"].invoke
		end		
		Rake::Task[prediction_task.name + ":labeling_2d"].invoke
		#Rake::Task[prediction_task.name + ":original_overlay_clean"].invoke
		Rake::Task[prediction_task.name + ":original_overlay"].invoke

		target_dir = "/home/pramook/pramook/workspace/animepose-paper/images/lsp_results/#{network_name}"
		FileUtils.mkdir_p(target_dir)
		count = 0
		random_examples.each do |index|
			source = "data/poseest/lsp/test/predict/#{network_name}/original_overlay/data_#{sprintf("%08d",index)}.png"
			run("cp -f #{source} #{target_dir}/#{sprintf("%02d",count)}.png")
			count += 1
		end
		
		pdj_task = lsp_tasks[:test][:predict][network_name][:pdj]
		Rake::Task[pdj_task.name + ":pdj_clean"].invoke
		Rake::Task[pdj_task.name + ":pdj"].invoke		
		pdj_results[network_name] = read_pdj_result_lsp(pdj_task.pdj_file_name)

		pcp_task = lsp_tasks[:test][:predict][network_name][:pcp]
		Rake::Task[pcp_task.name + ":pcp_clean"].invoke
		Rake::Task[pcp_task.name + ":pcp"].invoke		

		pcp_results[network_name] = read_pcp_result(pcp_task.pcp_file_name)
	end

	Rake::Task["poseest_lsp_test_predict_compare_all:graph_clean"].invoke
	Rake::Task["poseest_lsp_test_predict_compare_all:graph"].invoke

	target_dir = "/home/pramook/pramook/workspace/animepose-paper/images/lsp_experiment_pdj"
	run("mkdir -p #{target_dir}")
	run("cp -rf data/poseest/lsp/test/predict/compare_all/* #{target_dir}")

	run("mkdir -p /home/pramook/pramook/workspace/animepose-paper/data")
	target_file = "/home/pramook/pramook/workspace/animepose-paper/data/lsp_experiment_pcp.txt"
	File.open(target_file, "w") do |fout|
		paper_names = {
			"mmd_big2_googlenet" => "Synth",
			"lsp_googlenet" => "Lsp",
			"lsp_googlenet_finetune" => "Synth-Lsp",
		}
		["mmd_big2_googlenet", "lsp_googlenet", "lsp_googlenet_finetune"].each do |network_name|
			fout.write("\\textsc{#{paper_names[network_name]}} & ")
			["torso", "head", "upper_arms", "lower_arms", "upper_legs", "lower_legs"].each do |part_name|
				if pcp_results[network_name][part_name].nil?
					fout.write("--- ")
				else
					fout.write("#{sprintf("%.1f", pcp_results[network_name][part_name]*100.0)} ")
				end
				if part_name == "lower_legs"
					fout.write("\\\\\n")
				else
					fout.write("& ")
				end
			end
		end
	end

	target_file = "/home/pramook/pramook/workspace/animepose-paper/data/lsp_experiment_pdj.txt"
	File.open(target_file, "w") do |fout|
		paper_names = {
			"mmd_big2_googlenet" => "Synth",
			"lsp_googlenet" => "Lsp",
			"lsp_googlenet_finetune" => "Synth-Lsp",
		}
		["mmd_big2_googlenet", "lsp_googlenet", "lsp_googlenet_finetune"].each do |network_name|
			fout.write("\\textsc{#{paper_names[network_name]}} & ")			
			["hips", "neck", "head_top", "elbows", "wrists", "knees", "ankles"].each do |part_name|								
				if pdj_results[network_name][part_name].nil?
					fout.write("--- ")
				else
					fout.write("#{sprintf("%.1f", pdj_results[network_name][part_name]*100.0)} ")
				end
				if part_name == "ankles"
					fout.write("\\\\\n")
				else
					fout.write("& ")
				end
			end
		end
	end
end

#########################
# Variation Experiments #
#########################

mmd_var_tasks = {}
mmd_var_tasks[100000] = {}
mmd_var_tasks[100000][:configs] = RotateScaleTranslateTransformConfig.new(
	"poseest_mmd_var_100000_configs",
	"data/poseest/mmd_var/100000/configs",
	:source_data_list => "data/mmd_examples/03/0/im224/data_list.txt",
	:source_data_count => "data/mmd_examples/03/0/im224/data_count.txt",
	:count => 1000000,
	:limit => 1000,)
mmd_var_tasks[100000][:source] = RotateScaleTranslateTransformExample.new(
	"poseest_mmd_var_100000_im224_source",
	"data/poseest/mmd_var/100000/im224/source",
	:config_tasks => mmd_var_tasks[100000][:configs],			
	:image_width => 224,
	:image_height => 224,
	:rotate_min => -30,			
	:rotate_max => 30,
	:scale_min => 1.05,
	:scale_max => 1.2,
	:check_pose => false,
	:eye_to_head => 0.05)

[1000, 10000].each do |count|
	mmd_var_tasks[count] = {}
	mmd_var_tasks[count][:configs] = RotateScaleTranslateTransformConfig.new(
		"poseest_mmd_var_#{count}_configs",
		"data/poseest/mmd_var/#{count}/configs",
		:source_data_list => "data/mmd_examples/var/#{count}/im224/data_list.txt",
		:source_data_count => "data/mmd_examples/var/#{count}/im224/data_count.txt",
		:count => 1000000,
		:limit => 1000,)
	mmd_var_tasks[count][:source] = RotateScaleTranslateTransformExample.new(
		"poseest_mmd_var_#{count}_im224_source",
		"data/poseest/mmd_var/#{count}/im224/source",
		:config_tasks => mmd_var_tasks[count][:configs],			
		:image_width => 224,
		:image_height => 224,
		:rotate_min => -30,			
		:rotate_max => 30,
		:scale_min => 1.05,
		:scale_max => 1.2,
		:check_pose => false,
		:eye_to_head => 0.05)
end

[1000,10000,100000].each do |count|	
	[224].each do |image_size|
		mmd_var_tasks[count][:image] = CaffeImageData.new(
			"poseest_mmd_var_#{count}_im#{image_size}_image",
			"data/poseest/mmd_var/#{count}/im#{image_size}",
			:data_list_file => "data/poseest/mmd_var/#{count}/im#{image_size}/source/data_list.txt",
			:image_width => image_size,
			:image_height => image_size
		)

		mmd_var_tasks[count][:pose] = {}
		
		mmd_var_tasks[count][:pose][:mmd_21_weights] = CaffePoseData.new(
			"poseest_mmd_var_#{count}_im#{image_size}_pose_mmd_21_weights",
			"data/poseest/mmd_var/#{count}/im#{image_size}/mmd_21_weights",
			:data_list_file => "data/poseest/mmd_var/#{count}/im#{image_size}/source/data_list.txt",	
			:pose_2d_config => $p2dc_mmd_21_weights,
			:pose_xform => $p2xf_mmd_21_add_weights,
			:image_width => image_size,
			:image_height => image_size
		)	
	end	
end

networks[:mmd_var] = {}
[1000,10000,100000].each do |count|
	network_task = CaffeSimplePoseRegressor.new(
		"poseest_mmd_var_#{count}_googlenet", 
		"data/poseest/mmd_var/#{count}/googlenet", 
		:image_mean_file => mmd_var_tasks[count][:image].per_channel_image_mean_file_name,

		:train_image_lmdb_file => mmd_var_tasks[count][:image].image_lmdb_file_name,
		:train_pose_lmdb_file => mmd_var_tasks[count][:pose][:mmd_21_weights].pose_lmdb_file_name,
		:val_image_lmdb_file => big2_tasks[1][224][:image].image_lmdb_file_name,

		:val_pose_lmdb_file => big2_tasks[1][224][:pose][:mmd_21_weights].pose_lmdb_file_name,

		:base_lr => 0.001,	
		:snapshot => 100000,

		:test_iter => 200,
		:val_batch_size => 10,
		:test_interval => 1000,	

		:train_batch_size => 10,
		:max_iter => 400000,
		:stepsize => 100000,
		:epoch_iter_count => 2000,

		:display => 100,

		:web_service_port => 9292,

		:finetune_from => "data/caffe/bvlc_googlenet.caffemodel",

		:bone_count => $p2dc_mmd_21_weights.bone_names.count,

		:gamma => 0.96,
		:momentum => 0.9,
		:weight_decay => 0.0002,
		:inner_product_layer_std => 0.001,
		:network_type => :googlenet)

	networks[:mmd_var][count] = {
		:name => "mmd_var_#{count}_googlenet",
		:pose_2d_config => $p2dc_mmd_21_weights,
		:tasks => network_task,
		:caffemodel_file => network_task.caffemodel_file_name(3),
		:image_mean_file => mmd_var_tasks[count][:image].per_channel_image_mean_npy_file_name,
		:image_width => 224,
		:image_height => 224,
	}

	finetune_task = CaffeSimplePoseRegressor.new(
		"poseest_anime_mmd_var_#{count}_googlenet_finetune", 
		"data/poseest/anime/mmd_var_#{count}_googlenet_finetune", 
		:image_mean_file => anime_tasks[:train][224][:image].per_channel_image_mean_file_name,
		
		:train_image_lmdb_file => anime_tasks[:train][224][:image].image_lmdb_file_name,
		:train_pose_lmdb_file => anime_tasks[:train][224][:mmd_21_weights].pose_lmdb_file_name,
		:val_image_lmdb_file => anime_tasks[:val][224][:image].image_lmdb_file_name,
		:val_pose_lmdb_file => anime_tasks[:val][224][:mmd_21_weights].pose_lmdb_file_name,

		:base_lr => 0.0001,	
		:snapshot => 100000,

		:test_iter => 200,
		:val_batch_size => 10,
		:test_interval => 1000,	

		:train_batch_size => 10,
		:max_iter => 400000,
		:stepsize => 100000,
		:epoch_iter_count => 2000,

		:display => 100,

		:web_service_port => 9292,

		:finetune_from => network_task.caffemodel_file_name(3),

		:bone_count => $p2dc_mmd_21_weights.bone_names.count,

		:gamma => 0.96,
		:momentum => 0.9,
		:weight_decay => 0.0002,
		:inner_product_layer_std => 0.001,
		:network_type => :googlenet
	)

	networks[:anime][("mmd_var_#{count}_googlenet_finetune").to_sym] = {
		:name => "anime_mmd_var_#{count}_googlenet_finetune",
		:pose_2d_config => $p2dc_mmd_21_weights,
		:tasks => finetune_task,
		:caffemodel_file => finetune_task.caffemodel_file_name(3),
		:image_mean_file => anime_tasks[:train][224][:image].per_channel_image_mean_npy_file_name,
		:image_width => 224,
		:image_height => 224,
	}
end

[2].each do |data_set_index|
	[1000,10000,100000].each do |network|
		network_name = networks[:mmd_var][network][:name]
		image_size = networks[:mmd_var][network][:image_width]

		big2_tasks[data_set_index][:predict][network_name] = {}

		big2_tasks[data_set_index][:predict][network_name][:predict] = CaffePredictionTasks.new(
			"poseest_mmd_big2_#{data_set_index}_predict_#{network_name}",
			"data/poseest/mmd_big2/#{data_set_index}/predict/#{network_name}",
			:file_list => FileList.new("data/mmd_examples/big2/#{data_set_index}/im#{image_size}/data-*/data_*.png"),
			:pose_2d_config => networks[:mmd_var][network][:pose_2d_config],
			:caffe_deploy_file => networks[:mmd_var][network][:tasks].deploy_file_name,
			:caffemodel_file => networks[:mmd_var][network][:caffemodel_file],
			:image_mean_npy_file => networks[:mmd_var][network][:image_mean_file],
			:image_width => networks[:mmd_var][network][:image_width],
			:image_height => networks[:mmd_var][network][:image_height],
		)

		big2_tasks[data_set_index][:predict][network_name][:pdj] = PDJEvaluation.new(
			"poseest_mmd_big2_#{data_set_index}_predict_#{network_name}_pdj",
			"data/poseest/mmd_big2/#{data_set_index}/predict/#{network_name}/pdj",
			:prediction_file => big2_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => big2_tasks[data_set_index][image_size][:pose][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21
		)		
		
		big2_tasks[data_set_index][:predict][network_name][:pcp] = StrictPCPEvaluation.new(
			"poseest_mmd_big2_#{data_set_index}_predict_#{network_name}_pcp", 
			"data/poseest/mmd_big2/#{data_set_index}/predict/#{network_name}/pcp",
			:prediction_file => big2_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => big2_tasks[data_set_index][image_size][:pose][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21,
			:threshold => 0.5,
		)
	end
end

PDJComparison.new(
	"poseest_mmd_big2_2_predict_compare_var", 
	"data/poseest/mmd_big2/2/predict/compare_var",
	:pdj_files => [		
		["Synth-1k", "data/poseest/mmd_big2/2/predict/mmd_var_1000_googlenet/pdj/pdj.txt"],		
		["Synth-10k", "data/poseest/mmd_big2/2/predict/mmd_var_10000_googlenet/pdj/pdj.txt"],				
		["Synth-100k", "data/poseest/mmd_big2/2/predict/mmd_var_100000_googlenet/pdj/pdj.txt"],
		["Synth-1m", "data/poseest/mmd_big2/2/predict/mmd_big2_googlenet/pdj/pdj.txt"],
	],
	:pose_2d_config => $p2dc_mmd_21)

task :poseest_var_mmd_big2_experiment do
	pcp_results = {}
	pdj_results = {}

	["mmd_var_1000_googlenet", "mmd_var_10000_googlenet", "mmd_var_100000_googlenet"].each do |network_name|
		prediction_task = big2_tasks[2][:predict][network_name][:predict]
		#Rake::Task[prediction_task.name + ":raw_prediction_clean"].invoke
		Rake::Task[prediction_task.name + ":raw_prediction"].invoke
		Rake::Task[prediction_task.name + ":labeling_2d_clean"].invoke
		Rake::Task[prediction_task.name + ":labeling_2d"].invoke
		
		pdj_task = big2_tasks[2][:predict][network_name][:pdj]
		Rake::Task[pdj_task.name + ":pdj_clean"].invoke
		Rake::Task[pdj_task.name + ":pdj"].invoke
		pdj_results[network_name] = read_pdj_result(pdj_task.pdj_file_name)

		pcp_task = big2_tasks[2][:predict][network_name][:pcp]
		Rake::Task[pcp_task.name + ":pcp_clean"].invoke
		Rake::Task[pcp_task.name + ":pcp"].invoke
		pcp_results[network_name] = read_pcp_result(pcp_task.pcp_file_name)
	end
	pdj_results["mmd_big2_googlenet"] = read_pdj_result("data/poseest/mmd_big2/2/predict/mmd_big2_googlenet/pdj/pdj.txt")

	Rake::Task["poseest_mmd_big2_2_predict_compare_var:graph_clean"].invoke
	Rake::Task["poseest_mmd_big2_2_predict_compare_var:graph"].invoke

	target_dir = "/home/pramook/pramook/workspace/animepose-paper/images/var_experiment_pdj/mmd_big2"
	run("mkdir -p #{target_dir}")
	run("cp -rf data/poseest/mmd_big2/2/predict/compare_var/* #{target_dir}")

	target_file = "/home/pramook/pramook/workspace/animepose-paper/data/var_mmd_big2_experiment_pdj.txt"
	File.open(target_file, "w") do |fout|
		paper_names = {
			"mmd_var_1000_googlenet" => "Synth-1k",
			"mmd_var_10000_googlenet" => "Synth-10k",
			"mmd_var_100000_googlenet" => "Synth-100k",
			"mmd_big2_googlenet" => "Synth-1m",
		}
		["mmd_var_1000_googlenet", "mmd_var_10000_googlenet", "mmd_var_100000_googlenet", "mmd_big2_googlenet"].each do |network_name|
			fout.write("\\textsc{#{paper_names[network_name]}} & ")			
			["body", "head", "nose_root", "elbows", "wrists", "knees", "ankles"].each do |part_name|								
				if pdj_results[network_name][part_name].nil?
					fout.write("--- ")
				else
					fout.write("#{sprintf("%.1f", pdj_results[network_name][part_name]*100.0)} ")
				end
				if part_name == "ankles"
					fout.write("\\\\\n")
				else
					fout.write("& ")
				end
			end
		end
	end
end

[:test].each do |data_set_index|	
	[1000,10000,100000].each do |network_index|
		network = networks[:anime][("mmd_var_#{network_index}_googlenet_finetune").to_sym]
		network_name = network[:name]
		image_size = network[:image_width]

		anime_tasks[data_set_index][:predict][network_name] = {}

		file_list = FileList.new("data/poseest/anime/#{data_set_index}/im#{image_size}/source/data-*/data_*.png")		
		anime_tasks[data_set_index][:predict][network_name][:predict] = CaffePredictionTasks.new(
			"poseest_anime_#{data_set_index}_predict_#{network_name}",
			"data/poseest/anime/#{data_set_index}/predict/#{network_name}",			
			:file_list => file_list,
			:pose_2d_config => network[:pose_2d_config],
			:caffe_deploy_file => network[:tasks].deploy_file_name,
			:caffemodel_file => network[:caffemodel_file],
			:image_mean_npy_file => network[:image_mean_file],
			:image_width => network[:image_width],
			:image_height => network[:image_height],
			:original_data_list => "data/poseest/anime/#{data_set_index}/raw/data_list.txt",
			:overlay_pose_2d_config => $p2dc_mmd_21,
		)

		anime_tasks[data_set_index][:predict][network_name][:pdj] = PDJEvaluation.new(
			"poseest_anime_#{data_set_index}_predict_#{network_name}_pdj",
			"data/poseest/anime/#{data_set_index}/predict/#{network_name}/pdj",
			:prediction_file => anime_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => anime_tasks[data_set_index][image_size][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21
		)		
		
		anime_tasks[data_set_index][:predict][network_name][:pcp] = StrictPCPEvaluation.new(
			"poseest_anime_#{data_set_index}_predict_#{network_name}_pcp", 
			"data/poseest/anime/#{data_set_index}/predict/#{network_name}/pcp",
			:prediction_file => anime_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => anime_tasks[data_set_index][image_size][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21,
			:threshold => 0.5,
		)
	end

	[1000,10000,100000].each do |network_index|
		network = networks[:mmd_var][network_index]
		network_name = network[:name]
		image_size = network[:image_width]

		anime_tasks[data_set_index][:predict][network_name] = {}

		file_list = FileList.new("data/poseest/anime/#{data_set_index}/im#{image_size}/source/data-*/data_*.png")		
		anime_tasks[data_set_index][:predict][network_name][:predict] = CaffePredictionTasks.new(
			"poseest_anime_#{data_set_index}_predict_#{network_name}",
			"data/poseest/anime/#{data_set_index}/predict/#{network_name}",
			:file_list => file_list,
			:pose_2d_config => network[:pose_2d_config],
			:caffe_deploy_file => network[:tasks].deploy_file_name,
			:caffemodel_file => network[:caffemodel_file],
			:image_mean_npy_file => network[:image_mean_file],
			:image_width => network[:image_width],
			:image_height => network[:image_height],
		)

		anime_tasks[data_set_index][:predict][network_name][:pdj] = PDJEvaluation.new(
			"poseest_anime_#{data_set_index}_predict_#{network_name}_pdj",
			"data/poseest/anime/#{data_set_index}/predict/#{network_name}/pdj",
			:prediction_file => anime_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => anime_tasks[data_set_index][image_size][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21
		)		
		
		anime_tasks[data_set_index][:predict][network_name][:pcp] = StrictPCPEvaluation.new(
			"poseest_anime_#{data_set_index}_predict_#{network_name}_pcp", 
			"data/poseest/anime/#{data_set_index}/predict/#{network_name}/pcp",
			:prediction_file => anime_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => anime_tasks[data_set_index][image_size][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21,
			:threshold => 0.5,
		)
	end
end

PDJComparison.new(
	"poseest_anime_test_predict_compare_var", 
	"data/poseest/anime/test/predict/compare_var",
	:pdj_files => [		
		["Synth-1k-Draw", 
			"data/poseest/anime/test/predict/anime_mmd_var_1000_googlenet_finetune/pdj/pdj.txt",
			"b"],		
		["Synth-10k-Draw", 
			"data/poseest/anime/test/predict/anime_mmd_var_10000_googlenet_finetune/pdj/pdj.txt",
			"g"],				
		["Synth-100k-Draw", 
			"data/poseest/anime/test/predict/anime_mmd_var_100000_googlenet_finetune/pdj/pdj.txt",
			"r"],
		["Synth-1m-Draw", 
			"data/poseest/anime/test/predict/anime_googlenet_finetune/pdj/pdj.txt",
			"c"],
=begin			
		["Synth-1k", 
			"data/poseest/anime/test/predict/mmd_var_1000_googlenet/pdj/pdj.txt",
			"b--"],
		["Synth-10k", 
			"data/poseest/anime/test/predict/mmd_var_10000_googlenet/pdj/pdj.txt",
			"g--"],
		["Synth-100k", 
			"data/poseest/anime/test/predict/mmd_var_100000_googlenet/pdj/pdj.txt",
			"r--"],
		["Synth-1m", 
			"data/poseest/anime/test/predict/mmd_big2_googlenet/pdj/pdj.txt",
			"c--"],
		["Draw",
			"data/poseest/anime/test/predict/anime_googlenet/pdj/pdj.txt",
			"k"]
=end
	],
	:pose_2d_config => $p2dc_mmd_21,
	:show_legends => true)

task :poseest_var_anime_experiment do
	pcp_results = {}
	pdj_results = {}

	["anime_mmd_var_1000_googlenet_finetune", 
	 "anime_mmd_var_10000_googlenet_finetune", 
	 "anime_mmd_var_100000_googlenet_finetune",
	 #"mmd_var_1000_googlenet", 
	 #"mmd_var_10000_googlenet", 
	 #"mmd_var_100000_googlenet",
	].each do |network_name|
		prediction_task = anime_tasks[:test][:predict][network_name][:predict]
		#Rake::Task[prediction_task.name + ":raw_prediction_clean"].invoke
		Rake::Task[prediction_task.name + ":raw_prediction"].invoke
		Rake::Task[prediction_task.name + ":labeling_2d_clean"].invoke
		Rake::Task[prediction_task.name + ":labeling_2d"].invoke
		
		pdj_task = anime_tasks[:test][:predict][network_name][:pdj]
		Rake::Task[pdj_task.name + ":pdj_clean"].invoke
		Rake::Task[pdj_task.name + ":pdj"].invoke
		pdj_results[network_name] = read_pdj_result(pdj_task.pdj_file_name)

		pcp_task = anime_tasks[:test][:predict][network_name][:pcp]
		Rake::Task[pcp_task.name + ":pcp_clean"].invoke
		Rake::Task[pcp_task.name + ":pcp"].invoke
		pcp_results[network_name] = read_pcp_result(pcp_task.pcp_file_name)
	end
	pdj_results["anime_googlenet_finetune"] = read_pdj_result("data/poseest/anime/test/predict/anime_googlenet_finetune/pdj/pdj.txt")
	#pdj_results["mmd_big2_googlenet"] = read_pdj_result("data/poseest/anime/test/predict/mmd_big2_googlenet/pdj/pdj.txt")
	pdj_results["anime_googlenet"] = read_pdj_result("data/poseest/anime/test/predict/anime_googlenet/pdj/pdj.txt")

	Rake::Task["poseest_anime_test_predict_compare_var:graph_clean"].invoke
	Rake::Task["poseest_anime_test_predict_compare_var:graph"].invoke

	target_dir = "/home/pramook/pramook/workspace/animepose-paper/images/var_experiment_pdj/anime"
	run("mkdir -p #{target_dir}")
	run("cp -rf data/poseest/anime/test/predict/compare_var/* #{target_dir}")

	target_file = "/home/pramook/pramook/workspace/animepose-paper/data/var_anime_experiment_pdj.txt"
	File.open(target_file, "w") do |fout|
		paper_names = {
			"anime_mmd_var_1000_googlenet_finetune" => "Synth-1k-Draw",
			"anime_mmd_var_10000_googlenet_finetune" => "Synth-10k-Draw",
			"anime_mmd_var_100000_googlenet_finetune" => "Synth-100k-Draw",
			"anime_googlenet_finetune" => "Synth-1m-Draw",
			#"mmd_var_1000_googlenet" => "Synth-1k",
			#"mmd_var_10000_googlenet" => "Synth-10k",
			#"mmd_var_100000_googlenet" => "Synth-100k",
			#"mmd_big2_googlenet" => "Synth-1m",
			#"anime_googlenet" => "Draw",
		}		
		["anime_mmd_var_1000_googlenet_finetune", 
			"anime_mmd_var_10000_googlenet_finetune", 
			"anime_mmd_var_100000_googlenet_finetune", 
			"anime_googlenet_finetune",
			#"mmd_var_1000_googlenet",
			#"mmd_var_10000_googlenet",
			#"mmd_var_100000_googlenet",
			#"mmd_big2_googlenet",
			#"anime_googlenet",
		].each do |network_name|
			fout.write("\\textsc{#{paper_names[network_name]}} & ")			
			["body", "head", "nose_root", "elbows", "wrists", "knees", "ankles"].each do |part_name|								
				if pdj_results[network_name][part_name].nil?
					fout.write("--- ")
				else
					fout.write("#{sprintf("%.1f", pdj_results[network_name][part_name]*100.0)} ")
				end
				if part_name == "ankles"
					fout.write("\\\\\n")
				else
					fout.write("& ")
				end
			end
		end
	end
end

##########################
# Appearance Experiments #
##########################

appear_tasks = {}

[0,1,2].each do |data_set_index|
	appear_tasks[data_set_index] = {}
	[224].each do |image_size|
		appear_tasks[data_set_index][image_size] = {}

		appear_tasks[data_set_index][image_size][:image] = CaffeImageData.new(
			"poseest_appear_#{data_set_index}_im#{image_size}_image",
			"data/poseest/appear/#{data_set_index}/im#{image_size}",
			:data_list_file => "data/mmd_examples/appear/#{data_set_index}/im#{image_size}/data_list.txt",
			:image_width => image_size,
			:image_height => image_size
		)

		appear_tasks[data_set_index][image_size][:pose] = {}
		
		appear_tasks[data_set_index][image_size][:pose][:mmd_21_weights] = CaffePoseData.new(
			"poseest_appear_#{data_set_index}_im#{image_size}_pose_mmd_21_weights",
			"data/poseest/appear/#{data_set_index}/im#{image_size}/mmd_21_weights",
			:data_list_file => "data/mmd_examples/appear/#{data_set_index}/im#{image_size}/data_list.txt",	
			:pose_2d_config => $p2dc_mmd_21_weights,
			:pose_xform => $p2xf_mmd_21_add_weights,
			:image_width => image_size,
			:image_height => image_size
		)		
	end
end

networks[:appear] = {}
3.times do |index|
	image_size = 224

	network_task = CaffeSimplePoseRegressor.new(
		"poseest_appear_#{index}_googlenet", 
		"data/poseest/appear/#{index}/googlenet", 
		:image_mean_file => appear_tasks[index][image_size][:image].per_channel_image_mean_file_name,

		:train_image_lmdb_file => appear_tasks[index][image_size][:image].image_lmdb_file_name,
		:train_pose_lmdb_file => appear_tasks[index][image_size][:pose][:mmd_21_weights].pose_lmdb_file_name,
		:val_image_lmdb_file => big2_tasks[1][224][:image].image_lmdb_file_name,
		:val_pose_lmdb_file => big2_tasks[1][224][:pose][:mmd_21_weights].pose_lmdb_file_name,

		:base_lr => 0.001,	
		:snapshot => 100000,

		:test_iter => 200,
		:val_batch_size => 10,
		:test_interval => 1000,	

		:train_batch_size => 10,
		:max_iter => 400000,
		:stepsize => 100000,
		:epoch_iter_count => 2000,

		:display => 100,

		:web_service_port => 9292,

		:finetune_from => "data/caffe/bvlc_googlenet.caffemodel",

		:bone_count => $p2dc_mmd_21_weights.bone_names.count,

		:gamma => 0.96,
		:momentum => 0.9,
		:weight_decay => 0.0002,
		:inner_product_layer_std => 0.001,
		:network_type => :googlenet)

	networks[:appear][index] = {
		:name => "appear_#{index}_googlenet",
		:pose_2d_config => $p2dc_mmd_21_weights,
		:tasks => network_task,
		:caffemodel_file => network_task.caffemodel_file_name(1),
		:image_mean_file => appear_tasks[index][image_size][:image].per_channel_image_mean_npy_file_name,
		:image_width => image_size,
		:image_height => image_size,
	}

	finetune_task = CaffeSimplePoseRegressor.new(
		"poseest_anime_appear_#{index}_googlenet_finetune", 
		"data/poseest/anime/appear_#{index}_googlenet_finetune", 
		:image_mean_file => anime_tasks[:train][224][:image].per_channel_image_mean_file_name,
		
		:train_image_lmdb_file => anime_tasks[:train][224][:image].image_lmdb_file_name,
		:train_pose_lmdb_file => anime_tasks[:train][224][:mmd_21_weights].pose_lmdb_file_name,
		:val_image_lmdb_file => anime_tasks[:val][224][:image].image_lmdb_file_name,
		:val_pose_lmdb_file => anime_tasks[:val][224][:mmd_21_weights].pose_lmdb_file_name,

		:base_lr => 0.001,	
		:snapshot => 100000,

		:test_iter => 200,
		:val_batch_size => 10,
		:test_interval => 1000,	

		:train_batch_size => 10,
		:max_iter => 400000,
		:stepsize => 100000,
		:epoch_iter_count => 2000,

		:display => 100,

		:web_service_port => 9292,

		:finetune_from => network_task.caffemodel_file_name(1),

		:bone_count => $p2dc_mmd_21_weights.bone_names.count,

		:gamma => 0.96,
		:momentum => 0.9,
		:weight_decay => 0.0002,
		:inner_product_layer_std => 0.001,
		:network_type => :googlenet
	)

	networks[:anime][("anime_appear_#{index}_googlenet_finetune").to_sym] = {
		:name => "anime_appear_#{index}_googlenet_finetune",
		:pose_2d_config => $p2dc_mmd_21_weights,
		:tasks => finetune_task,
		:caffemodel_file => finetune_task.caffemodel_file_name(1),
		:image_mean_file => anime_tasks[:train][224][:image].per_channel_image_mean_npy_file_name,
		:image_width => 224,
		:image_height => 224,
	}	
end

[2].each do |data_set_index|
	3.times do |network|
		network_name = networks[:appear][network][:name]
		image_size = networks[:appear][network][:image_width]

		big2_tasks[data_set_index][:predict][network_name] = {}

		big2_tasks[data_set_index][:predict][network_name][:predict] = CaffePredictionTasks.new(
			"poseest_mmd_big2_#{data_set_index}_predict_#{network_name}",
			"data/poseest/mmd_big2/#{data_set_index}/predict/#{network_name}",
			:file_list => FileList.new("data/mmd_examples/big2/#{data_set_index}/im#{image_size}/data-*/data_*.png"),
			:pose_2d_config => networks[:appear][network][:pose_2d_config],
			:caffe_deploy_file => networks[:appear][network][:tasks].deploy_file_name,
			:caffemodel_file => networks[:appear][network][:caffemodel_file],
			:image_mean_npy_file => networks[:appear][network][:image_mean_file],
			:image_width => networks[:appear][network][:image_width],
			:image_height => networks[:appear][network][:image_height],
		)

		big2_tasks[data_set_index][:predict][network_name][:pdj] = PDJEvaluation.new(
			"poseest_mmd_big2_#{data_set_index}_predict_#{network_name}_pdj",
			"data/poseest/mmd_big2/#{data_set_index}/predict/#{network_name}/pdj",
			:prediction_file => big2_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => big2_tasks[data_set_index][image_size][:pose][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21
		)		
		
		big2_tasks[data_set_index][:predict][network_name][:pcp] = StrictPCPEvaluation.new(
			"poseest_mmd_big2_#{data_set_index}_predict_#{network_name}_pcp", 
			"data/poseest/mmd_big2/#{data_set_index}/predict/#{network_name}/pcp",
			:prediction_file => big2_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => big2_tasks[data_set_index][image_size][:pose][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21,
			:threshold => 0.5,
		)
	end
end

PDJComparison.new(
	"poseest_mmd_big2_2_predict_compare_appear", 
	"data/poseest/mmd_big2/2/predict/compare_appear",
	:pdj_files => [		
		["Appear-10", "data/poseest/mmd_big2/2/predict/appear_0_googlenet/pdj/pdj.txt"],		
		["Appear-100", "data/poseest/mmd_big2/2/predict/appear_1_googlenet/pdj/pdj.txt"],				
		["Appear-1000", "data/poseest/mmd_big2/2/predict/appear_2_googlenet/pdj/pdj.txt"],		
	],
	:pose_2d_config => $p2dc_mmd_21)

task :poseest_appear_mmd_big2_experiment do
	pcp_results = {}
	pdj_results = {}

	["appear_0_googlenet", "appear_1_googlenet", "appear_2_googlenet"].each do |network_name|
		prediction_task = big2_tasks[2][:predict][network_name][:predict]
		#Rake::Task[prediction_task.name + ":raw_prediction_clean"].invoke
		Rake::Task[prediction_task.name + ":raw_prediction"].invoke
		Rake::Task[prediction_task.name + ":labeling_2d_clean"].invoke
		Rake::Task[prediction_task.name + ":labeling_2d"].invoke
		
		pdj_task = big2_tasks[2][:predict][network_name][:pdj]
		Rake::Task[pdj_task.name + ":pdj_clean"].invoke
		Rake::Task[pdj_task.name + ":pdj"].invoke
		pdj_results[network_name] = read_pdj_result(pdj_task.pdj_file_name)

		pcp_task = big2_tasks[2][:predict][network_name][:pcp]
		Rake::Task[pcp_task.name + ":pcp_clean"].invoke
		Rake::Task[pcp_task.name + ":pcp"].invoke
		pcp_results[network_name] = read_pcp_result(pcp_task.pcp_file_name)
	end

	Rake::Task["poseest_mmd_big2_2_predict_compare_appear:graph_clean"].invoke
	Rake::Task["poseest_mmd_big2_2_predict_compare_appear:graph"].invoke

	target_dir = "/home/pramook/pramook/workspace/animepose-paper/images/appear_experiment_pdj/mmd_big2"
	run("mkdir -p #{target_dir}")
	run("cp -rf data/poseest/mmd_big2/2/predict/compare_appear/* #{target_dir}")

	target_file = "/home/pramook/pramook/workspace/animepose-paper/data/appear_mmd_big2_experiment_pdj.txt"
	File.open(target_file, "w") do |fout|
		paper_names = {			
			"appear_0_googlenet" => "Appear-10",
			"appear_1_googlenet" => "Appear-100",
			"appear_2_googlenet" => "Appear-1k",						
		}		
		["appear_0_googlenet", 
		 "appear_1_googlenet", 
		 "appear_2_googlenet",
		].each do |network_name|
			fout.write("\\textsc{#{paper_names[network_name]}} & ")			
			["body", "head", "nose_root", "elbows", "wrists", "knees", "ankles"].each do |part_name|								
				if pdj_results[network_name][part_name].nil?
					fout.write("--- ")
				else
					fout.write("#{sprintf("%.1f", pdj_results[network_name][part_name]*100.0)} ")
				end
				if part_name == "ankles"
					fout.write("\\\\\n")
				else
					fout.write("& ")
				end
			end
		end
	end

=begin
	run("mkdir -p /home/pramook/pramook/workspace/animepose-paper/data")
	target_file = "/home/pramook/pramook/workspace/animepose-paper/data/anime_experiment_pcp.txt"
	File.open(target_file, "w") do |fout|
		paper_names = {
			"mmd_big2_googlenet" => "Synth",
			"anime_googlenet" => "Draw",
			"anime_googlenet_finetune" => "Synth-Draw",
		}
		["mmd_big2_googlenet", "anime_googlenet", "anime_googlenet_finetune"].each do |network_name|
			fout.write("\\textsc{#{paper_names[network_name]}} & ")
			["torso", "head", "upper_arms", "lower_arms", "upper_legs", "lower_legs"].each do |part_name|
				if pcp_results[network_name][part_name].nil?
					fout.write("--- ")
				else
					fout.write("#{sprintf("%.1f", pcp_results[network_name][part_name]*100.0)} ")
				end
				if part_name == "lower_legs"
					fout.write("\\\\\n")
				else
					fout.write("& ")
				end
			end
		end
	end
=end
end

[:test].each do |data_set_index|	
	[0,1,2].each do |network_index|
		network = networks[:anime][("anime_appear_#{network_index}_googlenet_finetune").to_sym]
		network_name = network[:name]
		image_size = network[:image_width]

		anime_tasks[data_set_index][:predict][network_name] = {}

		file_list = FileList.new("data/poseest/anime/#{data_set_index}/im#{image_size}/source/data-*/data_*.png")		
		anime_tasks[data_set_index][:predict][network_name][:predict] = CaffePredictionTasks.new(
			"poseest_anime_#{data_set_index}_predict_#{network_name}",
			"data/poseest/anime/#{data_set_index}/predict/#{network_name}",			
			:file_list => file_list,
			:pose_2d_config => network[:pose_2d_config],
			:caffe_deploy_file => network[:tasks].deploy_file_name,
			:caffemodel_file => network[:caffemodel_file],
			:image_mean_npy_file => network[:image_mean_file],
			:image_width => network[:image_width],
			:image_height => network[:image_height],
			:original_data_list => "data/poseest/anime/#{data_set_index}/raw/data_list.txt",
			:overlay_pose_2d_config => $p2dc_mmd_21,
		)

		anime_tasks[data_set_index][:predict][network_name][:pdj] = PDJEvaluation.new(
			"poseest_anime_#{data_set_index}_predict_#{network_name}_pdj",
			"data/poseest/anime/#{data_set_index}/predict/#{network_name}/pdj",
			:prediction_file => anime_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => anime_tasks[data_set_index][image_size][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21
		)		
		
		anime_tasks[data_set_index][:predict][network_name][:pcp] = StrictPCPEvaluation.new(
			"poseest_anime_#{data_set_index}_predict_#{network_name}_pcp", 
			"data/poseest/anime/#{data_set_index}/predict/#{network_name}/pcp",
			:prediction_file => anime_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => anime_tasks[data_set_index][image_size][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21,
			:threshold => 0.5,
		)
	end

	[0,1,2].each do |network_index|
		network = networks[:appear][network_index]
		network_name = network[:name]
		image_size = network[:image_width]

		anime_tasks[data_set_index][:predict][network_name] = {}

		file_list = FileList.new("data/poseest/anime/#{data_set_index}/im#{image_size}/source/data-*/data_*.png")		
		anime_tasks[data_set_index][:predict][network_name][:predict] = CaffePredictionTasks.new(
			"poseest_anime_#{data_set_index}_predict_#{network_name}",
			"data/poseest/anime/#{data_set_index}/predict/#{network_name}",
			:file_list => file_list,
			:pose_2d_config => network[:pose_2d_config],
			:caffe_deploy_file => network[:tasks].deploy_file_name,
			:caffemodel_file => network[:caffemodel_file],
			:image_mean_npy_file => network[:image_mean_file],
			:image_width => network[:image_width],
			:image_height => network[:image_height],
		)

		anime_tasks[data_set_index][:predict][network_name][:pdj] = PDJEvaluation.new(
			"poseest_anime_#{data_set_index}_predict_#{network_name}_pdj",
			"data/poseest/anime/#{data_set_index}/predict/#{network_name}/pdj",
			:prediction_file => anime_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => anime_tasks[data_set_index][image_size][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21
		)		
		
		anime_tasks[data_set_index][:predict][network_name][:pcp] = StrictPCPEvaluation.new(
			"poseest_anime_#{data_set_index}_predict_#{network_name}_pcp", 
			"data/poseest/anime/#{data_set_index}/predict/#{network_name}/pcp",
			:prediction_file => anime_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => anime_tasks[data_set_index][image_size][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21,
			:threshold => 0.5,
		)
	end
end

PDJComparison.new(
	"poseest_anime_test_predict_compare_appear", 
	"data/poseest/anime/test/predict/compare_appear",
	:pdj_files => [		
		["Appear-10-Draw", 
			"data/poseest/anime/test/predict/anime_appear_0_googlenet_finetune/pdj/pdj.txt",
			"b"],		
		["Appear-100-Draw", 
			"data/poseest/anime/test/predict/anime_appear_1_googlenet_finetune/pdj/pdj.txt",
			"g"],				
		["Appear-1k-Draw", 
			"data/poseest/anime/test/predict/anime_appear_2_googlenet_finetune/pdj/pdj.txt",
			"r"],		
		["Appear-10", 
			"data/poseest/anime/test/predict/appear_0_googlenet/pdj/pdj.txt",
			"b--"],
		["Appear-100", 
			"data/poseest/anime/test/predict/appear_1_googlenet/pdj/pdj.txt",
			"g--"],
		["Appear-1k", 
			"data/poseest/anime/test/predict/appear_2_googlenet/pdj/pdj.txt",
			"r--"],		
		["Draw",
			"data/poseest/anime/test/predict/anime_googlenet/pdj/pdj.txt",
			"k"]
	],
	:pose_2d_config => $p2dc_mmd_21,
	:show_legends => false)

task :poseest_appear_anime_experiment do
	pcp_results = {}
	pdj_results = {}

	["anime_appear_0_googlenet_finetune", 
	 "anime_appear_1_googlenet_finetune", 
	 "anime_appear_2_googlenet_finetune",
	 "appear_0_googlenet", 
	 "appear_1_googlenet", 
	 "appear_2_googlenet",
	].each do |network_name|
		prediction_task = anime_tasks[:test][:predict][network_name][:predict]
		#Rake::Task[prediction_task.name + ":raw_prediction_clean"].invoke
		Rake::Task[prediction_task.name + ":raw_prediction"].invoke
		Rake::Task[prediction_task.name + ":labeling_2d_clean"].invoke
		Rake::Task[prediction_task.name + ":labeling_2d"].invoke
		
		pdj_task = anime_tasks[:test][:predict][network_name][:pdj]
		Rake::Task[pdj_task.name + ":pdj_clean"].invoke
		Rake::Task[pdj_task.name + ":pdj"].invoke
		pdj_results[network_name] = read_pdj_result(pdj_task.pdj_file_name)

		pcp_task = anime_tasks[:test][:predict][network_name][:pcp]
		Rake::Task[pcp_task.name + ":pcp_clean"].invoke
		Rake::Task[pcp_task.name + ":pcp"].invoke
		pcp_results[network_name] = read_pcp_result(pcp_task.pcp_file_name)
	end		
	pdj_results["anime_googlenet"] = read_pdj_result("data/poseest/anime/test/predict/anime_googlenet/pdj/pdj.txt")

	Rake::Task["poseest_anime_test_predict_compare_appear:graph_clean"].invoke
	Rake::Task["poseest_anime_test_predict_compare_appear:graph"].invoke

	target_dir = "/home/pramook/pramook/workspace/animepose-paper/images/appear_experiment_pdj/anime"
	run("mkdir -p #{target_dir}")
	run("cp -rf data/poseest/anime/test/predict/compare_appear/* #{target_dir}")

	target_file = "/home/pramook/pramook/workspace/animepose-paper/data/appear_anime_experiment_pdj.txt"
	File.open(target_file, "w") do |fout|
		paper_names = {
			"anime_appear_0_googlenet_finetune" => "Appear-10-Draw",
			"anime_appear_1_googlenet_finetune" => "Appear-100-Draw",
			"anime_appear_2_googlenet_finetune" => "Appear-1k-Draw",			
			"appear_0_googlenet" => "Appear-10",
			"appear_1_googlenet" => "Appear-100",
			"appear_2_googlenet" => "Appear-1k",			
			"anime_googlenet" => "Draw",
		}		
		["anime_appear_0_googlenet_finetune", 
		 "anime_appear_1_googlenet_finetune", 
		 "anime_appear_2_googlenet_finetune",
		 "appear_0_googlenet", 
		 "appear_1_googlenet", 
		 "appear_2_googlenet",
		 "anime_googlenet",
		].each do |network_name|
			fout.write("\\textsc{#{paper_names[network_name]}} & ")			
			["body", "head", "nose_root", "elbows", "wrists", "knees", "ankles"].each do |part_name|								
				if pdj_results[network_name][part_name].nil?
					fout.write("--- ")
				else
					fout.write("#{sprintf("%.1f", pdj_results[network_name][part_name]*100.0)} ")
				end
				if part_name == "ankles"
					fout.write("\\\\\n")
				else
					fout.write("& ")
				end
			end
		end
	end
end

##########################
# Pose Experiments #
##########################

pose_tasks = {}

[0,1,2].each do |data_set_index|
	pose_tasks[data_set_index] = {}
	[224].each do |image_size|
		pose_tasks[data_set_index][image_size] = {}

		pose_tasks[data_set_index][image_size][:image] = CaffeImageData.new(
			"poseest_pose_#{data_set_index}_im#{image_size}_image",
			"data/poseest/pose/#{data_set_index}/im#{image_size}",
			:data_list_file => "data/mmd_examples/pose/#{data_set_index}/im#{image_size}/data_list.txt",
			:image_width => image_size,
			:image_height => image_size
		)

		pose_tasks[data_set_index][image_size][:pose] = {}
		
		pose_tasks[data_set_index][image_size][:pose][:mmd_21_weights] = CaffePoseData.new(
			"poseest_pose_#{data_set_index}_im#{image_size}_pose_mmd_21_weights",
			"data/poseest/pose/#{data_set_index}/im#{image_size}/mmd_21_weights",
			:data_list_file => "data/mmd_examples/pose/#{data_set_index}/im#{image_size}/data_list.txt",	
			:pose_2d_config => $p2dc_mmd_21_weights,
			:pose_xform => $p2xf_mmd_21_add_weights,
			:image_width => image_size,
			:image_height => image_size
		)		
	end
end

networks[:pose] = {}
3.times do |index|
	image_size = 224

	network_task = CaffeSimplePoseRegressor.new(
		"poseest_pose_#{index}_googlenet", 
		"data/poseest/pose/#{index}/googlenet", 
		:image_mean_file => pose_tasks[index][image_size][:image].per_channel_image_mean_file_name,

		:train_image_lmdb_file => pose_tasks[index][image_size][:image].image_lmdb_file_name,
		:train_pose_lmdb_file => pose_tasks[index][image_size][:pose][:mmd_21_weights].pose_lmdb_file_name,
		:val_image_lmdb_file => big2_tasks[1][224][:image].image_lmdb_file_name,
		:val_pose_lmdb_file => big2_tasks[1][224][:pose][:mmd_21_weights].pose_lmdb_file_name,

		:base_lr => 0.001,	
		:snapshot => 100000,

		:test_iter => 200,
		:val_batch_size => 10,
		:test_interval => 1000,	

		:train_batch_size => 10,
		:max_iter => 400000,
		:stepsize => 100000,
		:epoch_iter_count => 2000,

		:display => 100,

		:web_service_port => 9292,

		:finetune_from => "data/caffe/bvlc_googlenet.caffemodel",

		:bone_count => $p2dc_mmd_21_weights.bone_names.count,

		:gamma => 0.96,
		:momentum => 0.9,
		:weight_decay => 0.0002,
		:inner_product_layer_std => 0.001,
		:network_type => :googlenet)

	networks[:pose][index] = {
		:name => "pose_#{index}_googlenet",
		:pose_2d_config => $p2dc_mmd_21_weights,
		:tasks => network_task,
		:caffemodel_file => network_task.caffemodel_file_name(1),
		:image_mean_file => pose_tasks[index][image_size][:image].per_channel_image_mean_npy_file_name,
		:image_width => image_size,
		:image_height => image_size,
	}

	finetune_task = CaffeSimplePoseRegressor.new(
		"poseest_anime_pose_#{index}_googlenet_finetune", 
		"data/poseest/anime/pose_#{index}_googlenet_finetune", 
		:image_mean_file => anime_tasks[:train][224][:image].per_channel_image_mean_file_name,
		
		:train_image_lmdb_file => anime_tasks[:train][224][:image].image_lmdb_file_name,
		:train_pose_lmdb_file => anime_tasks[:train][224][:mmd_21_weights].pose_lmdb_file_name,
		:val_image_lmdb_file => anime_tasks[:val][224][:image].image_lmdb_file_name,
		:val_pose_lmdb_file => anime_tasks[:val][224][:mmd_21_weights].pose_lmdb_file_name,

		:base_lr => 0.001,	
		:snapshot => 100000,

		:test_iter => 200,
		:val_batch_size => 10,
		:test_interval => 1000,	

		:train_batch_size => 10,
		:max_iter => 400000,
		:stepsize => 100000,
		:epoch_iter_count => 2000,

		:display => 100,

		:web_service_port => 9292,

		:finetune_from => network_task.caffemodel_file_name(1),

		:bone_count => $p2dc_mmd_21_weights.bone_names.count,

		:gamma => 0.96,
		:momentum => 0.9,
		:weight_decay => 0.0002,
		:inner_product_layer_std => 0.001,
		:network_type => :googlenet
	)

	networks[:anime][("anime_pose_#{index}_googlenet_finetune").to_sym] = {
		:name => "anime_pose_#{index}_googlenet_finetune",
		:pose_2d_config => $p2dc_mmd_21_weights,
		:tasks => finetune_task,
		:caffemodel_file => finetune_task.caffemodel_file_name(3),
		:image_mean_file => anime_tasks[:train][224][:image].per_channel_image_mean_npy_file_name,
		:image_width => 224,
		:image_height => 224,
	}	
end

[2].each do |data_set_index|
	3.times do |network_index|
		network = networks[:pose][network_index]
		network_name = network[:name]
		image_size = network[:image_width]

		big2_tasks[data_set_index][:predict][network_name] = {}

		big2_tasks[data_set_index][:predict][network_name][:predict] = CaffePredictionTasks.new(
			"poseest_mmd_big2_#{data_set_index}_predict_#{network_name}",
			"data/poseest/mmd_big2/#{data_set_index}/predict/#{network_name}",
			:file_list => FileList.new("data/mmd_examples/big2/#{data_set_index}/im#{image_size}/data-*/data_*.png"),
			:pose_2d_config => network[:pose_2d_config],
			:caffe_deploy_file => network[:tasks].deploy_file_name,
			:caffemodel_file => network[:caffemodel_file],
			:image_mean_npy_file => network[:image_mean_file],
			:image_width => network[:image_width],
			:image_height => network[:image_height],
		)

		big2_tasks[data_set_index][:predict][network_name][:pdj] = PDJEvaluation.new(
			"poseest_mmd_big2_#{data_set_index}_predict_#{network_name}_pdj",
			"data/poseest/mmd_big2/#{data_set_index}/predict/#{network_name}/pdj",
			:prediction_file => big2_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => big2_tasks[data_set_index][image_size][:pose][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21
		)		
		
		big2_tasks[data_set_index][:predict][network_name][:pcp] = StrictPCPEvaluation.new(
			"poseest_mmd_big2_#{data_set_index}_predict_#{network_name}_pcp", 
			"data/poseest/mmd_big2/#{data_set_index}/predict/#{network_name}/pcp",
			:prediction_file => big2_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => big2_tasks[data_set_index][image_size][:pose][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21,
			:threshold => 0.5,
		)
	end
end

PDJComparison.new(
	"poseest_mmd_big2_2_predict_compare_pose", 
	"data/poseest/mmd_big2/2/predict/compare_pose",
	:pdj_files => [		
		["Pose-1k", "data/poseest/mmd_big2/2/predict/pose_0_googlenet/pdj/pdj.txt"],		
		["Pose-10k", "data/poseest/mmd_big2/2/predict/pose_1_googlenet/pdj/pdj.txt"],				
		["Pose-100k", "data/poseest/mmd_big2/2/predict/pose_2_googlenet/pdj/pdj.txt"],		
	],
	:pose_2d_config => $p2dc_mmd_21)

task :poseest_pose_mmd_big2_experiment do
	pcp_results = {}
	pdj_results = {}

	["pose_0_googlenet", "pose_1_googlenet", "pose_2_googlenet"].each do |network_name|
		prediction_task = big2_tasks[2][:predict][network_name][:predict]
		#Rake::Task[prediction_task.name + ":raw_prediction_clean"].invoke
		Rake::Task[prediction_task.name + ":raw_prediction"].invoke
		Rake::Task[prediction_task.name + ":labeling_2d_clean"].invoke
		Rake::Task[prediction_task.name + ":labeling_2d"].invoke
		
		pdj_task = big2_tasks[2][:predict][network_name][:pdj]
		Rake::Task[pdj_task.name + ":pdj_clean"].invoke
		Rake::Task[pdj_task.name + ":pdj"].invoke
		pdj_results[network_name] = read_pdj_result(pdj_task.pdj_file_name)

		pcp_task = big2_tasks[2][:predict][network_name][:pcp]
		Rake::Task[pcp_task.name + ":pcp_clean"].invoke
		Rake::Task[pcp_task.name + ":pcp"].invoke
		pcp_results[network_name] = read_pcp_result(pcp_task.pcp_file_name)
	end

	Rake::Task["poseest_mmd_big2_2_predict_compare_pose:graph_clean"].invoke
	Rake::Task["poseest_mmd_big2_2_predict_compare_pose:graph"].invoke

	target_dir = "/home/pramook/pramook/workspace/animepose-paper/images/pose_experiment_pdj/mmd_big2"
	run("mkdir -p #{target_dir}")
	run("cp -rf data/poseest/mmd_big2/2/predict/compare_pose/* #{target_dir}")

	target_file = "/home/pramook/pramook/workspace/animepose-paper/data/pose_mmd_big2_experiment_pdj.txt"
	File.open(target_file, "w") do |fout|
		paper_names = {			
			"pose_0_googlenet" => "Pose-1k",
			"pose_1_googlenet" => "Pose-10k",
			"pose_2_googlenet" => "Pose-100k",						
		}		
		["pose_0_googlenet", 
		 "pose_1_googlenet", 
		 "pose_2_googlenet",
		].each do |network_name|
			fout.write("\\textsc{#{paper_names[network_name]}} & ")			
			["body", "head", "nose_root", "elbows", "wrists", "knees", "ankles"].each do |part_name|
				if pdj_results[network_name][part_name].nil?
					fout.write("--- ")
				else
					fout.write("#{sprintf("%.1f", pdj_results[network_name][part_name]*100.0)} ")
				end
				if part_name == "ankles"
					fout.write("\\\\\n")
				else
					fout.write("& ")
				end
			end
		end
	end

=begin
	run("mkdir -p /home/pramook/pramook/workspace/animepose-paper/data")
	target_file = "/home/pramook/pramook/workspace/animepose-paper/data/anime_experiment_pcp.txt"
	File.open(target_file, "w") do |fout|
		paper_names = {
			"mmd_big2_googlenet" => "Synth",
			"anime_googlenet" => "Draw",
			"anime_googlenet_finetune" => "Synth-Draw",
		}
		["mmd_big2_googlenet", "anime_googlenet", "anime_googlenet_finetune"].each do |network_name|
			fout.write("\\textsc{#{paper_names[network_name]}} & ")
			["torso", "head", "upper_arms", "lower_arms", "upper_legs", "lower_legs"].each do |part_name|
				if pcp_results[network_name][part_name].nil?
					fout.write("--- ")
				else
					fout.write("#{sprintf("%.1f", pcp_results[network_name][part_name]*100.0)} ")
				end
				if part_name == "lower_legs"
					fout.write("\\\\\n")
				else
					fout.write("& ")
				end
			end
		end
	end
=end
end


######################
# Setups Experiments #
######################

begin
	network_task = CaffeSimplePoseRegressor.new(
		"poseest_mmd_big2_googlenet_mmd_21",
		"data/poseest/mmd_big2/googlenet_mmd_21",
		:image_mean_file => $caffe_permuted_mmd_03_0_im224_image.per_channel_image_mean_file_name,

		:train_image_lmdb_file => big2_tasks[0][224][:image].image_lmdb_file_name,
		:train_pose_lmdb_file => big2_tasks[0][224][:pose][:mmd_21].pose_lmdb_file_name,
		:val_image_lmdb_file => big2_tasks[1][224][:image].image_lmdb_file_name,
		:val_pose_lmdb_file => big2_tasks[1][224][:pose][:mmd_21].pose_lmdb_file_name,

		:base_lr => 0.001,	
		:snapshot => 100000,

		:test_iter => 200,
		:val_batch_size => 10,
		:test_interval => 1000,	

		:train_batch_size => 10,
		:max_iter => 400000,
		:stepsize => 100000,
		:epoch_iter_count => 2000,

		:display => 100,

		:web_service_port => 9292,

		:finetune_from => "data/caffe/bvlc_googlenet.caffemodel",

		:bone_count => $p2dc_mmd_21.bone_names.count,

		:gamma => 0.96,
		:momentum => 0.9,
		:weight_decay => 0.0002,
		:inner_product_layer_std => 0.001,
		:network_type => :googlenet)

	networks[:mmd_big2]["mmd_big2_googlenet_mmd_21"] = {
		:name => "mmd_big2_googlenet_mmd_21",
		:pose_2d_config => $p2dc_mmd_21,
		:tasks => network_task,
		:caffemodel_file => network_task.caffemodel_file_name(1),
		:image_mean_file => $caffe_permuted_mmd_03_0_im224_image.per_channel_image_mean_npy_file_name,
		:image_width => 224,
		:image_height => 224,
	}
end

begin
	network_task = CaffeSimplePoseRegressor.new(
		"poseest_mmd_big2_googlenet_mmd_21_midlimbs",
		"data/poseest/mmd_big2/googlenet_mmd_21_midblims",
		:image_mean_file => $caffe_permuted_mmd_03_0_im224_image.per_channel_image_mean_file_name,

		:train_image_lmdb_file => big2_tasks[0][224][:image].image_lmdb_file_name,
		:train_pose_lmdb_file => big2_tasks[0][224][:pose][:mmd_21_midlimbs].pose_lmdb_file_name,
		:val_image_lmdb_file => big2_tasks[1][224][:image].image_lmdb_file_name,
		:val_pose_lmdb_file => big2_tasks[1][224][:pose][:mmd_21_midlimbs].pose_lmdb_file_name,

		:base_lr => 0.001,	
		:snapshot => 100000,

		:test_iter => 200,
		:val_batch_size => 10,
		:test_interval => 1000,	

		:train_batch_size => 10,
		:max_iter => 400000,
		:stepsize => 100000,
		:epoch_iter_count => 2000,

		:display => 100,

		:web_service_port => 9292,

		:finetune_from => "data/caffe/bvlc_googlenet.caffemodel",

		:bone_count => $p2dc_mmd_21_midlimbs.bone_names.count,

		:gamma => 0.96,
		:momentum => 0.9,
		:weight_decay => 0.0002,
		:inner_product_layer_std => 0.001,
		:network_type => :googlenet)

	networks[:mmd_big2]["mmd_big2_googlenet_mmd_21_midlimbs"] = {
		:name => "mmd_big2_googlenet_mmd_21_midlimbs",
		:pose_2d_config => $p2dc_mmd_21_midlimbs,
		:tasks => network_task,
		:caffemodel_file => network_task.caffemodel_file_name(1),
		:image_mean_file => $caffe_permuted_mmd_03_0_im224_image.per_channel_image_mean_npy_file_name,
		:image_width => 224,
		:image_height => 224,
	}
end

[2].each do |data_set_index|
	["mmd_big2_googlenet_mmd_21", "mmd_big2_googlenet_mmd_21_midlimbs"].each do |network_index|
		network = networks[:mmd_big2][network_index]
		network_name = network[:name]
		image_size = network[:image_width]

		big2_tasks[data_set_index][:predict][network_name] = {}

		big2_tasks[data_set_index][:predict][network_name][:predict] = CaffePredictionTasks.new(
			"poseest_mmd_big2_#{data_set_index}_predict_#{network_name}",
			"data/poseest/mmd_big2/#{data_set_index}/predict/#{network_name}",
			:file_list => FileList.new("data/mmd_examples/big2/#{data_set_index}/im#{image_size}/data-*/data_*.png"),
			:pose_2d_config => network[:pose_2d_config],
			:caffe_deploy_file => network[:tasks].deploy_file_name,
			:caffemodel_file => network[:caffemodel_file],
			:image_mean_npy_file => network[:image_mean_file],
			:image_width => network[:image_width],
			:image_height => network[:image_height],
		)

		big2_tasks[data_set_index][:predict][network_name][:pdj] = PDJEvaluation.new(
			"poseest_mmd_big2_#{data_set_index}_predict_#{network_name}_pdj",
			"data/poseest/mmd_big2/#{data_set_index}/predict/#{network_name}/pdj",
			:prediction_file => big2_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => big2_tasks[data_set_index][image_size][:pose][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21
		)		
		
		big2_tasks[data_set_index][:predict][network_name][:pcp] = StrictPCPEvaluation.new(
			"poseest_mmd_big2_#{data_set_index}_predict_#{network_name}_pcp", 
			"data/poseest/mmd_big2/#{data_set_index}/predict/#{network_name}/pcp",
			:prediction_file => big2_tasks[data_set_index][:predict][network_name][:predict].labeling_2d_file_name,
			:answer_file => big2_tasks[data_set_index][image_size][:pose][:mmd_21].labeling_2d_file_name,
			:pose_2d_config => $p2dc_mmd_21,
			:pose_xform => $p2xf_project_mmd_21,
			:threshold => 0.5,
		)
	end
end

PDJComparison.new(
	"poseest_mmd_big2_2_predict_compare_settings", 
	"data/poseest/mmd_big2/2/predict/compare_settings",
	:pdj_files => [		
		["J-21", "data/poseest/mmd_big2/2/predict/mmd_big2_googlenet_mmd_21/pdj/pdj.txt"],		
		["J-29", "data/poseest/mmd_big2/2/predict/mmd_big2_googlenet_mmd_21_midlimbs/pdj/pdj.txt"],				
		["J-29-W (Synth)", "data/poseest/mmd_big2/2/predict/mmd_big2_googlenet/pdj/pdj.txt"],		
	],
	:pose_2d_config => $p2dc_mmd_21)

task :poseest_settings_experiment do
	pcp_results = {}
	pdj_results = {}

	["mmd_big2_googlenet_mmd_21", 
	 "mmd_big2_googlenet_mmd_21_midlimbs"].each do |network_name|
		prediction_task = big2_tasks[2][:predict][network_name][:predict]
		#Rake::Task[prediction_task.name + ":raw_prediction_clean"].invoke
		Rake::Task[prediction_task.name + ":raw_prediction"].invoke
		Rake::Task[prediction_task.name + ":labeling_2d_clean"].invoke
		Rake::Task[prediction_task.name + ":labeling_2d"].invoke
		
		pdj_task = big2_tasks[2][:predict][network_name][:pdj]
		Rake::Task[pdj_task.name + ":pdj_clean"].invoke
		Rake::Task[pdj_task.name + ":pdj"].invoke
		pdj_results[network_name] = read_pdj_result(pdj_task.pdj_file_name)

		pcp_task = big2_tasks[2][:predict][network_name][:pcp]
		Rake::Task[pcp_task.name + ":pcp_clean"].invoke
		Rake::Task[pcp_task.name + ":pcp"].invoke
		pcp_results[network_name] = read_pcp_result(pcp_task.pcp_file_name)
	end
	pdj_results["mmd_big2_googlenet"] = read_pdj_result("data/poseest/mmd_big2/2/predict/mmd_big2_googlenet/pdj/pdj.txt")

	Rake::Task["poseest_mmd_big2_2_predict_compare_settings:graph_clean"].invoke
	Rake::Task["poseest_mmd_big2_2_predict_compare_settings:graph"].invoke

	target_dir = "/home/pramook/pramook/workspace/animepose-paper/images/settings_experiment_pdj"
	run("mkdir -p #{target_dir}")
	run("cp -rf data/poseest/mmd_big2/2/predict/compare_settings/* #{target_dir}")

	target_file = "/home/pramook/pramook/workspace/animepose-paper/data/settings_experiment_pdj.txt"
	File.open(target_file, "w") do |fout|
		paper_names = {
			"mmd_big2_googlenet_mmd_21" => "J-21",
			"mmd_big2_googlenet_mmd_21_midlimbs" => "J-29",
			"mmd_big2_googlenet" => "J-29-W (Synth)",
		}		
		["mmd_big2_googlenet_mmd_21", 
		 "mmd_big2_googlenet_mmd_21_midlimbs", 
		 "mmd_big2_googlenet",
		].each do |network_name|
			fout.write("\\textsc{#{paper_names[network_name]}} & ")			
			["body", "head", "nose_root", "elbows", "wrists", "knees", "ankles"].each do |part_name|
				if pdj_results[network_name][part_name].nil?
					fout.write("--- ")
				else
					fout.write("#{sprintf("%.1f", pdj_results[network_name][part_name]*100.0)} ")
				end
				if part_name == "ankles"
					fout.write("\\\\\n")
				else
					fout.write("& ")
				end
			end
		end
	end

=begin
	run("mkdir -p /home/pramook/pramook/workspace/animepose-paper/data")
	target_file = "/home/pramook/pramook/workspace/animepose-paper/data/anime_experiment_pcp.txt"
	File.open(target_file, "w") do |fout|
		paper_names = {
			"mmd_big2_googlenet" => "Synth",
			"anime_googlenet" => "Draw",
			"anime_googlenet_finetune" => "Synth-Draw",
		}
		["mmd_big2_googlenet", "anime_googlenet", "anime_googlenet_finetune"].each do |network_name|
			fout.write("\\textsc{#{paper_names[network_name]}} & ")
			["torso", "head", "upper_arms", "lower_arms", "upper_legs", "lower_legs"].each do |part_name|
				if pcp_results[network_name][part_name].nil?
					fout.write("--- ")
				else
					fout.write("#{sprintf("%.1f", pcp_results[network_name][part_name]*100.0)} ")
				end
				if part_name == "lower_legs"
					fout.write("\\\\\n")
				else
					fout.write("& ")
				end
			end
		end
	end
=end
end