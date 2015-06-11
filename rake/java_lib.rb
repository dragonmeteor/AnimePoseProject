require File.expand_path(File.dirname(__FILE__) + "/lib.rb")

def jars_of_yumyai
	jars = [
		"yumyai_jg/lib/*",
		"groovy/lib/*",
		"yumyai_jg/dist/yumyai_jg.jar",		
	]
	if platform == :windows
		jars.join(";")
	else
		jars.join(":")
	end
end

def java_command
	if OS.windows?
		cmd = "\"#{ENV["JAVA_HOME"]}\\bin\\java\" -Xmx6g"
	else
		cmd = "java -Xmx6g"
	end
end

def call_java(main_class, *args)
	cmd = "#{java_command} -server -classpath \"#{jars_of_yumyai}\" #{main_class} #{args.join(" ")}"
	run(cmd)
end

def call_java_and_get_output(main_class, *args)	
	cmd = "#{java_command} -server -classpath \"#{jars_of_yumyai}\" #{main_class} #{args.join(" ")}"
	puts (cmd)
	output = IO.popen(cmd)
	out = output.readlines
	output.close
	out
end