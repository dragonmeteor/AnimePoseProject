require 'sinatra'
require 'erb'

set :port, ARGV[0].to_i
set :public_folder, ARGV[2]

template_content = <<-TEMPLATE
<html>
	<head>
		<title>Training and Validation Loss of <%= task_name %></title>
	</head>
	<body>
		<h1>Training and Validation Loss for <%= task_name %></h1>

		<h2>Training Loss</h2>
		<img src="train_loss.png"/>

		<h2>Validation Loss</h2>
		<img src="val_loss.png"/>
	</body>
</html>
TEMPLATE

get '/' do
	task_name = ARGV[1]
	port = ARGV[0].to_i
	dir_name = ARGV[2]

	system("rake #{task_name}:train_loss_clean")
	system("rake #{task_name}:train_loss_graph")
	system("rake #{task_name}:val_loss_clean")
	system("rake #{task_name}:val_loss_graph")

	template = ERB.new(template_content)	
	file_content = template.result(binding)
	file_content
end