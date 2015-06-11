require 'rubygems'
require 'bundler/setup'
require 'json'
require 'fileutils'

$args_dir = "data/args"

FileUtils.mkdir_p($args_dir)

def gen_args_file_name
	"#{$args_dir}/args_#{Time.now.strftime("%Y%m%d%H%M%S%N")}.txt"
end

def args_file(json_obj)
	content = JSON.pretty_generate(json_obj)
	filename = gen_args_file_name
	File.open(filename, "w") { |fout| fout.write(content) }
	filename
end

def read_args_file(filename)
	JSON.parse(File.read(filename))
end

def read_json_file(filename)
	JSON.parse(File.read(filename))
end