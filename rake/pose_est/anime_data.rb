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

Manual2DLabelingTasks.new("full_body_solo_labeling", 
	"data/anime_images/full_body_solo",
	:source_dir => "data/anime_images/full_body_solo",
	:pose_2d_config => $p2dc_mmd_full)

Manual2DLabelingTasks.new("full_body_solo_800x600_labeling", 
	"data/anime_images/full_body_solo_800x600",
	:source_dir => "data/anime_images/full_body_solo_800x600",
	:can_label => false,
	:pose_2d_config => $p2dc_mmd_21)

def split_full_body_solo_800x600
	data = JSON.parse(File.read("data/anime_images/full_body_solo_800x600/labeling_2d.txt"))
	data = data.shuffle
	train = data[0..1399]
	val = data[1400..1499]
	test = data[1500..1999]

	FileUtils.mkdir_p("data/poseest/anime/train/raw")
	File.open("data/poseest/anime/train/raw/labeling_2d.txt", "w") do |fout|
		fout.write(JSON.pretty_generate(train))
	end

	FileUtils.mkdir_p("data/poseest/anime/val/raw")
	File.open("data/poseest/anime/val/raw/labeling_2d.txt", "w") do |fout|
		fout.write(JSON.pretty_generate(val))
	end

	FileUtils.mkdir_p("data/poseest/anime/test/raw")
	File.open("data/poseest/anime/test/raw/labeling_2d.txt", "w") do |fout|
		fout.write(JSON.pretty_generate(test))
	end
end

["train", "val", "test"].each do |data_name|
	file "data/poseest/anime/#{data_name}/raw/labeling_2d.txt" do
		split_full_body_solo_800x600
	end 

	Manual2DLabelingTasks.new("poseest_anime_#{data_name}_raw", 
		"data/poseest/anime/#{data_name}/raw",
		:source_dir => "data/poseest/anime/#{data_name}/raw",
		:can_label => false,
		:pose_2d_config => $p2dc_mmd_21)	
end

task :split_full_body_solo_800x600 => [
	"data/poseest/anime/train/raw/labeling_2d.txt",
	"data/poseest/anime/val/raw/labeling_2d.txt",
	"data/poseest/anime/test/raw/labeling_2d.txt",
]

class Labeling2DToDataTasks < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:labeling_2d_file => "",
			:limit => 1000,
			:overlay_line_width => 3,
			:overlay_point_width => 10,
			:pose_2d_config => nil,
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def gen_tasks
		data_tasks
		data_list_tasks
		data_count_tasks
		pose_overlay_tasks
	end

	def convert
		labeling_2d = JSON.parse(File.read(options[:labeling_2d_file]))

		FileUtils.mkdir_p("#{dir_name}/data")

		data_list = []
		count = 0
		labeling_2d.each do |item|
			image_file = "#{dir_name}/data/data_#{sprintf("%08d",count)}.png"
			pose_file = "#{dir_name}/data/data_#{sprintf("%08d", count)}_data.json"

			File.open(pose_file, "w") do |fout|
				fout.write(JSON.pretty_generate({"points_2d" => item["points"]}))
			end

			run("convert #{item["file_name"]} #{image_file}")

			data_list << [image_file, pose_file]

			count += 1			
		end

		File.open(data_list_file_name, "w") do |fout|
			fout.write(JSON.pretty_generate(data_list))
		end

		File.open(data_count_file_name, "w") do |fout|
			fout.write(JSON.pretty_generate({"count" => labeling_2d.count}))
		end

		touch(data_file_name)
	end

	no_index_file_tasks(:data, Proc.new {"#{dir_name}/data_done.txt"}) do
		file data_file_name => [options[:labeling_2d_file]] do
			convert
		end
	end

	no_index_file_tasks(:data_list, Proc.new {"#{dir_name}/data_list.txt"}) do
		file data_list_file_name => [options[:labeling_2d_file]] do
			convert
		end
	end

	no_index_file_tasks(:data_count, Proc.new {"#{dir_name}/data_count.txt"}) do
		file data_count_file_name => [options[:labeling_2d_file]] do
			convert
		end
	end

	no_index_file_tasks(:pose_overlay, Proc.new {"#{dir_name}/pose_overlay/done.txt"}) do
		file pose_overlay_file_name => [options[:labeling_2d_file], options[:pose_2d_config].config_file_name] do
			FileUtils.mkdir_p("#{dir_name}/pose_overlay")
			call_java("yumyai.poseest.label2d.RenderPoseLabeling2D", args_file(
					"labeling_2d_file" => options[:labeling_2d_file],
					"pose_2d_config_file" => options[:pose_2d_config].config_file_name,
					"output_file_printf" => "#{dir_name}/pose_overlay/%08d.png",
					"line_width" => options[:overlay_line_width],
					"point_width" => options[:overlay_point_width],
			))
			run("touch #{pose_overlay_file_name}")
		end
	end
end

["train", "val", "test"].each do |data_name|
	Labeling2DToDataTasks.new("poseest_anime_#{data_name}_raw_convert",
		"data/poseest/anime/#{data_name}/raw",
		:labeling_2d_file => "data/poseest/anime/#{data_name}/raw/labeling_2d.txt",
		:pose_2d_config => $p2dc_mmd_21)
end