require 'rubygems'
require 'bundler/setup'
require 'fileutils'
require 'json'
require File.expand_path(File.dirname(__FILE__) + "/../lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../java_lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../args.rb") 

class Manual2DLabelingTasks < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:source_dir => nil,
			:can_label => true,
			:pose_2d_config => nil,			
		}.merge(_options)
		super(_name, _dir_name, _options)		
	end

	def gen_tasks		
		image_list_tasks
		labeling_2d_tasks
		label_tasks
		view_labeling_2d_tasks
	end

	no_index_file_tasks(:image_list, Proc.new {"#{dir_name}/image_list.txt"}) do
		file image_list_file_name do
			file_list = FileList.new(
				"#{options[:source_dir]}/*.png", 
				"#{options[:source_dir]}/*.jpg",
				#"#{options[:source_dir]}/**/*.png",
				#"#{options[:source_dir]}/**/*.jpg",
			).to_a
			puts file_list.count
			File.open(image_list_file_name, "w", :encoding => "utf-8") do |f|
				f.write(JSON.pretty_generate(file_list))				
			end
		end
	end

	no_index_file_tasks(:labeling_2d, Proc.new {"#{dir_name}/labeling_2d.txt"}) do
		file labeling_2d_file_name => [image_list_file_name] do
			call_java("yumyai.poseest.label2d.WriteInitialLabelingFile", args_file({
				"image_list_file" => image_list_file_name,
				"output_file" => labeling_2d_file_name
			}))
		end
	end

	def label_tasks
		namespace name do
			task :label => [labeling_2d_file_name] do
				call_java("yumyai.poseest.label2d.LabelPose2D", labeling_2d_file_name)
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
end