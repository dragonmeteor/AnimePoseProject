require 'rubygems'
require 'bundler/setup'
require 'fileutils'
require 'json'
require 'rake'
require File.expand_path(File.dirname(__FILE__) + "/../lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../java_lib.rb")
require File.expand_path(File.dirname(__FILE__) + "/pose_2d_configs.rb")

class StrictPCPEvaluation < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:prediction_file => nil,
			:answer_file => nil,
			:threshold => 0.5,			
			:pose_2d_config => nil,
			:pose_xform => nil,
			:combo_edges => [
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
				["torso", [
					["body_upper", "neck"],
				]],
				["head", [
					["head", "nose_root"],
				]],
			]	
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def gen_tasks
		pcp_tasks
	end

	def point_distance(p, q)
		dx = p[0] - q[0]
		dy = p[1] - q[1]
		Math.sqrt(dx*dx + dy*dy)
	end

	no_index_file_tasks(:pcp, Proc.new {"#{dir_name}/pcp.txt"}) do
		file pcp_file_name => [options[:prediction_file], options[:answer_file]] do
			predictions = JSON.parse(File.read(options[:prediction_file]))
			answers     = JSON.parse(File.read(options[:answer_file]))
			pose_config = options[:pose_2d_config]
			pose_xform  = options[:pose_xform]

			predictions.sort! { |x,y| x["file_name"] <=> y["file_name"] }
			answers.sort! { |x,y| x["file_name"] <=> y["file_name"] }

			fraction = {}
			example_count = {}
			pose_config.edges.each do |edge|				
				edge_name = "#{edge[0]}---#{edge[1]}"
				fraction[edge_name] = 0
				example_count[edge_name] = 0
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

				pose_config.edges.each do |edge|
					edge_name = "#{edge[0]}---#{edge[1]}"					
					parent = edge[0]
					child = edge[1]
					
					if !answer.include?(parent) || !answer.include?(child)
						next
					end 
					if !prediction.include?(parent) || !prediction.include?(child)
						next
					end
					if answer[parent].nil? || answer[child].nil?
						next
					end
					if prediction[parent].nil? || prediction[child].nil?
						next
					end

					example_count[edge_name] += 1

					a0 = answer[parent]
					a1 = answer[child]
					p0 = prediction[parent]
					p1 = prediction[child]

					bone_length = point_distance(a0, a1)
					d0 = point_distance(p0, a0)
					d1 = point_distance(p1, a1)
					if d0 <= options[:threshold]*bone_length && d1 <= options[:threshold]*bone_length
						fraction[edge_name] += 1
					end
				end
			end

=begin
			pose_config.edges.each do |edge|
				edge_name = "#{edge[0]}---#{edge[1]}"				
				puts fraction[edge_name]
			end
=end

			output = {}
			fraction.each do |edge_name, value|
				bone_names = edge_name.split("---")				
				item = {
					"edge" => bone_names,
					"score" => fraction[edge_name]*1.0/[example_count[edge_name],1].max,
					"count" => fraction[edge_name],
					#"total" => example_count[edge_name]
					"total" => predictions.count
				}
				output[edge_name] = item
			end

			options[:combo_edges].each do |combo_edge_info|
				combo_edge_name = combo_edge_info[0]
				edge_list = combo_edge_info[1]
				count = 0
				total = 0				
				edge_list.each do |edge|
					edge_name = "#{edge[0]}---#{edge[1]}"					
					count += output[edge_name]["count"]
					total += output[edge_name]["total"]
				end
				if total == 0
					total = 0
					count = 0
				end
				output[combo_edge_name] = {
					"edge" => combo_edge_name,
					"score" => count*1.0 /[total,1].max,
					"count" => count,
					"total" => total,
				}
			end

			File.open(pcp_file_name, "w") do |fout|
				fout.write(JSON.pretty_generate(output))
			end			
		end
	end
end