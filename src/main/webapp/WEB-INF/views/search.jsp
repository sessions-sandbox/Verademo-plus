<%@ page language="java" contentType="text/html; charset=US-ASCII"
	pageEncoding="US-ASCII"%>
<%@ page import="com.veracode.verademo.model.*"%>
<%@ page import="java.util.*"%>

<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta http-equiv="X-UA-Compatible" content="IE=edge">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="description" content="">
<meta name="author" content="">

<title>Search Blabs</title>

<!-- Bootstrap core CSS -->
<link href="resources/css/bootstrap.min.css" rel="stylesheet">
<!-- Bootstrap theme -->
<link href="resources/css/bootstrap-theme.min.css" rel="stylesheet">

<!-- Custom styles for this template -->
<link href="resources/css/pwm.css" rel="stylesheet">

<style>
	.search-container {
		margin-top: 20px;
	}
	.search-box {
		margin-bottom: 20px;
	}
	.config-panel {
		margin-bottom: 20px;
		padding: 12px;
		border: 1px solid #ddd;
		border-radius: 4px;
		background-color: #f9f9f9;
	}
	#configStatus {
		margin-top: 8px;
	}
	.similarity-score {
		background-color: #5cb85c;
		color: white;
		padding: 2px 8px;
		border-radius: 3px;
		font-size: 12px;
		margin-left: 10px;
	}
	.no-results {
		text-align: center;
		padding: 40px;
		color: #999;
	}
	.loading {
		text-align: center;
		padding: 20px;
	}
</style>
</head>

<body role="document">

	<div class="container">

		<div class="header clearfix">
			<nav>
				<ul class="nav nav-pills pull-right">
					<li role="presentation"><a href="feed">Feed</a></li>
					<li role="presentation"><a href="blabbers">Blabbers</a></li>
					<li role="presentation"><a href="profile">Profile</a></li>
					<li role="presentation" class="active"><a href="search">Search</a></li>
					<li role="presentation"><a href="tools">Tools</a></li>
					<li role="presentation"><a href="logout">Logout</a></li>
				</ul>
			</nav>
			<img src="resources/images/Tokyoship_Talk_icon.svg" height="100"
				width="100">
		</div>

	</div>

	<div class="container theme-showcase" role="main">

		<div class="page-header">
			<h3>Fuzzy Search Blabs</h3>
			<h4>Search through jokes and blabs with intelligent fuzzy matching</h4>
			<p class="text-muted">
				Current algorithm: <strong id="currentAlgorithmLabel">${currentAlgorithm}</strong>
			</p>
		</div>

		<div class="search-container">
			<div class="config-panel">
				<div class="form-inline" role="form">
					<div class="form-group">
						<input id="yamlConfigFile" type="file" accept=".yaml,.yml,text/yaml,application/x-yaml" class="form-control" />
					</div>
					<div class="form-group">
						<button id="uploadYamlConfig" type="button" class="btn btn-default">Upload YAML Config</button>
					</div>
				</div>
				<div id="configStatus"></div>
			</div>

			<div class="search-box">
				<form id="searchForm" class="form-inline" role="form">
					<div class="form-group" style="width: 70%;">
						<input id="searchQuery" class="form-control" type="text"
							placeholder="Enter search term..." name="query" style="width: 100%;" />
					</div>
					<div class="form-group">
						<button type="submit" class="btn btn-primary">Search</button>
					</div>
				</form>
				<small class="text-muted">
					Powered by configurable fuzzy search algorithm (YAML-based configuration)
				</small>
			</div>

			<div id="searchResults">
				<!-- Results will be displayed here -->
			</div>
		</div>

	</div>
	<!-- /container -->

	<!-- Bootstrap core JavaScript
    ================================================== -->
	<!-- Placed at the end of the document so the pages load faster -->
	<script src="resources/js/jquery-1.11.2.min.js"></script>
	<script src="resources/js/bootstrap.min.js"></script>

	<!-- Fuzzy search JavaScript -->
	<script type="text/javascript">
		$(document).ready(function() {
			$('#uploadYamlConfig').click(function() {
				var fileInput = document.getElementById('yamlConfigFile');
				if (!fileInput.files || fileInput.files.length === 0) {
					showConfigStatus('Please select a YAML file first.', 'warning');
					return;
				}

				var file = fileInput.files[0];
				var fileName = file.name.toLowerCase();
				if (!fileName.endsWith('.yaml') && !fileName.endsWith('.yml')) {
					showConfigStatus('Only .yaml or .yml files are supported.', 'danger');
					return;
				}

				var reader = new FileReader();
				reader.onload = function(evt) {
					showConfigStatus('Uploading configuration...', 'info');
					$.ajax({
						url: 'search',
						method: 'POST',
						data: evt.target.result,
						contentType: 'application/x-yaml; charset=UTF-8',
						processData: false,
						dataType: 'json',
						success: function(data) {
							if (data.error) {
								showConfigStatus('Config update failed: ' + data.error, 'danger');
								return;
							}

							if (data.algorithm) {
								$('#currentAlgorithmLabel').text(data.algorithm);
							}
							showConfigStatus('Configuration updated successfully.', 'success');
						},
						error: function(xhr, status, error) {
							showConfigStatus('Config update error: ' + error, 'danger');
						}
					});
				};
				reader.onerror = function() {
					showConfigStatus('Failed to read the selected file.', 'danger');
				};
				reader.readAsText(file);
			});

			$('#searchForm').submit(function(e) {
				e.preventDefault();
				var query = $('#searchQuery').val().trim();

				if (query === '') {
					alert('Please enter a search term');
					return;
				}

				// Show loading indicator
				$('#searchResults').html('<div class="loading"><i>Searching...</i></div>');

				// Make AJAX request to fuzzy search endpoint
				$.ajax({
					url: 'search-blabs',
					method: 'GET',
					data: { query: query },
					dataType: 'json',
					success: function(data) {
						displayResults(data);
					},
					error: function(xhr, status, error) {
						$('#searchResults').html(
							'<div class="alert alert-danger">Error performing search: ' + error + '</div>'
						);
					}
				});
			});
		});

		function showConfigStatus(message, type) {
			$('#configStatus').html('<div class="alert alert-' + type + '">' + escapeHtml(message) + '</div>');
		}

		function displayResults(data) {
			var resultsHtml = '';

			if (data.error) {
				resultsHtml = '<div class="alert alert-danger">' + data.error + '</div>';
			} else if (data.count === 0) {
				resultsHtml = '<div class="no-results">' +
					'<h4>No matches found</h4>' +
					'<p>Try a different search term or check your fuzzy search settings</p>' +
					'</div>';
			} else {
				resultsHtml = '<div class="alert alert-success">' +
					'Found ' + data.count + ' match(es) for "' + escapeHtml(data.query) + '"' +
					'</div>';

				resultsHtml += '<div class="detailBox">' +
					'<div class="titleBox"><label>Search Results</label></div>' +
					'<div class="actionBox">' +
					'<ul class="commentList">';

				data.results.forEach(function(result) {
					var similarityPercent = Math.round(result.similarity * 100);
					var similarityClass = similarityPercent >= 80 ? '#5cb85c' :
										  similarityPercent >= 60 ? '#f0ad4e' : '#d9534f';

					resultsHtml += '<li>' +
						'<div>' +
						'<div class="commenterImage">' +
						'<img src="resources/images/' + result.username + '.png" />' +
						'</div>' +
						'<div class="commentText">' +
						'<p>' + escapeHtml(result.content) +
						'<span class="similarity-score" style="background-color: ' + similarityClass + '">' +
						similarityPercent + '% match</span></p>' +
						'<span class="date sub-text">by ' + escapeHtml(result.blabName) +
						' on ' + result.timestamp + '</span><br/>' +
						'<span class="date sub-text">' +
						'<a href="blab?blabid=' + result.blabid + '">' +
						result.commentCount + ' Comments</a>' +
						'</span>' +
						'</div>' +
						'</div>' +
						'</li>';
				});

				resultsHtml += '</ul></div></div>';
			}

			$('#searchResults').html(resultsHtml);
		}

		function escapeHtml(text) {
			var map = {
				'&': '&amp;',
				'<': '&lt;',
				'>': '&gt;',
				'"': '&quot;',
				"'": '&#039;'
			};
			return text.replace(/[&<>"']/g, function(m) { return map[m]; });
		}
	</script>
</body>
</html>
