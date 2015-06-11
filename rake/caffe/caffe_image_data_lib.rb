require 'rubygems'
require 'bundler/setup'
require 'erb'
require 'json'
require File.expand_path(File.dirname(__FILE__) + "/../lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../java_lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../pose_est/pose_2d_configs.rb")
require File.expand_path(File.dirname(__FILE__) + "/../args.rb")

class CaffeImageData < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:data_list_file => nil,			
			:image_width => 227,
			:image_height => 227,
			:caffe_root => "/opt/caffe",
			:random_seed => 47841321987,
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def gen_tasks		
		image_lmdb_tasks
		image_mean_tasks
		image_mean_npy_tasks
		image_mean_png_tasks
		per_channel_image_mean_npy_tasks
		per_channel_image_mean_tasks
		per_channel_image_mean_png_tasks
		file_order_tasks
	end

	no_index_file_tasks(:image_lmdb, Proc.new {"#{dir_name}/image.lmdb"}) do
		file image_lmdb_file_name => [options[:data_list_file]] do
			if !File.exists?(image_lmdb_file_name)
				args = {
					"data_list_file" => options[:data_list_file],
					"output_file" => image_lmdb_file_name,					
					"caffe_root" => options[:caffe_root]
				}
				if !options[:random_seed].nil?
					args["random_seed"] = options[:random_seed]
				end
				args_file_name = args_file(args)
				run("python script/caffe/data_list_to_image_lmdb.py #{args_file_name}")
			end
		end
	end

	def file_order_tasks
		namespace name do
			task :file_order do				
				args = {
					"data_list_file" => options[:data_list_file],					
				}			
				if !options[:random_seed].nil?
					args["random_seed"] = options[:random_seed]
				end
				args_file_name = args_file(args)
				run("python script/caffe/print_file_order.py #{args_file_name}")
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
end