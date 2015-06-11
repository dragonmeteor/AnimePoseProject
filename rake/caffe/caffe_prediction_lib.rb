require 'rubygems'
require 'bundler/setup'
require 'erb'
require 'json'
require File.expand_path(File.dirname(__FILE__) + "/../lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../java_lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../pose_est/pose_2d_configs.rb")
require File.expand_path(File.dirname(__FILE__) + "/../args.rb")

class CaffePredictionTasks < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:file_list => nil,			
			:pose_2d_config => nil,
			:caffe_deploy_file => nil,
			:caffemodel_file => nil,
			:image_mean_npy_file => nil,
			:image_width => 227,
			:image_height => 227,
			:original_data_list => nil,
			:point_width => 7,
			:line_width => 3,
			:overlay_pose_2d_config => nil,
		}.merge(_options)

		super(_name, _dir_name, _options)
	end

	def gen_tasks
		file_list_tasks
		raw_prediction_tasks
		labeling_2d_tasks
		view_labeling_2d_tasks
		original_overlay_tasks
	end

	no_index_file_tasks(:file_list, Proc.new {"#{dir_name}/file_list.txt"}) do
		file file_list_file_name do
			File.open(file_list_file_name, "w") do |f|
				f.write(JSON.pretty_generate(options[:file_list].to_a))
			end
		end
	end

	no_index_file_tasks(:raw_prediction, Proc.new {"#{dir_name}/raw_prediction.txt"}) do
		file raw_prediction_file_name => [file_list_file_name, options[:caffe_deploy_file]] do
			args = {
				"deploy_file" => options[:caffe_deploy_file],
				"caffemodel_file" => options[:caffemodel_file],
				"mean_file" => options[:image_mean_npy_file],
				"image_width" => options[:image_width],
				"image_height" => options[:image_height],
				"file_list" => file_list_file_name,
				"output_file" => raw_prediction_file_name
			}
			run("python script/caffe/pose_estimate_file_list.py #{args_file(args)}")
		end
	end

	no_index_file_tasks(:labeling_2d, Proc.new {"#{dir_name}/labeling_2d.txt"}) do
		file labeling_2d_file_name => [raw_prediction_file_name] do			
			predictions = JSON.parse(File.read(raw_prediction_file_name))			
			output = []
			predictions.count.times do |i|
				prediction = predictions[i]
				item = {
					"file_name" => options[:file_list][i],
					"points" => {}
				}				
				output << item
				options[:pose_2d_config].bone_names.count.times do |j|									
					x = (prediction[2*j]+0.5) * options[:image_width]
					y = (prediction[2*j+1]+0.5) * options[:image_height]
					item["points"][options[:pose_2d_config].bone_names[j]] = [x,y]					
				end
			end
			File.open(labeling_2d_file_name, "w") do |fout|
				fout.write(JSON.pretty_generate(output))
			end
		end
	end

	def view_labeling_2d_tasks
		namespace name do
			task :view_labeling_2d => [labeling_2d_file_name] do
				call_java("yumyai.poseest.label2d.ViewPoseLabeling2D", args_file(
					"labeling_2d_file" => labeling_2d_file_name,
					"pose_2d_config_file" => options[:pose_2d_config].config_file_name
				))
			end
		end
	end

	no_index_file_tasks(:original_overlay, Proc.new {"#{dir_name}/original_overlay/done.txt"}) do
		file original_overlay_file_name => [labeling_2d_file_name] do
			FileUtils.mkdir_p("#{dir_name}/original_overlay")
			args = {
				"labeling_2d_file" => labeling_2d_file_name,
				"original_data_list_file" => options[:original_data_list],
				"pose_2d_config_file" => options[:overlay_pose_2d_config].config_file_name,
				"resized_image_width" => options[:image_width],
				"resized_image_height" => options[:image_height],
				"output_file_printf" => "#{dir_name}/original_overlay/data_%08d.png",
				"line_width" => options[:line_width],
				"point_width" => options[:point_width]
			}
			call_java("yumyai.poseest.label2d.RenderPoseLabeling2DToOriginalImage", args_file(args))
			run("touch #{original_overlay_file_name}")
		end
	end
end