require 'rubygems'
require 'bundler/setup'
require 'erb'
require 'json'
require File.expand_path(File.dirname(__FILE__) + "/../lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../args.rb") 
require File.expand_path(File.dirname(__FILE__) + "/simple_pose_regressor_lib.rb") 

class CaffeWeightedPoseRegressor < CaffePoseRegressor
	def initialize(_name, _dir_name, _options)
		_options = {
			:train_weight_lmdb_file => "",			
			:val_weight_lmdb_file => "",			
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def write_train_val_file
		if options[:network_type] == :alexnet
			template_file_name = "rake/caffe/weighted_pose_regressor_alexnet_train_val.erb"
		elsif options[:network_type] == :googlenet
			template_file_name = "rake/caffe/weighted_pose_regressor_googlenet_train_val.erb"
		else
			throw RuntimeError.new("not implemented yet")
		end
		template_content = File.read(template_file_name)
		template = ERB.new(template_content)
		file_content = template.result(binding)
		File.open(train_val_file_name, "wt") { |f| f.write(file_content) }
	end

	def write_deploy_file
		if options[:network_type] == :alexnet
			template_file_name = "rake/caffe/weighted_pose_regressor_alexnet_deploy.erb"
		elsif options[:network_type] == :googlenet
			template_file_name = "rake/caffe/weighted_pose_regressor_googlenet_deploy.erb"
		else
			throw RuntimeError.new("not implemented yet")			
		end
		template_content = File.read(template_file_name)
		template = ERB.new(template_content)
		file_content = template.result(binding)
		File.open(deploy_file_name, "wt") { |f| f.write(file_content) }
	end
end

class CaffeWeightedPoseRegressorTryLearningRates < FileProcessingTasks
	attr_reader :trials

	def initialize(_name, _dir_name, _options)
		_options = {
			:image_mean_file => "",

			:train_image_lmdb_file => "",			
			:train_pose_lmdb_file => "",
			:train_batch_size => 16,

			:val_image_lmdb_file => "",
			:val_pos_lmdb_file => "",
			:val_batch_size => 16,

			:network_type => :alexnet,

			:test_iter => 10,
			:test_interval => 1000,			
			:gamma => 0.1,
			:stepsize => 200000,
			:max_iter => 1000000,
			:momentum => 0.9,
			:solver_type => "SGD",
			:snapshot => 10000,
			:weight_decay => 0.0005,

			:bone_count => 18,

			:caffe_root => "/opt/caffe",

			:web_service_port => 9393,

			:base_lr => [1e-9, 3e-9, 1e-8, 3e-8, 1e-7, 3e-7, 1e-6, 3e-6, 1e-5, 3e-5, 1e-4, 1e-3, 1e-2, 1e-1, 1],
		}.merge(_options)

		@trials = []
		_options[:base_lr].count.times do |i|
			__options = {}.merge(_options)
			__options[:base_lr] = _options[:base_lr][i]
			@trials << CaffeWeightedPoseRegressor.new("#{_name}_#{i}", "#{_dir_name}/#{i}", __options)
		end

		super(_name, _dir_name, _options)
	end

	def gen_tasks
		task_from_tasks(name, 
			trials.map {|x| x.name}, 
			["caffemodel", "solverstate", "log", 
				"train_loss", "val_loss", 
				"train_loss_graph", "val_loss_graph", "train_val_loss_graph"].product(["", "_clean"]).map {|x,y| "#{x}#{y}"})
	end

	def_index :trial do
		options[:base_lr].length
	end
end
