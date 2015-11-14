<html>
	<head style="position: absolute; height: 10%; width: 100%;">
		<link rel='stylesheet' href='./css/normalize.css' type='text/css'>
		<link rel='stylesheet' href='./css/skeleton.css' type='text/css'>
		<script>
			function getParameterByName(name) {
    			name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    			var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
        		results = regex.exec(location.search);
    			return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
			}
			var p = parseInt(getParameterByName("p"));
		</script>
	</head>
	<body style="position: absolute; left: 10%; top: 10%;">
		<div class="container" style="min-width: 400px;">
			<div id="header" class="row" style="border: 1px solid black; width: 100%; padding-right: 100px;">
				${sessionState.username}<br>
				${sessionState.email}<br>
				Story App V1
			</div>

			<div id="content" class="row">
				<div class="four columns">
					<#if sessionState.page gt 0>
						<button onclick="window.location.search='?story=${storyName}&p=' + --p">Prev</button><br>
					</#if>
					<#if sessionState.page lt sessionState.lastPage>
						<button onclick="window.location.search='?story=${storyName}&p=' + ++p">Next<br>
					</#if>
					<button onclick="window.location.href ='/StoryReader/select'">Home
				</div>
				<div class="eight columns">
					${pageContent}
				</div>
			</div>

			<div id="footer" class="row" style="border: 1px solid black; width: 100%;padding-right: 100px;">
				Andrew Hayes
			</div>
		</div>
	</body>
</html>