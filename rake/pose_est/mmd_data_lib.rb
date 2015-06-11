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

class MmdTrainingExampleParamsTasks < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:model_file => nil,
			:motion_file => nil,
			:background_file => nil,
			:count => 100,
			:limit => 100
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def gen_tasks
		mmbg_tasks
	end

	def_index :mmbg do
		(options[:count] / options[:limit]) + (if options[:count] % options[:limit] == 0 then 0 else 1 end)
	end

	one_index_file_tasks(:mmbg, :mmbg, Proc.new { |index|
		"#{dir_name}/mmbg-#{sprintf("%05d", index)}.txt"
	}) do |index|
		mmbg_file = mmbg_file_name(index)
		#file mmbg_file => [options[:model_file], options[:motion_file], options[:background_file]] do			
		file mmbg_file do
			call_java("yumyai.poseest.mmd.example.GenRandomTrainingExampleModelMotionBackground", 
				options[:model_file],
				options[:motion_file],
				options[:background_file],
				options[:count],
				options[:limit],
				"#{dir_name}/mmbg",
				"txt")			
		end
	end	
end

class MmdTrainingExampleTasks < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})		
		_options = {
			:mmbg_tasks => nil,
			:image_width => 400,
			:image_height => 400,
			:view_settings => [0.5, 0.6, 0.7, 0.8, 0.9, 1.0],
			:render_joint_pos => true,
			:camera_theta_min => 75,
			:camera_theta_max => 105,
			:camera_phi_min => 45,
			:camera_phi_max => 135,
			:camera_rotation_min => -30,
			:camera_rotation_max => 30,
			:camera_shift_x_min => -1,
			:camera_shift_x_max => 1,
			:camera_shift_y_min => -1,
			:camera_shift_y_max => 1,
			:output_bones => ["body_upper", "neck", "head", "nose_root", 
				"arm_left", "elbow_left", "wrist_left", 
				"arm_right", "elbow_right", "wrist_right",
				"leg_left", "knee_left", "ankle_left", "tiptoe_left",
				"leg_right", "knee_right", "ankle_right", "tiptoe_right"],
			:caffe_root => "/opt/caffe",
			:view_labeling_config_file => $p2dc_mmd_18.config_file_name
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def gen_tasks
		data_tasks
		settings_tasks
		gen_data_task
		#labels_for_yang_ramanan_tasks
		#lmdb_bones_tasks
		#image_lmdb_tasks
		#bone_lmdb_tasks
		#image_mean_tasks
		#image_mean_npy_tasks
		#image_mean_png_tasks
		#per_channel_image_mean_npy_tasks
		#per_channel_image_mean_tasks
		#per_channel_image_mean_png_tasks
		#loss_of_average_pose_tasks
		labeling_2d_tasks
		view_labeling_2d_tasks
		data_list_tasks
		data_count_tasks
	end

	def_index :mmbg do
		options[:mmbg_tasks].mmbg_count
	end

	def write_settings_file
		File.open(settings_file_name, "w") do |fout|
			text = JSON.pretty_generate({
				:image_width => options[:image_width],
				:image_height => options[:image_height],
				:render_joint_pos => options[:render_joint_pos],
				:view_settings => options[:view_settings],
				:camera_theta_min => options[:camera_theta_min],
				:camera_theta_max => options[:camera_theta_max],
				:camera_phi_min => options[:camera_phi_min],
				:camera_phi_max => options[:camera_phi_max],
				:camera_rotation_min => options[:camera_rotation_min],
				:camera_rotation_max => options[:camera_rotation_max],
				:camera_shift_x_min => options[:camera_shift_x_min],
				:camera_shift_x_max => options[:camera_shift_x_max],
				:camera_shift_y_min => options[:camera_shift_y_min],
				:camera_shift_y_max => options[:camera_shift_y_max],
			})
			fout.write(text)
		end
	end	

	no_index_file_tasks(:settings, Proc.new {"#{dir_name}/settings.txt"}) do
		file settings_file_name do
			write_settings_file
		end
	end

	def data_dir_name(index)
		dir_index = index/options[:mmbg_tasks].options[:limit]
		"#{dir_name}/data-#{sprintf("%05d", dir_index)}"
	end

	def image_file_name(index)		
		"#{data_dir_name(index)}/data_#{sprintf("%08d", index)}.png"
	end

	def pose_file_name(index)
		"#{data_dir_name(index)}/data_#{sprintf("%08d", index)}_data.txt"
	end	

	one_index_file_tasks(:data, :mmbg, Proc.new { |index|
		"#{dir_name}/data-#{sprintf("%05d", index)}/done.txt"
	}) do |index|
		data_file = data_file_name(index)
		#file data_file => [options[:mmbg_tasks].mmbg_file_name(index)] do
		file data_file do
			write_settings_file
			data_dir = "#{dir_name}/data-#{sprintf("%05d", index)}"
			FileUtils.mkdir_p(data_dir)			
			call_java("yumyai.poseest.mmd.example.GenTrainingExampleData",
				options[:mmbg_tasks].mmbg_file_name(index), 
				settings_file_name, 
				options[:mmbg_tasks].options[:model_file], 
				data_dir)
			run("touch #{data_dir}/done.txt")
		end
	end

	def gen_data_task
		namespace name do
			task :gen_data, [:start,:stop] do |t, args|
				files = []				
				start = args.start.to_i
				stop = args.stop.to_i
				(start..(stop-1)).each do |index|
					Rake::Task[data_file_name(index)].invoke
				end
			end
		end
	end

	def compute_data_list
		output = []
		skipped = []
		invalid_count = 0
		options[:mmbg_tasks].options[:count].times do |index|					
			begin				
				image_file = image_file_name(index)
				pose_file = pose_file_name(index)				
				pose = JSON.parse(File.read(pose_file))
				if (index+1) % 100 == 0
					puts "Processed #{index+1} files ..."
				end
				valid = true
				points = pose["points_2d"]					
				points.each do |bone_name, point|
					if point[0].to_f.nan? || point[1].to_f.nan?
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
			rescue
				puts "Error when processing \"#{pose_file}\""
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
		file data_list_file_name do
			compute_data_list
		end
	end

	no_index_file_tasks(:data_count, Proc.new { "#{dir_name}/data_count.txt"}) do
		file data_count_file_name do
			compute_data_list
		end
	end

=begin
	no_index_file_tasks(:lmdb_bones, Proc.new {"#{dir_name}/lmdb_bones.txt"}) do
		file lmdb_bones_file_name do
			File.open(lmdb_bones_file_name, "wt") do |fout|
				fout.write("#{options[:output_bones].length}\n")
				options[:output_bones].each do |bone_name|
					fout.write("#{bone_name}\n")
				end
			end
		end
	end

	no_index_file_tasks(:image_lmdb, Proc.new {"#{dir_name}/image.lmdb"}) do
		file image_lmdb_file_name => [lmdb_bones_file_name] do
			if !File.exists?(image_lmdb_file_name)
				run("python script/prepare_lmdb_from_mmd_examples.py #{dir_name} " +
					"#{options[:mmbg_tasks].options[:count]} " +
					"#{options[:mmbg_tasks].options[:limit]} #{lmdb_bones_file_name} #{image_lmdb_file_name} #{bone_lmdb_file_name}")
			end
		end
	end

	no_index_file_tasks(:bone_lmdb, Proc.new {"#{dir_name}/bone.lmdb"}) do
		file bone_lmdb_file_name => [lmdb_bones_file_name] do
			if !File.exists?(bone_lmdb_file_name)
				run("python script/prepare_lmdb_from_mmd_examples.py #{dir_name} " +
					"#{options[:mmbg_tasks].options[:count]} " +
					"#{options[:mmbg_tasks].options[:limit]} #{lmdb_bones_file_name} #{image_lmdb_file_name} #{bone_lmdb_file_name}")
			end
		end
	end

	no_index_file_tasks(:image_mean, Proc.new {"#{dir_name}/image_mean.binary_proto"}) do
		file image_mean_file_name => [image_lmdb_file_name] do
			run("#{options[:caffe_root]}/build/tools/compute_image_mean #{image_lmdb_file_name} #{image_mean_file_name}")
		end
	end

	no_index_file_tasks(:image_mean_npy, Proc.new {"#{dir_name}/image_mean.npy"}) do
		file image_mean_npy_file_name => [image_mean_file_name] do
			run("python script/convert_proto_mean_to_numpy.py #{image_mean_file_name} #{image_mean_npy_file_name}")
		end
	end

	no_index_file_tasks(:image_mean_png, Proc.new {"#{dir_name}/image_mean.png"}) do
		file image_mean_png_file_name => [image_mean_npy_file_name] do
			run("python script/numpy_to_image.py #{image_mean_npy_file_name} #{image_mean_png_file_name}")
		end
	end

	no_index_file_tasks(:per_channel_image_mean_npy, Proc.new {"#{dir_name}/per_channel_image_mean.npy"}) do
		file per_channel_image_mean_npy_file_name => [image_mean_npy_file_name] do
			run("python script/mean_to_per_channel_mean.py #{image_mean_npy_file_name} #{per_channel_image_mean_npy_file_name}")
		end		
	end	

	no_index_file_tasks(:per_channel_image_mean, Proc.new {"#{dir_name}/per_channel_image_mean.binary_proto"}) do
		file per_channel_image_mean_file_name => [per_channel_image_mean_npy_file_name] do
			run("python script/convert_numpy_to_proto_mean.py #{per_channel_image_mean_npy_file_name} #{per_channel_image_mean_file_name}")
		end
	end

	no_index_file_tasks(:per_channel_image_mean_png, Proc.new {"#{dir_name}/per_channel_image_mean.png"}) do
		file per_channel_image_mean_png_file_name => [per_channel_image_mean_npy_file_name] do
			run("python script/numpy_to_image.py #{per_channel_image_mean_npy_file_name} #{per_channel_image_mean_png_file_name}")
		end
	end

	no_index_file_tasks(:loss_of_average_pose, Proc.new {"#{dir_name}/loss_of_average_pose.txt"}) do
		file loss_of_average_pose_file_name => [lmdb_bones_file_name] do
			run("python script/compute_loss_of_average_pose.py #{dir_name} " +
				"#{options[:mmbg_tasks].options[:count]} " +
				"#{options[:mmbg_tasks].options[:limit]} " +
				"#{lmdb_bones_file_name} #{options[:image_width]} " +
				"#{loss_of_average_pose_file_name}")
		end
	end
=end

	no_index_file_tasks(:labeling_2d, Proc.new {"#{dir_name}/labeling_2d.txt"}) do
		file labeling_2d_file_name => data_file_list do
			output = []	
			options[:mmbg_tasks].options[:count].times do |index|
				dir_index = index/options[:mmbg_tasks].options[:limit]					
				data_dir = "#{dir_name}/data-#{sprintf("%05d", dir_index)}"
				data_file = "#{data_dir}/data_#{sprintf("%08d", index)}_data.txt"
				image_file = "#{data_dir}/data_#{sprintf("%08d", index)}.png"
				data = JSON.load(File.new(data_file))
				data = data["points_2d"]
				output << {
					"file_name" => image_file,
					"points" => data
				}				
			end
			File.open(labeling_2d_file_name, "w") do |fout|
				fout.write(JSON.pretty_generate(output))				
			end
		end
	end

	def view_labeling_2d_tasks
		namespace name do
			task :view_labeling_2d => [labeling_2d_file_name, options[:view_labeling_config_file]] do
				call_java("yumyai.poseest.label2d.ViewPoseLabeling2D", args_file(
					"labeling_2d_file" => labeling_2d_file_name,
					"pose_2d_config_file" => options[:view_labeling_config_file]
				))				
			end
		end
	end	

=begin
	no_index_file_tasks(:labels_for_yang_ramanan, Proc.new {
		"#{dir_name}/labels_for_yang_ramanan.txt"
	}) do		
		file labels_for_yang_ramanan_file_name => [data_file_name] do
			File.open(labels_for_yang_ramanan_file_name, "w") do |fout|
				joints = [
					"body_upper",		# 0
			        "body_lower",		# 1
			        "neck",				# 2
			        "head",				# 3
			        "shoulder_left",	# 4
			        "arm_left",			# 5
			        "elbow_left",		# 6
			        "wrist_left",		# 7
			        "thumb_left",		# 8
			        "leg_left",			# 9
			        "knee_left",		#10
			        "ankle_left",		#11
			        "shoulder_right",	#11
			        "arm_right",		#12
			        "elbow_right",		#13
			        "wrist_right",		#14
			        "thumb_right",		#15
			        "left_right",		#16
			        "knee_right",		#17
			        "ankle_right",		#18
			        "nose_tip",			#19
			        "nose_root",		#20
			        "tiptoe_right",		#21
			        "tiptoe_left"		#22
			    ]

				count = options[:mmbg_tasks].options[:count]
				fout.write("#{count}\n")
				count.times do |i|
					data_file = "#{dir_name}/data/data_#{sprintf("%08d", i)}_data.txt"
					png_file = "#{dir_name}/data/data_#{sprintf("%08d", i)}.png"
					data = JSON.parse(File.read(data_file))
					fout.write(File.expand_path(File.dirname(__FILE__) + "/../../#{png_file}\n"))
					joints.each do |joint_name|
						x = data["points_2d"][joint_name][0]
						y = data["points_2d"][joint_name][1]
						fout.write("#{x} #{y}\n")
					end
				end
			end
		end
	end
=end
end
