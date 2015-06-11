require 'rubygems'
require 'bundler/setup'
require 'fileutils'
require 'json'
require File.expand_path(File.dirname(__FILE__) + "/../lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../java_lib.rb")

class MmdMotionPool < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:source_dir => nil,
			:default_model => "data/pmd/hzeo_kanekiBW/hzeo_kanekiB.pmx"
		}.merge(_options)
		super(_name, _dir_name, _options)		
	end

	def gen_tasks
		raw_motion_list_tasks
		usable_motion_list_tasks
		unique_frames_tasks
		images_tasks
		animated_tasks
		raw_motion_and_images_js_tasks
		raw_motion_and_images_json_tasks
		mark_motions_tasks
		used_motions_frames_tasks
		used_motions_frames_and_bones_tasks
		motion_info_tasks
		motion_count_tasks
	end

	no_index_file_tasks(:raw_motion_list, Proc.new { "#{dir_name}/raw_motion_list.txt" }) do
		file raw_motion_list_file_name do
			file_list = FileList.new("#{options[:source_dir]}/**/*.vmd", "#{options[:source_dir]}/**/*.vpd")
			puts file_list.count
			File.open(raw_motion_list_file_name, "w", :encoding => "utf-8") do |f|
				file_list.each do |filename|
					f.write(filename + "\n")
				end
			end
		end		
	end

	no_index_file_tasks(:usable_motion_list, Proc.new {"#{dir_name}/usable_motion_list.txt"}) do
		file usable_motion_list_file_name => [raw_motion_list_file_name] do
			call_java("yumyai.poseest.mmd.motion.FilterUsableVmdVpd", 
				raw_motion_list_file_name,
				usable_motion_list_file_name)
		end	
	end

	no_index_file_tasks(:unique_frames, Proc.new {"#{dir_name}/unique_frames.txt"}) do
		file unique_frames_file_name => [usable_motion_list_file_name] do
			call_java("yumyai.poseest.mmd.motion.ListUniquePosesFromList", usable_motion_list_file_name,
				unique_frames_file_name)
		end
	end

	no_index_file_tasks(:images, Proc.new {"#{dir_name}/images/done.txt"}) do
		file images_file_name => [unique_frames_file_name] do
			FileUtils.mkdir_p("#{dir_name}/images")
			file_count = 0
			File.open(unique_frames_file_name, :encoding=>"utf-8") do |fin|
				while !fin.eof?
					filename = fin.readline.strip
					comps = fin.readline.strip.split(" ")
					File.open("#{dir_name}/images/temp.txt", "w", :encoding=>"utf-8") do |fout|
						fout.write(options[:default_model] + "\n")
						fout.write(filename + "\n")
						fout.write("#{dir_name}/images/#{sprintf("%05d", file_count)}\n")
						comps.each do |s|
							fout.write(s + "\n")
						end
					end
					call_java("yumyai.poseest.mmd.motion.RenderAnimationFramesNoPhysicsFromConfig",
						"#{dir_name}/images/temp.txt")
					puts
					puts "Processed #{file_count} files"
					puts
					file_count += 1
				end
			end
			run("touch #{images_file_name}")
		end
	end

	no_index_file_tasks(:animated, Proc.new {"#{dir_name}/images/animated_done.txt"}) do
		file animated_file_name => [images_file_name] do
			dir_list = FileList.new("#{dir_name}/images/0*")
			dir_list.each do |dir|
				if !File.exists?("#{dir}/animated.gif")
					run("convert -delay 0 -loop 0 -alpha set -dispose previous #{dir}/*.png #{dir}/animated.gif")		
				end
				run("convert -delay 1 -loop 0 -alpha remove -background white -dispose background #{dir}/*.png #{dir}/animated_bg.gif")
			end
			run("touch #{animated_file_name}")
		end
	end

	no_index_file_tasks(:raw_motion_and_images_js, Proc.new {"#{dir_name}/html/raw_motion_and_images.js"}) do
		file raw_motion_and_images_js_file_name => [unique_frames_file_name] do
			data = []
			File.open(unique_frames_file_name, :encoding => "utf-8") do |fin|
				count = 0
				while !fin.eof?
					filename = fin.readline.strip
					fin.readline
					dir = "../images/#{sprintf("%05d", count)}"
					data << {
						:file_name => filename,
						:animation_name => "#{dir}/animated.gif",
						:first_frame_name => "#{dir}/00000.png"
					}
					count += 1
				end
			end
			FileUtils.mkdir_p("#{dir_name}/html")
			text = JSON.pretty_generate(data)
			File.open(raw_motion_and_images_js_file_name, "w", :enconding=>"utf-8") do |f|
				f.write("var data = ")
				f.write(text)
				f.write(";")
			end
		end
	end

	no_index_file_tasks(:raw_motion_and_images_json, Proc.new {"#{dir_name}/raw_motion_and_images.json"}) do
		file raw_motion_and_images_json_file_name => [unique_frames_file_name] do
			data = []
			File.open(unique_frames_file_name, :encoding => "utf-8") do |fin|
				count = 0
				while !fin.eof?
					filename = fin.readline.strip
					fin.readline
					dir = "#{dir_name}/images/#{sprintf("%05d", count)}"
					data << {
						:file_name => filename,
						:animation_name => "#{dir}/animated_bg.gif",
						:usage_flag => 2
					}
					count += 1
				end
			end	
			text = JSON.pretty_generate(data)
			File.open(raw_motion_and_images_json_file_name, "w", :enconding=>"utf-8") do |f|		
				f.write(text)		
			end
		end
	end

	def mark_motions_tasks
		namespace name do
			task :mark_motions => [raw_motion_and_images_json_file_name] do
				call_java("yumyai.poseest.mmd.motion.ViewAndMarkMmdMotionList",  raw_motion_and_images_json_file_name)
			end
		end
	end

	no_index_file_tasks(:used_motions_frames, Proc.new {"#{dir_name}/used_motions_frames.txt"}) do
		file used_motions_frames_file_name => [raw_motion_and_images_json_file_name, unique_frames_file_name] do
			raw_data = JSON.parse( File.read(raw_motion_and_images_json_file_name))
			
			frame_data = {}
			File.open(unique_frames_file_name, :encoding=>"utf-8") do |fin|
				lines = fin.readlines
				index = 0
				while index < lines.length do
					if lines[index].strip.length > 0
						frame_data[lines[index].strip] = lines[index+1].strip
					end
					index += 2			
				end
			end

			File.open(used_motions_frames_file_name, "w", :encoding=>"utf-8") do |fout|
				raw_data.each do |item|
					if item["usage_flag"] == 0
						fout.write(item["file_name"] + "\n")
						fout.write(frame_data[item["file_name"]] + "\n")
					end
				end
			end	
		end
	end

	no_index_file_tasks(:used_motions_frames_and_bones, Proc.new {"#{dir_name}/used_motions_frames_and_bones.txt"}) do
		file used_motions_frames_and_bones_file_name => [used_motions_frames_file_name] do
			call_java("yumyai.poseest.mmd.motion.WriteMotionBoneList", 
				used_motions_frames_file_name,
				used_motions_frames_and_bones_file_name)
		end
	end

	no_index_file_tasks(:motion_info, Proc.new {
		"#{dir_name}/motion_info.txt"
	}) do
		file motion_info_file_name => [used_motions_frames_and_bones_file_name] do
			run("cp -f #{used_motions_frames_and_bones_file_name} #{motion_info_file_name}")
		end
	end

	no_index_file_tasks(:motion_count, Proc.new {
		"#{dir_name}/motion_count.txt"
	}) do
		file motion_count_file_name => [motion_info_file_name] do
			call_java("yumyai.poseest.mmd.motion.CountMotion", motion_info_file_name, motion_count_file_name)
		end
	end
end

class MmdMotionSplitPool < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:source_file => "",
			:partitions => [80, 10, 10]
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def gen_tasks
		motion_info_tasks
		motion_count_tasks
	end

	def_index :partition do
		options[:partitions].length
	end

	one_index_file_tasks(:motion_info, :partition, Proc.new { |partition_index|
		"#{dir_name}/motion_info_#{sprintf("%02d", partition_index)}.txt"
	}) do |partition_index|
		motion_info_file = motion_info_file_name(partition_index)
		file motion_info_file => [options[:source_file]] do
			call_java("yumyai.poseest.mmd.motion.SplitMotionInfo",
				options[:source_file],
				"\"#{dir_name}/motion_info_%02d.txt\"",
				*options[:partitions])
		end
	end

	one_index_file_tasks(:motion_count, :partition, Proc.new { |partition_index|
		"#{dir_name}/motion_count_#{sprintf("%02d", partition_index)}.txt"
	}) do |partition_index|
		motion_count_file = motion_count_file_name(partition_index)
		motion_info_file = motion_info_file_name(partition_index)
		file motion_count_file => [motion_info_file] do
			call_java("yumyai.poseest.mmd.motion.CountMotion", motion_info_file, motion_count_file)
		end
	end
end

class MmdConsecutivelyLargerMotionPool < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:source_file => "",
			:counts => [1000, 10000, 100000]
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def gen_tasks
		motion_info_tasks
		motion_count_tasks
	end

	def_index :partition do
		options[:counts].length
	end

	one_index_file_tasks(:motion_info, :partition, Proc.new { |partition_index|
		"#{dir_name}/motion_info_#{sprintf("%02d", partition_index)}.txt"
	}) do |partition_index|
		motion_info_file = motion_info_file_name(partition_index)
		file motion_info_file => [options[:source_file]] do
			call_java("yumyai.poseest.mmd.motion.CreateConsecutivelyLargerMotionPool",
				options[:source_file],
				"\"#{dir_name}/motion_info_%02d.txt\"",
				*options[:counts])
		end
	end

	one_index_file_tasks(:motion_count, :partition, Proc.new { |partition_index|
		"#{dir_name}/motion_count_#{sprintf("%02d", partition_index)}.txt"
	}) do |partition_index|
		motion_count_file = motion_count_file_name(partition_index)
		motion_info_file = motion_info_file_name(partition_index)
		file motion_count_file => [motion_info_file] do
			call_java("yumyai.poseest.mmd.motion.CountMotion", motion_info_file, motion_count_file)
		end
	end
end