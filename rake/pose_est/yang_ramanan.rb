require 'rubygems'
require 'bundler/setup'
require 'fileutils'
require 'json'
require 'rake'
require File.expand_path(File.dirname(__FILE__) + "/../lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../java_lib.rb")

namespace :yang_ramanan do
	task :view_parse do
		call_java("yumyai.poseest.yang_ramanan.ViewData", 
			"data\\pose_est\\20121128-pose-release-ver1.3\\code-full\\PARSE\\parse_data.txt")
	end

	[2,3,4,5,6].each do |ep|
		task "view_buffy_ep#{ep}".to_sym do
			call_java("yumyai.poseest.yang_ramanan.ViewData", 
				"data\\pose_est\\20121128-pose-release-ver1.3\\code-full\\BUFFY\\episode_#{ep}.txt")
		end
	end
end