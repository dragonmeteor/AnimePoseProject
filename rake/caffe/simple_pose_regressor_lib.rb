require 'rubygems'
require 'bundler/setup'
require 'erb'
require 'json'
require File.expand_path(File.dirname(__FILE__) + "/../lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../args.rb") 

class CaffePoseRegressor < FileProcessingTasks
	def initialize(_name, _dir_name, _options)
		_options = {
			:caffe_root => "/opt/caffe",
			:web_service_port => 9393,

			:image_mean_file => "",

			:train_image_lmdb_file => "",			
			:train_pose_lmdb_file => "",
			:train_batch_size => 16,

			:val_image_lmdb_file => "",
			:val_pose_lmdb_file => "",
			:val_batch_size => 16,

			:inner_product_layer_std => 0.001,
			:network_type => :alexnet,

			:bone_count => 18,
			
			:finetune_from => nil,			

			:test_iter => 10,
			:test_interval => 1000,
			:snapshot => 10000,			
			:max_iter => 1000000,

			:solver_type => "SGD",			

			# SGD
			:base_lr => 1e-8,
			:momentum => 0.9,
			:weight_decay => 0.0005,
				
			:lr_policy => "step",

			# step lr_policy
			:gamma => 0.1,
			:stepsize => 200000,

			# poly lr_policy
			:power => 0.5,			
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def num_snapshot
		options[:max_iter] / options[:snapshot]
	end

	def gen_tasks
		deploy_tasks
		solverstate_tasks
		caffemodel_tasks
		log_tasks
		train_loss_tasks
		val_loss_tasks
		train_loss_graph_tasks
		val_loss_graph_tasks
		train_val_loss_graph_tasks
		loss_web_tasks
	end

	def train_val_file_name
		"#{dir_name}/train_val.prototxt"
	end

	def write_train_val_file
		raise RuntimeError.new("not implemented yet!")		
	end	

	def solver_file_name
		"#{dir_name}/solver.prototxt"		
	end	

	def write_solver_file(snapshot_index)
		template_content = <<-TEMPLATE
net: "<%= train_val_file_name %>"
solver_mode: GPU
max_iter: <%= (snapshot_index+1) * options[:snapshot] %>
snapshot: <%= options[:snapshot] %>
snapshot_prefix: "<%= dir_name %>/snapshot"
snapshot_after_train: false
test_iter: <%= options[:test_iter] %>
test_interval: <%= options[:test_interval] %>
display: <%= options[:display] %>

solver_type: <%= options[:solver_type] %>
gamma: <%= options[:gamma] %>
base_lr: <%= options[:base_lr] %>
momentum: <%= options[:momentum] %>
weight_decay: <%= options[:weight_decay] %>

lr_policy: "<%= options[:lr_policy] %>"
stepsize: <%= options[:stepsize] %>
power: <%= options[:power] %>
TEMPLATE

		template = ERB.new(template_content)
		file_content = template.result(binding)
		File.open(solver_file_name, "wt") { |f| f.write(file_content) }  
	end

	def write_defloy_file
		raise RuntimeError.new("not implemented yet!")		
	end
	
	no_index_file_tasks(:deploy, Proc.new {"#{dir_name}/deploy.prototxt"}) do
		file deploy_file_name do
			write_deploy_file			
		end
	end

	def_index :snapshot do
		num_snapshot
	end	

	def train_snapshot(index)
		log_file = log_file_name(index)
		if index == 0
			if options[:finetune_from].nil?
				command = "#{options[:caffe_root]}/build/tools/caffe train --solver=#{solver_file_name} 2>&1 | tee #{log_file}"		
			else
				command = "#{options[:caffe_root]}/build/tools/caffe train --solver=#{solver_file_name} --weights=#{options[:finetune_from]} 2>&1 | tee #{log_file}"		
			end
		else 
			command = "#{options[:caffe_root]}/build/tools/caffe train --solver=#{solver_file_name} " +
				"--snapshot=#{solverstate_file_name(index-1)} " +
			   "2>&1 | tee #{log_file}"
		end
		write_solver_file(index)
		write_train_val_file
		run(command)
	end

	one_index_file_tasks(:solverstate, :snapshot, Proc.new { |index|
		"#{dir_name}/snapshot_iter_#{options[:snapshot]*(index+1)}.solverstate"
	}) do |index|
		solverstate_file = solverstate_file_name(index)
		if index == 0
			dep = []
		else
			dep = [solverstate_file_name(index-1), caffemodel_file_name(index-1)]
		end
		file solverstate_file => dep do
			train_snapshot(index)
		end
	end

	one_index_file_tasks(:caffemodel, :snapshot, Proc.new { |index|
		"#{dir_name}/snapshot_iter_#{options[:snapshot]*(index+1)}.caffemodel"
	}) do |index|
		caffemodel_file = caffemodel_file_name(index)
		if index == 0
			dep = []
		else
			dep = [solverstate_file_name(index-1), caffemodel_file_name(index-1)]
		end
		file caffemodel_file => dep do
			train_snapshot(index)
		end
	end

	one_index_file_tasks(:log, :snapshot, Proc.new { |index| 
		"#{dir_name}/log_iter_#{options[:snapshot]*(index+1)}.txt"
	}) do |index|
		log_file = log_file_name(index)
		if index == 0
			dep = []
		else
			dep = [solverstate_file_name(index-1), caffemodel_file_name(index-1)]
		end
		file log_file => dep do
			train_snapshot(index)
		end
	end

	def compute_train_and_val_loss
		args = {
			"dir_name" => dir_name,
			"snapshot_iter" => options[:snapshot],
			"snapshot_count" => snapshot_count,
			"train_loss_file_name" => train_loss_file_name,
			"val_loss_file_name" => val_loss_file_name
		}
		command = "python script/caffe/parse_caffe_log_to_train_and_val_loss.py #{args_file(args)}"		
		run(command)
	end

	no_index_file_tasks(:train_loss, Proc.new { "#{dir_name}/train_loss.txt" }) do
		file train_loss_file_name do
			compute_train_and_val_loss
		end
	end

	no_index_file_tasks(:val_loss, Proc.new {"#{dir_name}/val_loss.txt"}) do
		file val_loss_file_name do
			compute_train_and_val_loss
		end
	end

	no_index_file_tasks(:train_loss_graph, Proc.new {"#{dir_name}/train_loss.png"}) do
		file train_loss_graph_file_name => [train_loss_file_name] do
			data = JSON.parse(File.read(train_loss_file_name))
			data = data[10..-1]
			args = {
				"title" => "Training Loss",
				"x_label" => "iteration index",
				"y_label" => "loss",
				"data" => [{
					"x" => data.map {|x| x[0]},
					"y" => data.map {|y| y[3]},
					"label" => "training loss"
				}],
				"output_file" => train_loss_graph_file_name
			}
			command = "python script/plot_line_graph.py #{args_file(args)}"
			run(command)
		end
	end

	no_index_file_tasks(:val_loss_graph, Proc.new {"#{dir_name}/val_loss.png"}) do
		file val_loss_graph_file_name => [val_loss_file_name] do
			data = JSON.parse(File.read(val_loss_file_name))
			data = data[2..-1]
			args = {
				"title" => "Validation Loss",
				"x_label" => "iteration index",
				"y_label" => "loss",
				"data" => [{
					"x" => data.map {|x| x[0]},
					"y" => data.map {|y| y[3]},
					"label" => "validation loss"
				}],
				"output_file" => val_loss_graph_file_name
			}
			command = "python script/plot_line_graph.py #{args_file(args)}"
			run(command)
		end
	end

	no_index_file_tasks(:train_val_loss_graph, Proc.new {"#{dir_name}/train_vol_loss.png"}) do
		file train_val_loss_graph_file_name => [train_loss_file_name, val_loss_file_name] do
			train_data = JSON.parse(File.read(train_loss_file_name))
			if train_data.length > 10			
				train_data = train_data[10..-1]
			end
			val_data = JSON.parse(File.read(val_loss_file_name))
			if val_data.length > 2
				val_data = val_data[2..-1]
			end
			args = {
				"title" => "Training & Validation Loss",
				"x_label" => "iteration index",
				"y_label" => "loss",
				"data" => [
					{
						"x" => train_data.map {|x| x[0]},
						"y" => train_data.map {|y| y[3]},
						"label" => "training loss"
					},
					{
						"x" => val_data.map {|x| x[0]},
						"y" => val_data.map {|y| y[3]},
						"label" => "validation loss"
					},
				],
				"output_file" => train_val_loss_graph_file_name
			}
			command = "python script/plot_line_graph.py #{args_file(args)}"
			run(command)
		end
	end

	def loss_web_tasks
		namespace name do
			task :run_loss_web do
				run("ruby rake/caffe/loss_web.rb #{options[:web_service_port]} #{name} #{dir_name}")
			end
		end
	end
end

class CaffeSimplePoseRegressor < CaffePoseRegressor
	def initialize(_name, _dir_name, _options)
		super(_name, _dir_name, _options)
	end

	def write_train_val_file
		if options[:network_type] == :alexnet
			template_file_name = "rake/caffe/simple_pose_regressor_alexnet_train_val.erb"
		elsif options[:network_type] == :googlenet
			template_file_name = "rake/caffe/simple_pose_regressor_googlenet_train_val.erb"
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
			template_file_name = "rake/caffe/simple_pose_regressor_alexnet_deploy.erb"
		elsif options[:network_type] == :googlenet
			template_file_name = "rake/caffe/simple_pose_regressor_googlenet_deploy.erb"
		else
			throw RuntimeError.new("not implemented yet")			
		end
		template_content = File.read(template_file_name)
		template = ERB.new(template_content)
		file_content = template.result(binding)
		File.open(deploy_file_name, "wt") { |f| f.write(file_content) }
	end
end

class CaffeSimpleRegressorTryLearningRates < FileProcessingTasks
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
			@trials << CaffeSimplePoseRegressor.new("#{_name}_#{i}", "#{_dir_name}/#{i}", __options)
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
