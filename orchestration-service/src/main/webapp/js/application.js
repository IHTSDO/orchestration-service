$(document).ready(function() {
	console.log("Document ready");
	var conceptRowTemplate = Handlebars.compile($("#concept-template").html());

	$(".select-all").click(function() {
		var $button = $(this);
		var $checkboxes = $(":checkbox", $button.parent().parent());
		if ($button.data("all") != "yes") {
			$button.data("all", "yes");
			$checkboxes.not(":checked").click();
			$button.text("Deselect all");
		} else {
			$button.data("all", "no");
			$checkboxes.filter(":checked").click();
			$button.text("Select all");
		}
	});

	$.ajax({
		dataType: "json",
		url: "mock_response/backlog.json",// TODO: Remove mock REST endpoint once UI developed
		//url: "REST/backlog",
		success: function(backlogConcepts) {
			var $backlogDiv = $("#backlog");
			for (var a = 0; a < backlogConcepts.length; a++) {
				var backlogConcept = backlogConcepts[a];
				var conceptRow = conceptRowTemplate(backlogConcept);
				$backlogDiv.append(conceptRow);
			}
			console.log("Done, backlogConcepts = " + backlogConcepts.length);
		}
	});

});