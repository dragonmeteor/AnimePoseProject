require 'rubygems'
require 'bundler/setup'
require 'erb'
require 'json'
require File.expand_path(File.dirname(__FILE__) + "/../lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../java_lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../pose_est/pose_2d_configs.rb")
require File.expand_path(File.dirname(__FILE__) + "/../args.rb")

class CaffePoseData < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:data_list_file => nil,
			:pose_2d_config => nil,
			:pose_xform => nil,
			:bone_weights => nil,			
			:image_width => 227,
			:image_height => 227,
			:limit => 10000,
			:random_seed => 47841321987,
			:caffe_root => "/opt/caffe",
			:check_image => true,
			:overlay_line_width => 3,
			:overlay_point_width => 10,
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def gen_tasks
		pose_lmdb_tasks
		weight_lmdb_tasks
		lmdb_tasks
		labeling_2d_tasks
		view_labeling_2d_tasks
		labeling_2d_python_tasks
		view_labeling_2d_python_tasks
		loss_of_average_pose_tasks
		pose_overlay_tasks
		file_order_tasks
	end

	def create_pose_and_weight_lmdb
		if !File.exists?(pose_lmdb_file_name) || !File.exists?(weight_lmdb_file_name)
			args = {
				"data_list_file" => options[:data_list_file],
				"pose_2d_config_file" => options[:pose_2d_config].config_file_name,
				"pose_xform_file" => options[:pose_xform].config_file_name,
				"image_width" => options[:image_width],					
				"image_height" => options[:image_height],					
				"pose_lmdb_file" => pose_lmdb_file_name,
				"weight_lmdb_file" => weight_lmdb_file_name,
				"caffe_root" => options[:caffe_root],
				"check_image" => options[:check_image],
				"bone_weights" => options[:bone_weights],				
			}			
			if !options[:random_seed].nil?
				args["random_seed"] = options[:random_seed]
			end
			args_file_name = args_file(args)
			run("python script/caffe/data_list_to_pose_lmdb.py #{args_file_name}")
		end
	end

	no_index_file_tasks(:pose_lmdb, Proc.new {"#{dir_name}/pose.lmdb"}) do
		file pose_lmdb_file_name => [options[:data_list_file], 
			options[:pose_2d_config].config_file_name,
			options[:pose_xform].config_file_name] do
			create_pose_and_weight_lmdb	
		end
	end	

	no_index_file_tasks(:weight_lmdb, Proc.new {"#{dir_name}/weight.lmdb"}) do
		file weight_lmdb_file_name => [options[:data_list_file],
			options[:pose_2d_config].config_file_name,
			options[:pose_xform].config_file_name] do
			create_pose_and_weight_lmdb
		end
	end

	def file_order_tasks
		namespace name do
			task :file_order do				
				args = {
					"data_list_file" => options[:data_list_file],					
				}			
				if !options[:random_seed].nil?
					args["random_seed"] = options[:random_seed]
				end
				args_file_name = args_file(args)
				run("python script/caffe/print_file_order.py #{args_file_name}")
			end
		end
	end

	def lmdb_tasks
		namespace name do
			task :lmdb => [:pose_lmdb, :weight_lmdb]
			task :lmdb_clean => [:pose_lmdb_clean, :weight_lmdb_clean]
		end
	end

	no_index_file_tasks(:labeling_2d, Proc.new {"#{dir_name}/labeling_2d.txt"}) do
		file labeling_2d_file_name => [options[:data_list_file]] do
			data_list = read_json_file(options[:data_list_file])			
			pose_xform = options[:pose_xform]

			output = []
			count = 0
			data_list.each do |item|				
				in_pose = read_json_file(item[1])
				labeling = {
					"file_name" => item[0],
					"points" => pose_xform.transform(in_pose["points_2d"])
				}
				output << labeling
				count += 1
				if count == options[:limit]
					break
				end				
			end

			File.open(labeling_2d_file_name, "w") do |fout|
				fout.write(JSON.pretty_generate(output))
			end
		end
	end

	def view_labeling_2d_tasks
		namespace name do
			task :view_labeling_2d => [labeling_2d_file_name, options[:pose_2d_config].config_file_name] do
				call_java("yumyai.poseest.label2d.ViewPoseLabeling2D", args_file(
					"labeling_2d_file" => labeling_2d_file_name,
					"pose_2d_config_file" => options[:pose_2d_config].config_file_name
				))				
			end
		end
	end

	no_index_file_tasks(:labeling_2d_python, Proc.new {"#{dir_name}/labeling_2d_python.txt"}) do
		file labeling_2d_python_file_name => [options[:data_list_file],
				options[:pose_2d_config].config_file_name,
				options[:pose_xform].config_file_name] do
			args = {
				"data_list_file" => options[:data_list_file],
				"pose_2d_config_file" => options[:pose_2d_config].config_file_name,
				"pose_xform_file" => options[:pose_xform].config_file_name,
				"output_file" => labeling_2d_python_file_name,
				"limit" => options[:limit],
			}
			args_file_name = args_file(args)
			run("python script/caffe/create_labeling_2d_file.py #{args_file_name}")
		end
	end

	def view_labeling_2d_python_tasks
		namespace name do
			task :view_labeling_2d_python => [labeling_2d_python_file_name, options[:pose_2d_config].config_file_name] do
				call_java("yumyai.poseest.label2d.ViewPoseLabeling2D", args_file(
					"labeling_2d_file" => labeling_2d_python_file_name,
					"pose_2d_config_file" => options[:pose_2d_config].config_file_name
				))				
			end
		end
	end

	no_index_file_tasks(:loss_of_average_pose, Proc.new {"#{dir_name}/loss_of_average_pose.txt"}) do
		file loss_of_average_pose_file_name => [options[:data_list_file]] do
			data_list = JSON.load(File.new(options[:data_list_file]))
			
			average_pose = {}
			options[:pose_2d_config].bone_names.each do |bone_name|
				average_pose[bone_name] = [0,0]
			end
			count = 0
			data_list.each do |item|
				pose_file = item[1]
				pose = JSON.load(File.new(pose_file))["points_2d"]
				pose = options[:pose_xform].transform(pose)
				options[:pose_2d_config].bone_names.each do |bone_name|
					average_pose[bone_name][0] += pose[bone_name][0]
					average_pose[bone_name][1] += pose[bone_name][1]
				end
				count += 1
				if count % 100 == 0
					puts "Processed #{count} files ..."
				end
			end
			options[:pose_2d_config].bone_names.each do |bone_name|
				average_pose[bone_name][0] /= data_list.count
				average_pose[bone_name][1] /= data_list.count
			end

			loss = 0
			count = 0
			data_list.each do |item|
				pose_file = item[1]
				pose = JSON.load(File.new(pose_file))["points_2d"]
				pose = options[:pose_xform].transform(pose)
				options[:pose_2d_config].bone_names.each do |bone_name|
					dx = (average_pose[bone_name][0] - pose[bone_name][0]) / options[:image_width]
					dy = (average_pose[bone_name][1] - pose[bone_name][1]) / options[:image_height]
					loss += (dx*dx + dy*dy)/2
				end
				count += 1
				if count % 100 == 0
					puts "Processed #{count} files ..."
				end
			end
			loss /= data_list.count
			File.open(loss_of_average_pose_file_name, "w") do |fout|
				fout.write("#{loss}\n")
			end
		end
	end

	no_index_file_tasks(:pose_overlay, Proc.new {"#{dir_name}/pose_overlay/done.txt"}) do
		file pose_overlay_file_name => [labeling_2d_file_name, options[:pose_2d_config].config_file_name] do
			FileUtils.mkdir_p("#{dir_name}/pose_overlay")
			call_java("yumyai.poseest.label2d.RenderPoseLabeling2D", args_file(
				"labeling_2d_file" => labeling_2d_file_name,
				"pose_2d_config_file" => options[:pose_2d_config].config_file_name,
				"output_file_printf" => "#{dir_name}/pose_overlay/%08d.png",
				"line_width" => options[:overlay_line_width],
				"point_width" => options[:overlay_point_width],
			))
			run("touch #{pose_overlay_file_name}")
		end
	end
end