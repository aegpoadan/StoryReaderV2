<html>
	<head style="position: absolute; height: 10%; width: 100%;">
		<link rel='stylesheet' href='./css/normalize.css' type='text/css'>
		<link rel='stylesheet' href='./css/skeleton.css' type='text/css'>
	</head>
	<body style="position: absolute; left: 10%; top: 10%;">
		<div class="container" style="min-width:400px;">
			<div id="header" class="row" style="border: 1px solid black; width: 100%; padding-right: 100px;">
				Andrew Hayes<br>
				CSE 383<br>
				Story App V2
			</div>
			<div id="content" class="row" style="width: 100%; left: 25%;">
				<p style="color: red">${sessionState.errorMessage}</p>
				<form style="position: relative; top: 10%;"method="post">
				<#list stories as story>
					Story: ${story.name} <input type="radio" name="story" value="${story.name}"><br>
				</#list><br><br>
					Username:<br><input type="text" name="username"><br>
					Password:<br><input type="text" name="password"><br>
					Email:<br><input type="text" name="email"><br>
					<input type="submit" value="Submit">
				</form>
			</div>
			<div id="footer" class="row" style="border: 1px solid black; width: 100%; padding-right: 100px;">
				Andrew Hayes
			</div>
		</div>
	</body>
</html>