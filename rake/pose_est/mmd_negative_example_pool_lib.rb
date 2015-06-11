require 'rubygems'
require 'bundler/setup'
require 'fileutils'
require 'json'
require File.expand_path(File.dirname(__FILE__) + "/../lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../java_lib.rb") 

class MmdNegativeExamplePoolTasks < FileProcessingTasks
	def initialize(_name, _dir_name, _options = {})
		_options = {
			:source_file => "",
			:count => 1000,
			:image_size => 400
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def gen_tasks
		image_tasks
	end

	no_index_file_tasks(:image, Proc.new {
		"#{dir_name}/done.txt"
	}) do
		file image_file_name => [options[:source_file]] do
			call_java("yumyai.poseest.mmd.background.GenRandomSubImages", 
				options[:source_file],
				options[:image_size],
				options[:count],
				"\"#{dir_name}/img_%08d.png\"")
		end
	end
end