require 'rubygems'
require 'bundler/setup'
require 'fileutils'
require 'json'
require 'rake'
require File.expand_path(File.dirname(__FILE__) + "/../lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../java_lib.rb")
require File.expand_path(File.dirname(__FILE__) + "/../args.rb")
require File.expand_path(File.dirname(__FILE__) + "/pose_2d_configs.rb")

class PDJEvaluation < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:prediction_file => nil,
			:answer_file => nil,			
			:pose_2d_config => nil,
			:pose_xform => nil,
			:start_threshold => 0,
			:end_threshold => 0.2,
			:interval_count => 40,
			:torso_length_joints => ["arm_left", "leg_right"],
			:combo_bones => [
				["shoulders", ["arm_left", "arm_right"]],
				["elbows", ["elbow_left", "elbow_right"]],
				["wrists", ["wrist_left", "wrist_right"]],
				["hips", ["leg_left", "leg_right"]],
				["knees", ["knee_left", "knee_right"]],
				["ankles", ["ankle_left", "ankle_right"]],				
				["body", ["body_upper"]],
				["shoe_tips", ["tiptoe_left", "tiptoe_right"]],
				["shoe_tip_left", ["tiptoe_left"]],
				["shoe_tip_right", ["tiptoe_right"]],
				["hip_left", ["leg_left"]],
				["hip_right", ["leg_right"]],
				["hips", ["leg_left", "leg_right"]],
				["shoulder_left", ["arm_left"]],
				["shoulder_right", ["arm_right"]],
				["thumbs", ["thumb_left", "thumb_right"]]
			],
			:show_legends => true,
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def gen_tasks
		pdj_tasks
		graph_tasks
		average_loss_tasks
	end

	def point_distance(p, q)
		dx = p[0] - q[0]
		dy = p[1] - q[1]
		dx = dx.to_f
		dy = dy.to_f
		Math.sqrt(dx*dx + dy*dy)
	end

	def_index :bone do
		options[:pose_2d_config].bone_names.count + options[:combo_bones].count
	end

	def get_bone_name(index)
		if index < options[:pose_2d_config].bone_names.count
			options[:pose_2d_config].bone_names[index]
		else
			options[:combo_bones][index - options[:pose_2d_config].bone_names.count][0]
		end
	end

	no_index_file_tasks(:average_loss, Proc.new {"#{dir_name}/average_loss.txt"}) do
		file average_loss_file_name => [options[:prediction_file], options[:answer_file]] do
			predictions = JSON.parse(File.read(options[:prediction_file]))
			answers     = JSON.parse(File.read(options[:answer_file]))
			pose_config = options[:pose_2d_config]
			pose_xform  = options[:pose_xform]

			sum = 0.0
			predictions.count.times do |prediction_index|
				prediction = predictions[prediction_index]
				while answer_index < answers.count && !(answers[answer_index]["file_name"] == prediction["file_name"]) do
					answer_index += 1
				end
				if answer_index > answers.count
					break
				end
				prediction = pose_xform.transform(prediction["points"])
				answer = answers[answer_index]["points"]

				pose_config.bone_names.each do |bone_name|
					if answer.include?(bone_name)
						actual = prediction[bone_name]
						expected = answer[bone_name]
						if !expected.nil? && !actual.nil?					
							d = point_distance(actual, expected)
							sum += d*d/2							
						end
					end
				end				
			end

			loss = sum / predictions.count

			File.open(average_loss_file_name, "w") do |fout|
				fout.write("#{loss}\n")
			end
		end
	end

	no_index_file_tasks(:pdj, Proc.new {"#{dir_name}/pdj.txt"}) do
		file pdj_file_name => [options[:prediction_file], options[:answer_file]] do
			predictions = JSON.parse(File.read(options[:prediction_file]))
			answers     = JSON.parse(File.read(options[:answer_file]))
			pose_config = options[:pose_2d_config]
			pose_xform  = options[:pose_xform]

			torso_bone_0 = options[:torso_length_joints][0]
			torso_bone_1 = options[:torso_length_joints][1]

			thresholds = []
			(options[:interval_count]+1).times do |i|
				x = options[:start_threshold] + (options[:end_threshold] - options[:start_threshold])*i*1.0/options[:interval_count]
				thresholds << x
			end

			pdj = {}
			pose_config.bone_names.each do |bone_name|
				pdj[bone_name] = []
				thresholds.each do
					pdj[bone_name] << 0.0
				end
			end

			example_count = {}
			pose_config.bone_names.each do |bone_name|
				example_count[bone_name] = 0
			end
			answer_index = 0			
			predictions.count.times do |prediction_index|
				prediction = predictions[prediction_index]
				while answer_index < answers.count && !(answers[answer_index]["file_name"] == prediction["file_name"]) do
					answer_index += 1
				end
				if answer_index > answers.count
					break
				end
				prediction = pose_xform.transform(prediction["points"])
				answer = answers[answer_index]["points"]				

				if !answer.include?(torso_bone_0) || !answer.include?(torso_bone_1)
					next
				end
				torso_length = point_distance(answer[torso_bone_0], answer[torso_bone_1])

				pose_config.bone_names.each do |bone_name|
					if answer.include?(bone_name)
						actual = prediction[bone_name]
						expected = answer[bone_name]
						if !expected.nil? && !actual.nil?					
							d = point_distance(actual, expected)
							example_count[bone_name] += 1

							thresholds.count.times do |i|
								threshold = thresholds[i]
								if d < threshold*torso_length
									pdj[bone_name][i] += 1
								end
							end
						end
					end
				end
			end

=begin
			pose_config.bone_names.each do |bone_name|
				thresholds.count.times do |i|
					if example_count[bone_name] == 0
						pdj[bone_name][i] = 0
					else
						pdj[bone_name][i] /= example_count[bone_name]
					end
				end
			end
=end

			output = {}
			pose_config.bone_names.each do |bone_name|
				item = []
				thresholds.count.times do |i|
					item << [thresholds[i], pdj[bone_name][i], example_count[bone_name]]					
				end
				output[bone_name] = item
			end

			options[:combo_bones].each do |combo_joint_info|
				joint_name = combo_joint_info[0]
				source_joints = combo_joint_info[1]
				item = []
				thresholds.count.times do |i|
					the_count = 0
					total = 0
					source_joints.each do |source_joint|						
						the_count += output[source_joint][i][1]
						total += output[source_joint][i][2]
					end
					item << [thresholds[i], the_count, total]
				end
				output[joint_name] = item
			end

			File.open(pdj_file_name, "w") do |fout|
				fout.write(JSON.pretty_generate(output))
			end
		end
	end	

	one_index_file_tasks(:graph, :bone, Proc.new {|bone_index|
		"#{dir_name}/#{get_bone_name(bone_index)}.png"
	}) do |bone_index|
		graph_file = graph_file_name(bone_index)
		file graph_file => [pdj_file_name] do			
			bone_name = get_bone_name(bone_index)
			pdj = JSON.parse(File.read(pdj_file_name))[bone_name]

			args = {
				"title" => "#{bone_name} (#{pdj[-1][1]*1.0/pdj[-1][2]} at #{pdj[-1][0]})",
				"x_label" => "normalized precision threshold",
				"y_label" => "percentage of detected joints (PDJ)",
				"data" => [{
					"x" => pdj.map {|x| x[0]},
					"y" => pdj.map {|x| x[1]*1.0/x[2]},
					"label" => "pdj",
				}],
				"legend_location" => "upper left",
				"output_file" => graph_file,
				"show_legends" => options[:show_legends],
			}
			run("python script/plot_line_graph.py #{args_file(args)}")			
		end
	end
end

class PDJComparison < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:pdj_files => [],
			:pose_2d_config => nil,
			:combo_bones => [
				["shoulders", ["arm_left", "arm_right"]],
				["elbows", ["elbow_left", "elbow_right"]],
				["wrists", ["wrist_left", "wrist_right"]],
				["hips", ["leg_left", "leg_right"]],
				["knees", ["knee_left", "knee_right"]],
				["ankles", ["ankle_left", "ankle_right"]],
				["shoe_tips", ["tiptoe_left", "tiptoe_right"]],
				["shoe_tip_left", ["tiptoe_left"]],
				["shoe_tip_right", ["tiptoe_right"]],
				["body", ["body_upper"]],
				["hips", ["hip_left", "hip_right"]],
				["hip_left", ["leg_left"]],
				["hip_right", ["leg_right"]],
				["shoulder_left", ["arm_left"]],
				["shoulder_right", ["arm_right"]],
				["thumbs", ["thumb_left", "thumb_right"]]
			],
			:show_legends => true
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def gen_tasks		
		graph_tasks
	end

	def_index :bone do
		options[:pose_2d_config].bone_names.count + options[:combo_bones].count
	end

	def get_bone_name(index)
		if index < options[:pose_2d_config].bone_names.count
			options[:pose_2d_config].bone_names[index]
		else
			options[:combo_bones][index - options[:pose_2d_config].bone_names.count][0]
		end
	end	
	
	one_index_file_tasks(:graph, :bone, Proc.new {|bone_index|
		"#{dir_name}/#{get_bone_name(bone_index)}.png"
	}) do |bone_index|
		graph_file = graph_file_name(bone_index)
		file graph_file => options[:pdj_files].map {|x| x[1]} do			
			bone_name = get_bone_name(bone_index)
			
			data = []
			options[:pdj_files].each do |item|
				pdj = JSON.parse(File.read(item[1]))[bone_name]
				if item.count > 2
					style = item[2]
				else
					style = nil
				end
				item = {
					"x" => pdj.map {|x| x[0]},
					"y" => pdj.map {|x| x[1]*1.0/[x[2],1].max},
					"label" => item[0],
					"style" => style
				}
				data << item
			end			
			
			args = {
				"title" => "#{bone_name}",
				"x_label" => "normalized precision threshold",
				"y_label" => "percentage of detected joints (PDJ)",
				"data" => data,
				"legend_location" => "upper left",
				"output_file" => graph_file,
				"show_legends" => options[:show_legends],
			}
			run("python script/plot_line_graph.py #{args_file(args)}")			
		end
	end
end

