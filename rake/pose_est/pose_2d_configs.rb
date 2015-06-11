require 'rubygems'
require 'bundler/setup'
require 'fileutils'
require 'json'
require 'rake'
require File.expand_path(File.dirname(__FILE__) + "/../lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../java_lib.rb")

class Pose2DConfig < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:bone_names => [],		
			:bone_colors => {},
			:edges => [],
		}.merge(_options)
		super(_name, _dir_name, _options)
		check_consistency
	end

	def check_consistency
		bone_names.each do |bone_name|
			if !bone_colors.has_key?(bone_name)
				raise RuntimeError, "bone_colors does not contain entry for '#{bone_name}'"
			end			
		end
		edges.each do |edge|
			if !bone_names.include?(edge[0])
				raise RuntimeError, "'#{edge[0]}' present in edges but not in bone_names"
			end
			if !bone_names.include?(edge[1])
				raise RuntimeError, "'#{edge[1]}' present in edges but not in bone_names"
			end
		end
	end

	def gen_tasks
		config_tasks
	end	

	def bone_names
		options[:bone_names]
	end

	def bone_colors
		options[:bone_colors]
	end

	def edges
		options[:edges]
	end

	no_index_file_tasks(:config, Proc.new {
		"#{dir_name}/pose_config.txt"
	}) do
		file config_file_name do
			output = {
				"bone_names" => bone_names,
				"bone_colors" => bone_colors,
				"edges" => edges
			}
			File.open(config_file_name, "w") do |fout|
				fout.write(JSON.pretty_generate(output))
			end
		end
	end
end

class Pose2DTransform < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:bone_xforms => {}
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def gen_tasks
		config_tasks
	end

	no_index_file_tasks(:config, Proc.new {"#{dir_name}/xform_config.txt"}) do
		file config_file_name do
			output = {
				"bone_xforms" => options[:bone_xforms]
			}
			File.open(config_file_name, "w") do |fout|
				fout.write(JSON.pretty_generate(output))
			end
		end
	end

	def transform(in_pose)
		out_pose = {}
		options[:bone_xforms].each do |bone_name, xform_list|
			pos = [0,0]
			valid = true
			xform_list.each do |xform|				
				weight = xform[0]
				in_pos = in_pose[xform[1]]
				if !in_pos.nil?
					pos[0] += weight*in_pos[0]
					pos[1] += weight*in_pos[1]
				else
					valid = false
				end				
			end
			if valid	
				out_pose[bone_name] = pos
			end
		end
		out_pose
	end
end

def make_identify_bone_xforms(bone_names)
	output = {}
	bone_names.each do |bone_name|
		output[bone_name] = [[1.0, bone_name]]
	end
	output
end

class Pose2DProjection < Pose2DTransform
	def initialize(_name, _dir_name, _options={})		
		bone_xforms = _options[:bone_xforms] || {}
		bone_xforms = bone_xforms.merge(make_identify_bone_xforms(_options[:target_config].bone_names))				
		_options[:bone_xforms] = bone_xforms
		super(_name, _dir_name, _options)
	end
end

$p2dc_mmd_full = Pose2DConfig.new("p2dc_mmd_full", "data/pose_2d_configs/mmd_full",
	:bone_names => [
		"body_upper",
		"body_lower",
		"neck",
		"head",
		"nose_tip",
		"nose_root",
		"shoulder_left",
		"arm_left",
		"elbow_left",
		"wrist_left",
		"thumb_left",
		"shoulder_right",
		"arm_right",
		"elbow_right",
		"wrist_right",
		"thumb_right",
		"leg_left",
		"knee_left",
		"ankle_left",
		"tiptoe_left",
		"leg_right",
		"knee_right",
		"ankle_right",
		"tiptoe_right",
	],
	:bone_colors => {
		"body_upper" => [0.5, 0,  0],
		"body_lower" => [0.5, 0.5, 0.5],
		"neck" => [0.75, 0, 0],
		"head" => [1, 0, 0],
		"nose_tip" => [1, 0.25, 0.25],
		"nose_root" => [1, 0.5, 0.5],
		"shoulder_left" => [0, 0, 0.25],
		"arm_left" => [0, 0, 0.5],
		"elbow_left" => [0, 0, 0.75],
		"wrist_left" => [0, 0, 1],
		"thumb_left" => [0.5, 0.5, 1],
		"shoulder_right" => [0, 0.25,0],
		"arm_right" => [0, 0.5, 0],	
		"elbow_right" => [0, 0.75, 0],
		"wrist_right" => [0, 1, 0],
		"thumb_right" => [0.5, 1, 0.5],
		"leg_left" => [0.25, 0, 0.25],
		"knee_left" => [0.5, 0, 0.5],
		"ankle_left" => [0.75, 0, 0.75],
		"tiptoe_left" => [1, 0, 1],
		"leg_right" => [0.25, 0.25, 0],
		"knee_right" => [0.5, 0.5, 0],
		"ankle_right" => [0.75, 0.75, 0],
		"tiptoe_right" => [1, 1, 0],
	},
	:edges => [
		["body_upper", "neck"],
		["body_upper", "body_lower"],
		["neck", "head"],
		["head", "nose_tip"],
		["head", "nose_root"],		
		["neck", "shoulder_left"],
		["shoulder_left", "arm_left"],
		["arm_left", "elbow_left"],
		["elbow_left", "wrist_left"],
		["wrist_left", "thumb_left"],
		["neck", "shoulder_right"],
		["shoulder_right", "arm_right"],
		["arm_right", "elbow_right"],
		["elbow_right", "wrist_right"],
		["wrist_right", "thumb_right"],
		["body_lower", "leg_left"],
		["leg_left", "knee_left"],
		["knee_left", "ankle_left"],
		["ankle_left", "tiptoe_left"],
		["body_lower", "leg_right"],
		["leg_right", "knee_right"],
		["knee_right", "ankle_right"],
		["ankle_right", "tiptoe_right"],
	])

$p2dc_mmd_18 = Pose2DConfig.new("p2dc_mmd_18", "data/pose_2d_configs/mmd_18",
	:bone_names => [
		"body_upper",
		"neck",
		"head",		
		"nose_root",		
		"arm_left",
		"elbow_left",
		"wrist_left",		
		"arm_right",
		"elbow_right",
		"wrist_right",		
		"leg_left",
		"knee_left",
		"ankle_left",
		"tiptoe_left",
		"leg_right",
		"knee_right",
		"ankle_right",
		"tiptoe_right",
	],
	:bone_colors => {
		"body_upper" => [0.5, 0,  0],
		"body_lower" => [0.5, 0.5, 0.5],
		"neck" => [0.75, 0, 0],
		"head" => [1, 0, 0],
		"nose_tip" => [1, 0.25, 0.25],
		"nose_root" => [1, 0.5, 0.5],
		"shoulder_left" => [0, 0, 0.25],
		"arm_left" => [0, 0, 0.5],
		"elbow_left" => [0, 0, 0.75],
		"wrist_left" => [0, 0, 1],
		"thumb_left" => [0.5, 0.5, 1],
		"shoulder_right" => [0, 0.25,0],
		"arm_right" => [0, 0.5, 0],	
		"elbow_right" => [0, 0.75, 0],
		"wrist_right" => [0, 1, 0],
		"thumb_right" => [0.5, 1, 0.5],
		"leg_left" => [0.25, 0, 0.25],
		"knee_left" => [0.5, 0, 0.5],
		"ankle_left" => [0.75, 0, 0.75],
		"tiptoe_left" => [1, 0, 1],
		"leg_right" => [0.25, 0.25, 0],
		"knee_right" => [0.5, 0.5, 0],
		"ankle_right" => [0.75, 0.75, 0],
		"tiptoe_right" => [1, 1, 0],
	},
	:edges => [
		["body_upper", "neck"],		
		["neck", "head"],		
		["head", "nose_root"],				
		["neck", "arm_left"],
		["arm_left", "elbow_left"],
		["elbow_left", "wrist_left"],		
		["neck", "arm_right"],
		["arm_right", "elbow_right"],
		["elbow_right", "wrist_right"],		
		["body_upper", "leg_left"],
		["leg_left", "knee_left"],
		["knee_left", "ankle_left"],
		["ankle_left", "tiptoe_left"],
		["body_upper", "leg_right"],
		["leg_right", "knee_right"],
		["knee_right", "ankle_right"],
		["ankle_right", "tiptoe_right"],
	])

$p2dc_mmd_18_midlimbs = Pose2DConfig.new("p2dc_mmd_18_midlimbs", "data/pose_2d_configs/mmd_18_midlimbs",
	:bone_names => [
		"body_upper",		
		"neck",
		"head",		
		"nose_root",				
		"arm_left",	
		"mid_upper_arm_left",
		"elbow_left",
		"mid_lower_arm_left",
		"wrist_left",
		"arm_right",
		"mid_upper_arm_right",
		"elbow_right",
		"mid_lower_arm_right",
		"wrist_right",
		"leg_left",
		"mid_upper_leg_left",
		"knee_left",
		"mid_lower_leg_left",
		"ankle_left",
		"tiptoe_left",
		"leg_right",
		"mid_upper_leg_right",
		"knee_right",
		"mid_lower_leg_right",
		"ankle_right",
		"tiptoe_right",		
	],
	:bone_colors => {
		"body_upper" => [0.5, 0,  0],
		"body_lower" => [0.5, 0.5, 0.5],
		"neck" => [0.75, 0, 0],
		"head" => [1, 0, 0],
		"nose_tip" => [1, 0.25, 0.25],
		"nose_root" => [1, 0.5, 0.5],
		"shoulder_left" => [0, 0, 0.25],		
		"arm_left" => [0, 0, 0.5],
		"mid_upper_arm_left" => [0,0,(0.5+0.75)/2],
		"elbow_left" => [0, 0, 0.75],
		"mid_lower_arm_left" => [0,0,(0.75+1)/2],
		"wrist_left" => [0, 0, 1],
		"thumb_left" => [0.5, 0.5, 1],
		"shoulder_right" => [0, 0.25,0],		
		"arm_right" => [0, 0.5, 0],	
		"mid_upper_arm_right" => [0,(0.5+0.75)/2,0],
		"elbow_right" => [0, 0.75, 0],
		"mid_lower_arm_right" => [0,(0.75+1)/2,0],
		"wrist_right" => [0, 1, 0],
		"thumb_right" => [0.5, 1, 0.5],
		"leg_left" => [0.25, 0, 0.25],
		"mid_upper_leg_left" => [(0.25+0.5)/2, 0, (0.25+0.5)/2],
		"knee_left" => [0.5, 0, 0.5],
		"mid_lower_leg_left" => [(0.5+0.75)/2, 0, (0.5+0.75)/2],
		"ankle_left" => [0.75, 0, 0.75],
		"tiptoe_left" => [1, 0, 1],
		"leg_right" => [0.25, 0.25, 0],
		"mid_upper_leg_right" => [(0.25+0.5)/2, (0.25+0.5)/2, 0],
		"knee_right" => [0.5, 0.5, 0],
		"mid_lower_leg_right" => [(0.5+0.75)/2, (0.5+0.75)/2, 0],
		"ankle_right" => [0.75, 0.75, 0],
		"tiptoe_right" => [1, 1, 0],
	},
	:edges => [
		["body_upper", "neck"],
		["neck", "head"],		
		["head", "nose_root"],
		["neck", "arm_left"],		
		["arm_left", "mid_upper_arm_left"],
		["mid_upper_arm_left", "elbow_left"],
		["elbow_left", "mid_lower_arm_left"],
		["mid_lower_arm_left", "wrist_left"],		
		["neck", "arm_right"],
		["arm_right", "mid_upper_arm_right"],
		["mid_upper_arm_right", "elbow_right"],
		["elbow_right", "mid_lower_arm_right"],	
		["mid_lower_arm_right", "wrist_right"],			
		["body_upper", "leg_left"],
		["leg_left", "mid_upper_leg_left"],
		["mid_upper_leg_left", "knee_left"],
		["knee_left", "mid_lower_leg_left"],
		["mid_lower_leg_left", "ankle_left"],
		["ankle_left", "tiptoe_left"],
		["body_upper", "leg_right"],
		["leg_right", "mid_upper_leg_right"],
		["mid_upper_leg_right", "knee_right"],
		["knee_right", "mid_lower_leg_right"],
		["mid_lower_leg_right","ankle_right"],
		["ankle_right", "tiptoe_right"],
	])

$p2dc_mmd_21 = Pose2DConfig.new("p2dc_mmd_21", "data/pose_2d_configs/mmd_21",
	:bone_names => [
		"body_upper",		
		"neck",
		"head",		
		"nose_tip",
		"nose_root",
		"arm_left",
		"elbow_left",
		"wrist_left",
		"thumb_left",
		"arm_right",
		"elbow_right",
		"wrist_right",
		"thumb_right",
		"leg_left",
		"knee_left",
		"ankle_left",
		"tiptoe_left",
		"leg_right",
		"knee_right",
		"ankle_right",
		"tiptoe_right",
	],
	:bone_colors => {
		"body_upper" => [0.5, 0,  0],
		"body_lower" => [0.5, 0.5, 0.5],
		"neck" => [0.75, 0, 0],
		"head" => [1, 0, 0],
		"nose_tip" => [1, 0.25, 0.25],
		"nose_root" => [1, 0.5, 0.5],
		"shoulder_left" => [0, 0, 0.25],
		"arm_left" => [0, 0, 0.5],
		"elbow_left" => [0, 0, 0.75],
		"wrist_left" => [0, 0, 1],
		"thumb_left" => [0.5, 0.5, 1],
		"shoulder_right" => [0, 0.25,0],
		"arm_right" => [0, 0.5, 0],	
		"elbow_right" => [0, 0.75, 0],
		"wrist_right" => [0, 1, 0],
		"thumb_right" => [0.5, 1, 0.5],
		"leg_left" => [0.25, 0, 0.25],
		"knee_left" => [0.5, 0, 0.5],
		"ankle_left" => [0.75, 0, 0.75],
		"tiptoe_left" => [1, 0, 1],
		"leg_right" => [0.25, 0.25, 0],
		"knee_right" => [0.5, 0.5, 0],
		"ankle_right" => [0.75, 0.75, 0],
		"tiptoe_right" => [1, 1, 0],
	},
	:edges => [
		["body_upper", "neck"],
		["neck", "head"],
		["head", "nose_tip"],	
		["head", "nose_root"],		
		["neck", "arm_left"],		
		["arm_left", "elbow_left"],
		["elbow_left", "wrist_left"],
		["wrist_left", "thumb_left"],
		["neck", "arm_right"],
		["arm_right", "elbow_right"],
		["elbow_right", "wrist_right"],		
		["wrist_right", "thumb_right"],
		["body_upper", "leg_left"],
		["leg_left", "knee_left"],
		["knee_left", "ankle_left"],
		["ankle_left", "tiptoe_left"],
		["body_upper", "leg_right"],
		["leg_right", "knee_right"],
		["knee_right", "ankle_right"],
		["ankle_right", "tiptoe_right"],
	])

$p2dc_mmd_21_midlimbs = Pose2DConfig.new("p2dc_mmd_21_midlimbs", "data/pose_2d_configs/mmd_21_midlimbs",
	:bone_names => [
		"body_upper",		
		"neck",
		"head",		
		"nose_tip",
		"nose_root",
		"arm_left",
		"mid_upper_arm_left",
		"elbow_left",
		"mid_lower_arm_left",
		"wrist_left",
		"thumb_left",
		"arm_right",
		"elbow_right",
		"mid_upper_arm_right",
		"wrist_right",
		"mid_lower_arm_right",
		"thumb_right",
		"leg_left",
		"mid_upper_leg_left",
		"knee_left",
		"mid_lower_leg_left",
		"ankle_left",
		"tiptoe_left",
		"leg_right",
		"mid_upper_leg_right",
		"knee_right",
		"mid_lower_leg_right",
		"ankle_right",
		"tiptoe_right",
	],
	:bone_colors => {
		"body_upper" => [0.5, 0,  0],
		"body_lower" => [0.5, 0.5, 0.5],
		"neck" => [0.75, 0, 0],
		"head" => [1, 0, 0],
		"nose_tip" => [1, 0.25, 0.25],
		"nose_root" => [1, 0.5, 0.5],
		"shoulder_left" => [0, 0, 0.25],		
		"arm_left" => [0, 0, 0.5],
		"mid_upper_arm_left" => [0,0,(0.5+0.75)/2],
		"elbow_left" => [0, 0, 0.75],
		"mid_lower_arm_left" => [0,0,(0.75+1)/2],
		"wrist_left" => [0, 0, 1],
		"thumb_left" => [0.5, 0.5, 1],
		"shoulder_right" => [0, 0.25,0],		
		"arm_right" => [0, 0.5, 0],	
		"mid_upper_arm_right" => [0,(0.5+0.75)/2,0],
		"elbow_right" => [0, 0.75, 0],
		"mid_lower_arm_right" => [0,(0.75+1)/2,0],
		"wrist_right" => [0, 1, 0],
		"thumb_right" => [0.5, 1, 0.5],
		"leg_left" => [0.25, 0, 0.25],
		"mid_upper_leg_left" => [(0.25+0.5)/2, 0, (0.25+0.5)/2],
		"knee_left" => [0.5, 0, 0.5],
		"mid_lower_leg_left" => [(0.5+0.75)/2, 0, (0.5+0.75)/2],
		"ankle_left" => [0.75, 0, 0.75],
		"tiptoe_left" => [1, 0, 1],
		"leg_right" => [0.25, 0.25, 0],
		"mid_upper_leg_right" => [(0.25+0.5)/2, (0.25+0.5)/2, 0],
		"knee_right" => [0.5, 0.5, 0],
		"mid_lower_leg_right" => [(0.5+0.75)/2, (0.5+0.75)/2, 0],
		"ankle_right" => [0.75, 0.75, 0],
		"tiptoe_right" => [1, 1, 0],
	},
	:edges => [
		["body_upper", "neck"],
		["neck", "head"],
		["head", "nose_tip"],	
		["head", "nose_root"],		
		["neck", "arm_left"],		
		["arm_left", "mid_upper_arm_left"],
		["mid_upper_arm_left", "elbow_left"],
		["elbow_left", "mid_lower_arm_left"],
		["mid_lower_arm_left", "wrist_left"],
		["wrist_left", "thumb_left"],
		["neck", "arm_right"],
		["arm_right", "mid_upper_arm_right"],
		["mid_upper_arm_right", "elbow_right"],
		["elbow_right", "mid_lower_arm_right"],	
		["mid_lower_arm_right", "wrist_right"],	
		["wrist_right", "thumb_right"],
		["body_upper", "leg_left"],
		["leg_left", "mid_upper_leg_left"],
		["mid_upper_leg_left", "knee_left"],
		["knee_left", "mid_lower_leg_left"],
		["mid_lower_leg_left", "ankle_left"],
		["ankle_left", "tiptoe_left"],
		["body_upper", "leg_right"],
		["leg_right", "mid_upper_leg_right"],
		["mid_upper_leg_right", "knee_right"],
		["knee_right", "mid_lower_leg_right"],
		["mid_lower_leg_right","ankle_right"],
		["ankle_right", "tiptoe_right"],
	])

$p2dc_mmd_21_weights = Pose2DConfig.new("p2dc_mmd_21_weight", 
	"data/pose_2d_configs/mmd_21_weight",
	:bone_names => [
		"body_upper",		
		"neck",
		"head",		
		"nose_tip",
		"nose_root",
		"arm_left",
		"mid_upper_arm_left",
		"elbow_left",
		"mid_lower_arm_left",
		"wrist_left", 		
		"thumb_left",
		"arm_right",
		"elbow_right",
		"mid_upper_arm_right",
		"wrist_right",		
		"mid_lower_arm_right",
		"thumb_right",
		"leg_left",
		"mid_upper_leg_left",
		"knee_left",
		"mid_lower_leg_left",
		"ankle_left",
		"tiptoe_left",
		"leg_right",
		"mid_upper_leg_right",
		"knee_right",
		"mid_lower_leg_right",
		"ankle_right",
		"tiptoe_right",
	],
	:bone_colors => {
		"body_upper" => [0.5, 0,  0],
		"body_lower" => [0.5, 0.5, 0.5],
		"neck" => [0.75, 0, 0],
		"head" => [1, 0, 0],
		"nose_tip" => [1, 0.25, 0.25],
		"nose_root" => [1, 0.5, 0.5],
		"shoulder_left" => [0, 0, 0.25],		
		"arm_left" => [0, 0, 0.5],
		"mid_upper_arm_left" => [0,0,(0.5+0.75)/2],
		"elbow_left" => [0, 0, 0.75],
		"mid_lower_arm_left" => [0,0,(0.75+1)/2],
		"wrist_left" => [0, 0, 1],
		"thumb_left" => [0.5, 0.5, 1],
		"shoulder_right" => [0, 0.25,0],		
		"arm_right" => [0, 0.5, 0],	
		"mid_upper_arm_right" => [0,(0.5+0.75)/2,0],
		"elbow_right" => [0, 0.75, 0],
		"mid_lower_arm_right" => [0,(0.75+1)/2,0],
		"wrist_right" => [0, 1, 0],
		"thumb_right" => [0.5, 1, 0.5],
		"leg_left" => [0.25, 0, 0.25],
		"mid_upper_leg_left" => [(0.25+0.5)/2, 0, (0.25+0.5)/2],
		"knee_left" => [0.5, 0, 0.5],
		"mid_lower_leg_left" => [(0.5+0.75)/2, 0, (0.5+0.75)/2],
		"ankle_left" => [0.75, 0, 0.75],
		"tiptoe_left" => [1, 0, 1],
		"leg_right" => [0.25, 0.25, 0],
		"mid_upper_leg_right" => [(0.25+0.5)/2, (0.25+0.5)/2, 0],
		"knee_right" => [0.5, 0.5, 0],
		"mid_lower_leg_right" => [(0.5+0.75)/2, (0.5+0.75)/2, 0],
		"ankle_right" => [0.75, 0.75, 0],
		"tiptoe_right" => [1, 1, 0],
	},
	:edges => [
		["body_upper", "neck"],
		["neck", "head"],
		["head", "nose_tip"],	
		["head", "nose_root"],		
		["neck", "arm_left"],		
		["arm_left", "mid_upper_arm_left"],
		["mid_upper_arm_left", "elbow_left"],
		["elbow_left", "mid_lower_arm_left"],
		["mid_lower_arm_left", "wrist_left"],
		["wrist_left", "thumb_left"],
		["neck", "arm_right"],
		["arm_right", "mid_upper_arm_right"],
		["mid_upper_arm_right", "elbow_right"],
		["elbow_right", "mid_lower_arm_right"],	
		["mid_lower_arm_right", "wrist_right"],	
		["wrist_right", "thumb_right"],
		["body_upper", "leg_left"],
		["leg_left", "mid_upper_leg_left"],
		["mid_upper_leg_left", "knee_left"],
		["knee_left", "mid_lower_leg_left"],
		["mid_lower_leg_left", "ankle_left"],
		["ankle_left", "tiptoe_left"],
		["body_upper", "leg_right"],
		["leg_right", "mid_upper_leg_right"],
		["mid_upper_leg_right", "knee_right"],
		["knee_right", "mid_lower_leg_right"],
		["mid_lower_leg_right","ankle_right"],
		["ankle_right", "tiptoe_right"],
	])

["elbow_left", "mid_lower_arm_left", "wrist_left", "thumb_left", 
 "elbow_right", "mid_lower_arm_right", "wrist_right", "thumb_right",
 "knee_left", "mid_lower_leg_left", "ankle_left", "tiptoe_left",
 "knee_right", "mid_lower_leg_right", "ankle_right", "tiptoe_right"].each do |bone_name|
	9.times do |i|
		$p2dc_mmd_21_weights.bone_names << "#{bone_name}_#{i+1}"
		$p2dc_mmd_21_weights.bone_colors["#{bone_name}_#{i+1}"] = $p2dc_mmd_21_weights.bone_colors[bone_name]
	end
end

$p2dc_mmd_21_weights.check_consistency

$p2dc_flic = Pose2DConfig.new("p2dc_flic",
	"data/pose_2d_configs/flic",
	:bone_names => [
		"left_shoulder",
		"left_elbow",
		"left_wrist",
		"right_shoulder",
		"right_elbow",
		"right_wrist",
		"left_hip",
		"right_hip",
		"left_eye",
		"right_eye",
		"nose",
	],
	:bone_colors => {		
		"right_eye" => [1, 0.75, 0.75],
		"left_eye" => [1, 0.25, 0.25],
		"nose" => [1, 0.5, 0.5],		
		"left_shoulder" => [0, 0, 0.5],		
		"left_elbow" => [0, 0, 0.75],		
		"left_wrist" => [0, 0, 1],				
		"right_shoulder" => [0, 0.5, 0],			
		"right_elbow" => [0, 0.75, 0],		
		"right_wrist" => [0, 1, 0],		
		"left_hip" => [0.5, 0, 0.5],
		"right_hip" => [0.5, 0.5, 0],
	},
	:edges => [
		["left_shoulder", "left_elbow"],
		["left_elbow", "left_wrist"],
		["right_shoulder", "right_elbow"],
		["right_elbow", "right_wrist"],
		["nose", "left_eye"],
		["nose", "right_eye"],
		["left_shoulder", "left_hip"],
		["right_shoulder", "right_hip"],
	]	
)

$p2dc_flic_renamed = Pose2DConfig.new("p2dc_flic_renamed",
	"data/pose_2d_configs/flic_renamed",
	:bone_names => [
		"arm_left",
		"elbow_left",
		"wrist_left",
		"arm_right",
		"elbow_right",
		"wrist_right",
		"leg_left",
		"leg_right",
		"eye_left",
		"eye_right",
		"nose",
	],
	:bone_colors => {
		"eye_right" => [1, 0.75, 0.75],
		"eye_left" => [1, 0.25, 0.25],
		"nose" => [1, 0.5, 0.5],		
		"arm_left" => [0, 0, 0.5],		
		"elbow_left" => [0, 0, 0.75],		
		"wrist_left" => [0, 0, 1],				
		"arm_right" => [0, 0.5, 0],			
		"elbow_right" => [0, 0.75, 0],		
		"wrist_right" => [0, 1, 0],		
		"leg_left" => [0.5, 0, 0.5],
		"leg_right" => [0.5, 0.5, 0],
	},
	:edges => [
		["arm_left", "elbow_left"],
		["elbow_left", "wrist_left"],
		["arm_right", "elbow_right"],
		["elbow_right", "wrist_right"],
		["nose", "eye_left"],
		["nose", "eye_right"],
		["arm_left", "leg_left"],
		["arm_right", "leg_right"],
	]
)

$p2dc_lsp = Pose2DConfig.new("p2dc_lsp",
	"data/pose_2d_configs/lsp",
	:bone_names => [
		'right_ankle',
		'right_knee',
		'right_hip',
		'left_hip',
		'left_knee',
		'left_ankle',
		'right_wrist',
		'right_elbow',
		'right_shoulder',
		'left_shoulder',
		'left_elbow',
		'left_wrist',
		"neck",
		'head_top'
	],
	:bone_colors => {		
		'neck' => [0.75, 0, 0],
		"head_top" => [1, 0, 0],		
		"left_shoulder" => [0, 0, 0.5],
		"left_elbow" => [0, 0, 0.75],
		"left_wrist" => [0, 0, 1],		
		"right_shoulder" => [0, 0.5, 0],	
		"right_elbow" => [0, 0.75, 0],
		"right_wrist" => [0, 1, 0],		
		"left_hip" => [0.25, 0, 0.25],
		"left_knee" => [0.5, 0, 0.5],
		"left_ankle" => [0.75, 0, 0.75],		
		"right_hip" => [0.25, 0.25, 0],
		"right_knee" => [0.5, 0.5, 0],
		"right_ankle" => [0.75, 0.75, 0],		
	},
	:edges => [
		["neck", "head_top"],
		["left_shoulder", "left_elbow"],
		["left_elbow", "left_wrist"],
		["right_shoulder", "right_elbow"],
		["right_elbow", "right_wrist"],
		["left_hip", "left_knee"],
		["left_knee", "left_ankle"],
		["right_hip", "right_knee"],
		["right_knee", "right_ankle"],
		["left_shoulder", "left_hip"],
		["right_shoulder", "right_hip"],
		["neck", "left_shoulder"],
		["neck", "right_shoulder"],
	]
)

$p2dc_lsp_renamed = Pose2DConfig.new("p2dc_lsp_renamed",
	"data/pose_2d_configs/lsp_renamed",
	:bone_names => [
		'ankle_right',
		'knee_right',
		'leg_right',
		'leg_left',
		'knee_left',
		'ankle_left',
		'wrist_right',
		'elbow_right',
		'arm_right',
		'arm_left',
		'elbow_left',
		'wrist_left',
		"neck",
		'head_top'
	],
	:bone_colors => {		
		'neck' => [0.75, 0, 0],
		"head_top" => [1, 0, 0],		
		"arm_left" => [0, 0, 0.5],
		"elbow_left" => [0, 0, 0.75],
		"wrist_left" => [0, 0, 1],		
		"arm_right" => [0, 0.5, 0],	
		"elbow_right" => [0, 0.75, 0],
		"wrist_right" => [0, 1, 0],		
		"leg_left" => [0.25, 0, 0.25],
		"knee_left" => [0.5, 0, 0.5],
		"ankle_left" => [0.75, 0, 0.75],		
		"leg_right" => [0.25, 0.25, 0],
		"knee_right" => [0.5, 0.5, 0],
		"ankle_right" => [0.75, 0.75, 0],		
	},
	:edges => [
		["neck", "head_top"],
		["arm_left", "elbow_left"],
		["elbow_left", "wrist_left"],
		["arm_right", "elbow_right"],
		["elbow_right", "wrist_right"],
		["leg_left", "knee_left"],
		["knee_left", "ankle_left"],
		["leg_right", "knee_right"],
		["knee_right", "ankle_right"],
		["arm_left", "leg_left"],
		["arm_right", "leg_right"],
		["neck", "arm_left"],
		["neck", "arm_right"],
	]
)

$p2dc_lsp_renamed_with_hip = Pose2DConfig.new("p2dc_lsp_renamed_with_hip",
	"data/pose_2d_configs/lsp_renamed_with_hip",
	:bone_names => [
		'ankle_right',
		'knee_right',
		'leg_right',
		'leg_left',
		'knee_left',
		'ankle_left',
		'wrist_right',
		'elbow_right',
		'arm_right',
		'arm_left',
		'elbow_left',
		'wrist_left',
		"neck",
		'head_top',
		'hip',
	],
	:bone_colors => {		
		'neck' => [0.75, 0, 0],
		"head_top" => [1, 0, 0],		
		"arm_left" => [0, 0, 0.5],
		"elbow_left" => [0, 0, 0.75],
		"wrist_left" => [0, 0, 1],		
		"arm_right" => [0, 0.5, 0],	
		"elbow_right" => [0, 0.75, 0],
		"wrist_right" => [0, 1, 0],		
		"leg_left" => [0.25, 0, 0.25],
		"knee_left" => [0.5, 0, 0.5],
		"ankle_left" => [0.75, 0, 0.75],		
		"leg_right" => [0.25, 0.25, 0],
		"knee_right" => [0.5, 0.5, 0],
		"ankle_right" => [0.75, 0.75, 0],
		"hip" => [0.5, 0 ,0],
	},
	:edges => [
		["neck", "head_top"],
		["arm_left", "elbow_left"],
		["elbow_left", "wrist_left"],
		["arm_right", "elbow_right"],
		["elbow_right", "wrist_right"],
		["leg_left", "knee_left"],
		["knee_left", "ankle_left"],
		["leg_right", "knee_right"],
		["knee_right", "ankle_right"],
		["arm_left", "leg_left"],
		["arm_right", "leg_right"],
		["hip", "neck"],
		["neck", "arm_left"],
		["neck", "arm_right"],
	]
)

$p2dc_lsp_renamed_midlimbs = Pose2DConfig.new("p2dc_lsp_renamed_midlimbs",
	"data/pose_2d_configs/lsp_renamed_midlimbs",
	:bone_names => [
		'ankle_right',
		"mid_lower_leg_right",
		'knee_right',
		"mid_upper_leg_right",
		'leg_right',
		'leg_left',
		"mid_upper_leg_left",
		'knee_left',
		"mid_lower_leg_left",
		'ankle_left',		
		'wrist_right',
		"mid_lower_arm_right",
		'elbow_right',
		"mid_upper_arm_right",
		'arm_right',
		'arm_left',
		"mid_upper_arm_left",
		'elbow_left',
		"mid_lower_arm_left",
		'wrist_left',
		"neck",
		'head_top'
	],
	:bone_colors => {		
		'neck' => [0.75, 0, 0],
		"head_top" => [1, 0, 0],		
		"arm_left" => [0, 0, 0.5],
		"mid_upper_arm_left" => [0, 0, (0.5+0.75)/2],
		"elbow_left" => [0, 0, 0.75],
		"mid_lower_arm_left" => [0,0,(0.75+1)/2],
		"wrist_left" => [0, 0, 1],		
		"arm_right" => [0, 0.5, 0],	
		"mid_upper_arm_right" => [0, (0.5+0.75)/2, 0],
		"elbow_right" => [0, 0.75, 0],
		"mid_lower_arm_right" => [0, (0.75+1)/2, 0],
		"wrist_right" => [0, 1, 0],		
		"leg_left" => [0.25, 0, 0.25],
		"mid_upper_leg_left" => [(0.25+0.5)/2, 0, (0.25+0.5)/2],
		"knee_left" => [0.5, 0, 0.5],
		"mid_lower_leg_left" => [(0.5+0.75)/2, 0, (0.5+0.75)/2],
		"ankle_left" => [0.75, 0, 0.75],	
		"leg_right" => [0.25, 0.25, 0],
		"mid_upper_leg_right" => [(0.25+0.5)/2, (0.25+0.5)/2, 0],
		"knee_right" => [0.5, 0.5, 0],
		"mid_lower_leg_right" => [(0.5+0.75)/2, (0.5+0.75)/2, 0],
		"ankle_right" => [0.75, 0.75, 0],		
	},
	:edges => [
		["neck", "head_top"],
		["arm_left", "mid_upper_arm_left"],
		["mid_upper_arm_left", "elbow_left"],
		["elbow_left", "mid_lower_arm_left"],
		["mid_lower_arm_left", "wrist_left"],
		["arm_right", "mid_upper_arm_right"],
		["mid_upper_arm_right", "elbow_right"],
		["elbow_right", "mid_lower_arm_right"],
		["mid_lower_arm_right", "wrist_right"],
		["leg_left", "mid_upper_leg_left"],
		["mid_upper_leg_left", "knee_left"],
		["knee_left", "mid_lower_leg_left"],
		["mid_lower_leg_left", "ankle_left"],
		["leg_right", "mid_upper_leg_right"],
		["mid_upper_leg_right", "knee_right"],
		["knee_right", "mid_lower_leg_right"],
		["mid_lower_leg_right", "ankle_right"],
		["arm_left", "leg_left"],
		["arm_right", "leg_right"],
		["neck", "arm_left"],
		["neck", "arm_right"],
	]
)

p2dc_tasks = [
	$p2dc_mmd_full,
	$p2dc_mmd_18,
	$p2dc_mmd_18_midlimbs,
	$p2dc_mmd_21,
	$p2dc_mmd_21_midlimbs,
	$p2dc_mmd_21_weights,
	$p2dc_flic,
	$p2dc_flic_renamed,
	$p2dc_lsp,
	$p2dc_lsp_renamed, 
	$p2dc_lsp_renamed_midlimbs,
	$p2dc_lsp_renamed_with_hip,
]

task_from_tasks("pose_2d_configs", p2dc_tasks.map {|x| x.name}, [
	"config"
	].product(["", "_clean"]).map {|x| "#{x[0]}#{x[1]}"})

$p2xf_project_mmd_full = Pose2DProjection.new("p2xf_mmd_full", 
	"data/pose_2d_xforms/project_mmd_full", 
	:target_config => $p2dc_mmd_full)
$p2xf_project_mmd_18 = Pose2DProjection.new("p2xf_mmd_18", 
	"data/pose_2d_xforms/project_mmd_18", 
	:target_config => $p2dc_mmd_18)
$p2xf_project_mmd_21 = Pose2DProjection.new("p2xf_mmd_21", 
	"data/pose_2d_xforms/project_mmd_21", 
	:target_config => $p2dc_mmd_21)
$p2xf_project_mmd_21_midlimbs = Pose2DProjection.new("p2xf_mmd_21_midlimbs", 
	"data/pose_2d_xforms/project_mmd_21_midlimbs", 
	:target_config => $p2dc_mmd_21_midlimbs)
$p2xf_project_flic = Pose2DProjection.new("p2xf_project_flic", 
	"data/pose_2d_xforms/project_flic",
	:target_config => $p2dc_flic)
$p2xf_project_lsp = Pose2DProjection.new("p2xf_project_lsp", 
	"data/pose_2d_xforms/project_lsp",
	:target_config => $p2dc_lsp)
$p2xf_project_lsp_renamed = Pose2DProjection.new("p2xf_project_lsp_renamed", 
	"data/pose_2d_xforms/project_lsp_renamed",
	:target_config => $p2dc_lsp_renamed)

mmd_18_add_midlimbs_xforms = make_identify_bone_xforms($p2dc_mmd_18.bone_names)
mmd_18_add_midlimbs_xforms["mid_upper_arm_left"] = [[0.5, "arm_left"], [0.5, "elbow_left"]]
mmd_18_add_midlimbs_xforms["mid_upper_arm_right"] = [[0.5, "arm_right"], [0.5, "elbow_right"]]
mmd_18_add_midlimbs_xforms["mid_lower_arm_left"] = [[0.5, "elbow_left"], [0.5, "wrist_left"]]
mmd_18_add_midlimbs_xforms["mid_lower_arm_right"] = [[0.5, "elbow_right"], [0.5, "wrist_right"]]
mmd_18_add_midlimbs_xforms["mid_upper_leg_left"] = [[0.5, "leg_left"], [0.5, "knee_left"]]
mmd_18_add_midlimbs_xforms["mid_upper_leg_right"] = [[0.5, "leg_right"], [0.5, "knee_right"]]
mmd_18_add_midlimbs_xforms["mid_lower_leg_left"] = [[0.5, "knee_left"], [0.5, "ankle_left"]]
mmd_18_add_midlimbs_xforms["mid_lower_leg_right"] = [[0.5, "knee_right"], [0.5, "ankle_right"]]
#puts add_midlimbs_xforms
$p2xf_mmd_18_add_midlimbs = Pose2DTransform.new("p2xf_mmd_18_add_midlimbs", 
	"data/pose_2d_xforms/mmd_18_add_midlimbs", :bone_xforms => mmd_18_add_midlimbs_xforms)

mmd_21_add_midlimbs_xforms = make_identify_bone_xforms($p2dc_mmd_21.bone_names)
mmd_21_add_midlimbs_xforms["mid_upper_arm_left"] = [[0.5, "arm_left"], [0.5, "elbow_left"]]
mmd_21_add_midlimbs_xforms["mid_upper_arm_right"] = [[0.5, "arm_right"], [0.5, "elbow_right"]]
mmd_21_add_midlimbs_xforms["mid_lower_arm_left"] = [[0.5, "elbow_left"], [0.5, "wrist_left"]]
mmd_21_add_midlimbs_xforms["mid_lower_arm_right"] = [[0.5, "elbow_right"], [0.5, "wrist_right"]]
mmd_21_add_midlimbs_xforms["mid_upper_leg_left"] = [[0.5, "leg_left"], [0.5, "knee_left"]]
mmd_21_add_midlimbs_xforms["mid_upper_leg_right"] = [[0.5, "leg_right"], [0.5, "knee_right"]]
mmd_21_add_midlimbs_xforms["mid_lower_leg_left"] = [[0.5, "knee_left"], [0.5, "ankle_left"]]
mmd_21_add_midlimbs_xforms["mid_lower_leg_right"] = [[0.5, "knee_right"], [0.5, "ankle_right"]]
#puts add_midlimbs_xforms
$p2xf_mmd_21_add_midlimbs = Pose2DTransform.new("p2xf_mmd_21_add_midlimbs", 
	"data/pose_2d_xforms/mmd_21_add_midlimbs", :bone_xforms => mmd_21_add_midlimbs_xforms)

mmd_21_add_weights_xforms = make_identify_bone_xforms($p2dc_mmd_21.bone_names)
mmd_21_add_weights_xforms["mid_upper_arm_left"] = [[0.5, "arm_left"], [0.5, "elbow_left"]]
mmd_21_add_weights_xforms["mid_upper_arm_right"] = [[0.5, "arm_right"], [0.5, "elbow_right"]]
mmd_21_add_weights_xforms["mid_lower_arm_left"] = [[0.5, "elbow_left"], [0.5, "wrist_left"]]
mmd_21_add_weights_xforms["mid_lower_arm_right"] = [[0.5, "elbow_right"], [0.5, "wrist_right"]]
mmd_21_add_weights_xforms["mid_upper_leg_left"] = [[0.5, "leg_left"], [0.5, "knee_left"]]
mmd_21_add_weights_xforms["mid_upper_leg_right"] = [[0.5, "leg_right"], [0.5, "knee_right"]]
mmd_21_add_weights_xforms["mid_lower_leg_left"] = [[0.5, "knee_left"], [0.5, "ankle_left"]]
mmd_21_add_weights_xforms["mid_lower_leg_right"] = [[0.5, "knee_right"], [0.5, "ankle_right"]]

["elbow_left", "mid_lower_arm_left", "wrist_left", "thumb_left", 
 "elbow_right", "mid_lower_arm_right", "wrist_right", "thumb_right",
 "knee_left", "mid_lower_leg_left", "ankle_left", "tiptoe_left",
 "knee_right", "mid_lower_leg_right", "ankle_right", "tiptoe_right"].each do |bone_name|
	9.times do |i|
		mmd_21_add_weights_xforms["#{bone_name}_#{i+1}"] = mmd_21_add_weights_xforms[bone_name]
	end
end

$p2xf_mmd_21_add_weights = Pose2DTransform.new("p2xf_mmd_21_add_weights", 
	"data/pose_2d_xforms/mmd_21_add_weights", :bone_xforms => mmd_21_add_weights_xforms)

$p2xf_rename_flic = Pose2DTransform.new("p2xf_rename_flic",
	"data/pose_2d_xforms/rename_flic",
	:bone_xforms => {
		"arm_left" => [[1, "left_shoulder"]],
		"elbow_left" => [[1, "left_elbow"]],
		"wrist_left" => [[1, "left_wrist"]],
		"arm_right" => [[1, "right_shoulder"]],
		"elbow_right" => [[1, "right_elbow"]],
		"wrist_right" => [[1, "right_wrist"]],
		"leg_left" => [[1, "left_hip"]],
		"leg_right" => [[1, "right_hip"]],
		"eye_left" => [[1, "left_eye"]],
		"eye_right" => [[1, "right_eye"]],
		"nose" => [[1, "nose"]],
	})

$p2xf_rename_lsp = Pose2DTransform.new("p2xf_rename_lsp",
	"data/pose_2d_xforms/rename_lsp",
	:bone_xforms => {
		'ankle_right' => [[1, "right_ankle"]],		
		'knee_right' => [[1, "right_knee"]],
		'leg_right' => [[1, "right_hip"]],
		'leg_left' => [[1, "left_hip"]],
		'knee_left' => [[1, "left_knee"]],
		'ankle_left' => [[1, "left_ankle"]],
		'wrist_right' => [[1, "right_wrist"]],
		'elbow_right' => [[1, "right_elbow"]],
		'arm_right' => [[1, "right_shoulder"]],
		'arm_left' => [[1, "left_shoulder"]],
		'elbow_left' => [[1, "left_elbow"]],
		'wrist_left' => [[1, "left_wrist"]],
		"neck" => [[1, "neck"]],
		'head_top' => [[1, "head_top"]],
	})

$p2xf_rename_lsp_add_hip = Pose2DTransform.new("p2xf_rename_lsp_add_hip",
	"data/pose_2d_xforms/rename_lsp_add_hip",
	:bone_xforms => {
		'ankle_right' => [[1, "right_ankle"]],		
		'knee_right' => [[1, "right_knee"]],
		'leg_right' => [[1, "right_hip"]],
		'leg_left' => [[1, "left_hip"]],
		'knee_left' => [[1, "left_knee"]],
		'ankle_left' => [[1, "left_ankle"]],
		'wrist_right' => [[1, "right_wrist"]],
		'elbow_right' => [[1, "right_elbow"]],
		'arm_right' => [[1, "right_shoulder"]],
		'arm_left' => [[1, "left_shoulder"]],
		'elbow_left' => [[1, "left_elbow"]],
		'wrist_left' => [[1, "left_wrist"]],
		"neck" => [[1, "neck"]],
		'head_top' => [[1, "head_top"]],
		"hip" => [[0.5, "left_hip"], [0.5, "right_hip"]],
	})

$p2xf_project_lsp_add_hip = Pose2DTransform.new("p2xf_project_lsp_add_hip",
	"data/pose_2d_xforms/project_lsp_add_hip",
	:bone_xforms => {
		'ankle_right' => [[1, "ankle_right"]],		
		'knee_right' => [[1, "knee_right"]],
		'leg_right' => [[1, "leg_right"]],
		'leg_left' => [[1, "leg_left"]],
		'knee_left' => [[1, "knee_left"]],
		'ankle_left' => [[1, "ankle_left"]],
		'wrist_right' => [[1, "wrist_right"]],
		'elbow_right' => [[1, "elbow_right"]],
		'arm_right' => [[1, "arm_right"]],
		'arm_left' => [[1, "arm_left"]],
		'elbow_left' => [[1, "elbow_left"]],
		'wrist_left' => [[1, "wrist_left"]],
		"neck" => [[1, "neck"]],
		'head_top' => [[1, "head_top"]],
		"hip" => [[0.5, "leg_left"], [0.5, "leg_right"]],
	})

$p2xf_rename_lsp_add_midlimbs = Pose2DTransform.new("p2xf_rename_lsp_add_midlimbs",
	"data/pose_2d_xforms/rename_lsp_add_midlimbs",
	:bone_xforms => {
		'ankle_right' => [[1, "right_ankle"]],		
		'knee_right' => [[1, "right_knee"]],
		'leg_right' => [[1, "right_hip"]],
		'leg_left' => [[1, "left_hip"]],
		'knee_left' => [[1, "left_knee"]],
		'ankle_left' => [[1, "left_ankle"]],
		'wrist_right' => [[1, "right_wrist"]],
		'elbow_right' => [[1, "right_elbow"]],
		'arm_right' => [[1, "right_shoulder"]],
		'arm_left' => [[1, "left_shoulder"]],
		'elbow_left' => [[1, "left_elbow"]],
		'wrist_left' => [[1, "left_wrist"]],
		"neck" => [[1, "neck"]],
		'head_top' => [[1, "head_top"]],
		"mid_upper_arm_left" => [[0.5, "left_shoulder"], [0.5, "left_elbow"]],
		"mid_upper_arm_right" => [[0.5, "right_shoulder"], [0.5, "right_elbow"]],
		"mid_lower_arm_left" => [[0.5, "left_elbow"], [0.5, "left_wrist"]],
		"mid_lower_arm_right" => [[0.5, "right_elbow"], [0.5, "right_wrist"]],
		"mid_upper_leg_left" => [[0.5, "left_hip"], [0.5, "left_knee"]],
		"mid_upper_leg_right" => [[0.5, "right_hip"], [0.5, "right_knee"]],
		"mid_lower_leg_left" => [[0.5, "left_knee"], [0.5, "left_ankle"]],
		"mid_lower_leg_right" => [[0.5, "right_knee"], [0.5, "right_ankle"]],
	})

$p2xf_mmd_to_flic_renamed = Pose2DTransform.new("p2xf_mmd_to_flic_renamed",
	"data/pose_2d_xforms/mmd_to_flic_renamed",
	:bone_xforms => {		
		'leg_right' => [[1, "leg_right"]],
		'leg_left' => [[1, "leg_left"]],		
		'wrist_right' => [[1, "wrist_right"]],
		'elbow_right' => [[1, "elbow_right"]],
		'arm_right' => [[1, "arm_right"]],
		'arm_left' => [[1, "arm_left"]],
		'elbow_left' => [[1, "elbow_left"]],
		'wrist_left' => [[1, "wrist_left"]],
		"nose" => [[1, "nose_tip"]],
		"eye_left" => [[1, "nose_root"]],
		"eye_right" => [[1, "nose_root"]],
	})

$p2xf_mmd_to_lsp_renamed = Pose2DTransform.new("p2xf_mmd_to_lsp_renamed",
	"data/pose_2d_xforms/mmd_to_lsp_renamed",
	:bone_xforms => {
		'ankle_right' => [[1, "ankle_right"]],
		'knee_right' => [[1, "knee_right"]],
		'leg_right' => [[1, "leg_right"]],
		'leg_left' => [[1, "leg_left"]],
		'knee_left' => [[1, "knee_left"]],
		'ankle_left' => [[1, "ankle_left"]],
		'wrist_right' => [[1, "wrist_right"]],
		'elbow_right' => [[1, "elbow_right"]],
		'arm_right' => [[1, "arm_right"]],
		'arm_left' => [[1, "arm_left"]],
		'elbow_left' => [[1, "elbow_left"]],
		'wrist_left' => [[1, "wrist_left"]],
		"neck" => [[1, "neck"]],
		'head_top' => [[1, "nose_root"]],
	})

p2xf_tasks = [
	$p2xf_project_mmd_full,
	$p2xf_project_mmd_18,	
	$p2xf_project_mmd_21,	
	$p2xf_project_mmd_21_midlimbs,	
	$p2xf_mmd_18_add_midlimbs,
	$p2xf_mmd_21_add_midlimbs,
	$p2xf_mmd_21_add_weights,
	$p2xf_project_flic,
	$p2xf_project_lsp,
	$p2xf_project_lsp_renamed,
	$p2xf_rename_flic,
	$p2xf_rename_lsp,
	$p2xf_rename_lsp_add_midlimbs,
	$p2xf_mmd_to_flic_renamed,
	$p2xf_mmd_to_lsp_renamed,
	$p2xf_rename_lsp_add_hip,
	$p2xf_project_lsp_add_hip
]

task_from_tasks("pose_2d_xforms", p2xf_tasks.map {|x| x.name}, [
	"config"
	].product(["", "_clean"]).map {|x| "#{x[0]}#{x[1]}"})

