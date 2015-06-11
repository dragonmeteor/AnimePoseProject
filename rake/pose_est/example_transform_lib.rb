require 'rubygems'
require 'bundler/setup'
require 'fileutils'
require 'json'
require File.expand_path(File.dirname(__FILE__) + "/../lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../java_lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/mmd_character_pool_lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/mmd_motion_pool_lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/mmd_background_pool_lib.rb")
require File.expand_path(File.dirname(__FILE__) + "/mmd_negative_example_pool_lib.rb")
require File.expand_path(File.dirname(__FILE__) + "/pose_2d_configs.rb")
require File.expand_path(File.dirname(__FILE__) + "/../args.rb")

class TransformExampleTasks < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:source_data_list => "",
			:source_data_count => "",
			:limit => 1000,			
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def source_data_count
		if @source_data_count.nil?
			@source_data_count = JSON.parse(File.read(options[:source_data_count]))["count"]
		end
		@source_data_count
	end

	def gen_tasks
		data_tasks
		data_list_tasks
		data_count_tasks
	end

	def data_dir_name(index)
		dir_index = index/options[:limit]		
		"#{dir_name}/data-#{sprintf("%05d", dir_index)}"
	end

	def image_file_name(index)		
		"#{data_dir_name(index)}/data_#{sprintf("%08d", index)}.png"
	end

	def pose_file_name(index)
		"#{data_dir_name(index)}/data_#{sprintf("%08d", index)}_data.txt"
	end

	def transform_data
	end


	no_index_file_tasks(:data, Proc.new {"#{dir_name}/data_done.txt"}) do
		file data_file_name => [options[:source_data_list], options[:source_data_count]] do
			transform_data			
			run("touch #{data_file_name}")
		end
	end

	def compute_data_list
		output = []
		skipped = []
		invalid_count = 0				
		source_data_count.times do |index|
			begin				
				image_file = image_file_name(index)
				pose_file = pose_file_name(index)					
				content = File.read(pose_file)						
				pose = JSON.parse(File.read(pose_file))								
				if (index+1) % 100 == 0
					puts "Processed #{index+1} files ..."
				end
				valid = true
				points = pose["points_2d"]					
				points.each do |bone_name, point|					
					if !point.nil? && (point[0].to_f.nan? || point[1].to_f.nan?)
						valid = false
					end					
				end				
				if valid
					output << [image_file, pose_file]
				else 
					invalid_count += 1
					skipped << index
					puts "NaN found when processing \"#{pose_file}\""
				end
			rescue Exception => e
				puts "Error when processing \"#{pose_file}\""
				puts e
				skipped << index
				invalid_count += 1
			end
		end
		puts "There are #{invalid_count} invalid files."
		File.open(data_list_file_name, "w") do |fout|
			fout.write(JSON.pretty_generate(output))
		end
		File.open(data_count_file_name, "w") do |fout|
			fout.write(JSON.pretty_generate({ "count" =>  output.count }))
		end
		puts skipped
	end

	no_index_file_tasks(:data_list, Proc.new { "#{dir_name}/data_list.txt"}) do
		file data_list_file_name => [data_file_name] do
			compute_data_list
		end
	end

	no_index_file_tasks(:data_count, Proc.new { "#{dir_name}/data_count.txt"}) do
		file data_count_file_name => [data_file_name] do
			compute_data_list
		end
	end
end

class CropExampleTasks < TransformExampleTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:source_data_list => "",
			:source_data_count => "",
			:limit => 1000,
			:source_width => 227,
			:source_height => 227,
			:target_width => 224,
			:target_height => 224,
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def transform_data
		args = {
			"data_list" => options[:source_data_list],
			"data_count" => source_data_count,
			"index_start" => 0,
			"index_end" => source_data_count,
			"target_width" => options[:target_width],
			"target_height" => options[:target_height],
			"limit" => options[:limit],
			"dir_printf" => "#{dir_name}/data-%05d",
			"image_file_printf" => "data_%08d.png",
			"pose_file_printf" => "data_%08d_data.txt",
		}
		args_file_name = args_file(args)
		call_java("yumyai.poseest.label2d.CropImageAndLabeling2D", args_file_name)
	end
end

class ResizeExampleTasks < TransformExampleTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:source_data_list => "",
			:source_data_count => "",
			:limit => 1000,			
			:target_width => 227,
			:target_height => 227,
			:background_mode => "border",
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def transform_data
		args = {
			"data_list" => options[:source_data_list],
			"data_count" => source_data_count,
			"index_start" => 0,
			"index_end" => source_data_count,
			"target_width" => options[:target_width],
			"target_height" => options[:target_height],
			"limit" => options[:limit],
			"dir_printf" => "#{dir_name}/data-%05d",
			"image_file_printf" => "data_%08d.png",
			"pose_file_printf" => "data_%08d_data.txt",
			"background_mode" => options[:background_mode]
		}
		args_file_name = args_file(args)
		call_java("yumyai.poseest.label2d.ResizeImageAndLabeling2D", args_file_name)
	end
end

class JointBasedResizeExampleTasks < TransformExampleTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:source_data_list => "",
			:source_data_count => "",
			:limit => 1000,			
			:target_width => 227,
			:target_height => 227,			
			:scale_factor => 1.3,
			:background_mode => "border",
			:eye_to_head => 0,
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def transform_data
		args = {
			"data_list" => options[:source_data_list],
			"data_count" => source_data_count,
			"index_start" => 0,
			"index_end" => source_data_count,
			"target_width" => options[:target_width],
			"target_height" => options[:target_height],
			"limit" => options[:limit],
			"dir_printf" => "#{dir_name}/data-%05d",
			"image_file_printf" => "data_%08d.png",
			"pose_file_printf" => "data_%08d_data.txt",
			"scale_factor" => options[:scale_factor],
			"background_mode" => options[:background_mode],
			"eye_to_head" => options[:eye_to_head],
		}
		args_file_name = args_file(args)
		call_java("yumyai.poseest.label2d.JointBasedResizeImageAndLabeling2D", 
			args_file_name)
	end
end

class RotateScaleTranslateTransformConfig < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})
		_options = {	
			:source_data_list => "",
			:source_data_count => "",
			:count => 2000,
			:limit => 1000,			
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def_index :config do
		(options[:count] / options[:limit]) + (if options[:count] % options[:limit] == 0 then 0 else 1 end)
	end

	def source_data_count
		if @source_data_count.nil?						
			@source_data_count = JSON.parse(File.read(options[:source_data_count]))["count"]
		end
		@source_data_count
	end

	def gen_tasks
		config_tasks
	end

	def generate_configs
		data_list = JSON.parse(File.read(options[:source_data_list]))

		source_count = []
		source_data_count.times do |i|
			source_count << options[:count] / data_list.count
		end
		remaining = options[:count] - (options[:count] / data_list.count)*data_list.count

		random = Random.new
		while remaining > 0
			index = random.rand(source_data_count)
			source_count[index] += 1
			remaining -= 1
		end		

		configs = []
		current_source = 0
		options[:count].times do |i|			
			while current_source < source_data_count && source_count[current_source] == 0 
				current_source += 1
			end			

			item = {
				"image_file" => data_list[current_source][0],
				"pose_file" => data_list[current_source][1],
				"x_shift" => random.rand,
				"y_shift" => random.rand,
				"rotation" => random.rand,
				"scale" => random.rand,
			}
			configs << item

			source_count[current_source] -= 1

			if (i+1) % 100 == 0
				puts "Generated #{i+1} files ..."
			end
		end

		config_count.times do |i|
			config_now = configs[(i*options[:limit])..((i+1)*options[:limit]-1)]			
			File.open(config_file_name(i), "w") do |fout|
				fout.write(JSON.pretty_generate(config_now))
			end

			if (i+1) % 10 == 0
				puts "Written #{i+1} files ..."
			end
		end
	end

	one_index_file_tasks(:config, :config, Proc.new { |config_index|
		"#{dir_name}/config_#{sprintf("%05d", config_index)}.txt"
	}) do |config_index|
		config_file = config_file_name(config_index)
		file config_file do #=> [options[:source_data_list], options[:source_data_count]] do
			generate_configs
		end
	end
end

class RotateScaleTranslateTransformExample < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:config_tasks => nil,
			:scale_factor => 1.2,
			:image_width => 227,
			:image_height => 227,
			:rotate_min => -45,
			:rotate_max => 45,
			:scale_min => 1.3,
			:scale_max => 1.5,
			:eye_to_head => 0.1,
			:check_pose => true,
			:background_mode => "border",
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def_index :config do
		options[:config_tasks].config_count
	end

	def gen_tasks
		data_tasks
		data_list_tasks
		data_count_tasks
	end

	def data_dir_name(index)
		dir_index = index/options[:config_tasks].options[:limit]
		"#{dir_name}/data-#{sprintf("%05d", dir_index)}"
	end

	def image_file_name(index)		
		"#{data_dir_name(index)}/data_#{sprintf("%08d", index)}.png"
	end

	def pose_file_name(index)
		"#{data_dir_name(index)}/data_#{sprintf("%08d", index)}_data.json"
	end	

	def settings_file_name
		"#{dir_name}/settings.txt"
	end

	def write_settings_file
		File.open(settings_file_name, "w") do |fout|
			text = JSON.pretty_generate({
				"image_width" => options[:image_width],
				"image_height" => options[:image_height],
				"scale_factor" => options[:scale_factor],
				"rotate_min" => options[:rotate_min],
				"rotate_max" => options[:rotate_max],
				"scale_min" => options[:scale_min],
				"scale_max" => options[:scale_max],
				"background_mode" => options[:background_mode],
				"eye_to_head" => options[:eye_to_head],
			})
			fout.write(text)
		end
	end	

	one_index_file_tasks(:data, :config, Proc.new { |config_index|
		data_index = config_index * options[:config_tasks].options[:limit]
		"#{data_dir_name(data_index)}/done.txt"
	}) do |config_index|
		data_file = data_file_name(config_index)
		file data_file => [options[:config_tasks].config_file_name(config_index)] do
			write_settings_file
			args = {
				"config_file" => options[:config_tasks].config_file_name(config_index),
				"settings_file" => settings_file_name,
				"index_start" => config_index * options[:config_tasks].options[:limit],
				"image_file_printf" => "#{dir_name}/data-#{sprintf("%05d", config_index)}/data_%08d.png",
				"pose_file_printf" => "#{dir_name}/data-#{sprintf("%05d", config_index)}/data_%08d_data.json"
			}
			call_java(
				"yumyai.poseest.label2d.RotateScaleTranslateImageAndScaleLabeling2D",
				args_file(args))
			run("touch #{data_file}")
		end
	end

	def source_data_count
		options[:config_tasks].options[:count]
	end

	def compute_data_list
		output = []
		skipped = []
		invalid_count = 0
		source_data_count.times do |index|
			image_file = image_file_name(index)
			pose_file = pose_file_name(index)
			if options[:check_pose]
				begin				
					
					content = File.read(pose_file)						
					pose = JSON.parse(File.read(pose_file))								
					if (index+1) % 100 == 0
						puts "Processed #{index+1} files ..."
					end
					valid = true
					points = pose["points_2d"]					
					points.each do |bone_name, point|					
						if !point.nil? && (point[0].to_f.nan? || point[1].to_f.nan?)
							valid = false
						end					
					end				
					if valid
						output << [image_file, pose_file]
					else 
						invalid_count += 1
						skipped << index
						puts "NaN found when processing \"#{pose_file}\""
					end
				rescue Exception => e
					puts "Error when processing \"#{pose_file}\""
					puts e
					skipped << index
					invalid_count += 1
				end
			else
				output << [image_file, pose_file]
			end
		end
		puts "There are #{invalid_count} invalid files."
		File.open(data_list_file_name, "w") do |fout|
			fout.write(JSON.pretty_generate(output))
		end
		File.open(data_count_file_name, "w") do |fout|
			fout.write(JSON.pretty_generate({ "count" =>  output.count }))
		end
		puts skipped
	end

	no_index_file_tasks(:data_list, Proc.new { "#{dir_name}/data_list.txt"}) do
		file data_list_file_name => data_file_list do
			compute_data_list
		end
	end

	no_index_file_tasks(:data_count, Proc.new { "#{dir_name}/data_count.txt"}) do
		file data_count_file_name => data_file_list do
			compute_data_list
		end
	end
end