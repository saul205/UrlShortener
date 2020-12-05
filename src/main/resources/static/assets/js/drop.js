dropArea = document.getElementById("files") || null;
if (dropArea !== null) dropArea.addEventListener('drop', changeType, false);
var file = null;
var ws;

$(document).ready( function(){
  $("#choosefile").click( function(event){
      var form = new FormData();
      connect()
      form.append('file', file)
      event.preventDefault()
      $.ajax({
          type: "POST",
          url: "/csv",
          data: form,
          contentType: false,
          cache: false,
          processData: false,
          success: function (msg) { 
            csvData = 'data:application/csv;charset=utf-8,' + encodeURIComponent(msg);
            var link = document.createElement("a");
            link.setAttribute("href", csvData);
            link.setAttribute("download", file.name.substring(0, file.name.length - 4) + "-short.csv");
            document.body.appendChild(link);
            link.click();
          },
          error: function (msg) { console.log("SEND CSV FAIL"); }
      });
  });
});

function download(msg) {
  console.log(file.name);
  var link = document.createElement('a');
  link.href = window.URL.createObjectURL(msg);
  link.download = file.name;
  link.click();
}

function handleClick() {
  var type = document.getElementById("choosefile");
  if(type.innerHTML.localeCompare("Upload file") != 0) {
    var input = document.getElementById("files");
    document.getElementById("dropfile").innerHTML = input.files.item(0).name;
    file = input.files.item(0)
    change();
  }
}

function changeType(e) {
  var dt = event.dataTransfer
  var files = dt.files
  document.getElementById("dropfile").innerHTML = files[0].name
  file = files[0]
  change()
}

function change() {
  document.getElementById("orfile").innerHTML = "";
  document.getElementById("choosefile").innerHTML = "Upload file";
  document.getElementById("choosefile").style.zIndex = "10000";
  document.getElementById("choosefile").style.position = "relative";
  document.getElementById("choosefile").style.cursor = "pointer";
}

function connect() {
	ws = new WebSocket('ws://localhost:8080/csvws')
	ws.onmessage = function(data) {
		console.log(data.data)
	}
}

function disconnect() {
	if (ws != null) {
		ws.close()
	}
	console.log("Websocket is in disconnected state")
}

function sendData() {
	var data = JSON.stringify({
		'user' : $("#user").val()
	})
	ws.send(data)
}
