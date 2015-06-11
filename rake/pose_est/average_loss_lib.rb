require 'rubygems'
require 'bundler/setup'
require 'fileutils'
require 'json'
require 'rake'
require File.expand_path(File.dirname(__FILE__) + "/../lib.rb") 
require File.expand_path(File.dirname(__FILE__) + "/../java_lib.rb")
require File.expand_path(File.dirname(__FILE__) + "/../args.rb")
require File.expand_path(File.dirname(__FILE__) + "/pose_2d_configs.rb")

class AverageLossTasks < FileProcessingTasks
	def initialize(_name, _dir_name, _options={})
		_options = {
			:prediction_file => nil,
			:answer_file => nil,			
			:image_width => 0,
			:image_height => 0,
			:bone_weights => {},
			:pose_2d_config => nil
		}.merge(_options)
		super(_name, _dir_name, _options)
	end

	def gen_tasks
		average_loss_tasks
	end

	def point_distance(p, q)
		
	end	

	no_index_file_tasks(:average_loss, Proc.new {"#{dir_name}/average_loss.txt"}) do
		file average_loss_file_name => [options[:prediction_file], options[:answer_file]] do
			predictions = JSON.parse(File.read(options[:prediction_file]))
			answers     = JSON.parse(File.read(options[:answer_file]))
			pose_config = options[:pose_2d_config]			

			sum = 0.0
			answer_index = 0
			predictions.count.times do |prediction_index|
				prediction = predictions[prediction_index]
				while answer_index < answers.count && !(answers[answer_index]["file_name"] == prediction["file_name"]) do
					answer_index += 1
				end
				if answer_index > answers.count
					break
				end
				prediction = prediction["points"]
				answer = answers[answer_index]["points"]

				pose_config.bone_names.each do |bone_name|
					if answer.include?(bone_name)
						actual = prediction[bone_name]
						expected = answer[bone_name]						
						if !expected.nil? && !actual.nil?
							p = actual
							q = expected
							dx = (p[0] - q[0]).to_f / options[:image_width]
							dy = (p[1] - q[1]).to_f / options[:image_height]														
							sum += options[:bone_weights][bone_name]*(dx*dx + dy*dy) / 2.0
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
end
