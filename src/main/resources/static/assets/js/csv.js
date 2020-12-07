dropArea = document.getElementById("files") || null;
if (dropArea !== null) dropArea.addEventListener('drop', changeType, false);
var file = null;
var ws;
var ip = "";

function text(url) {
  return fetch(url).then(res => res.text());
}

$(document).ready( function(){
  text('https://www.cloudflare.com/cdn-cgi/trace').then(data => {
    let ipRegex = /[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}.[0-9]{1,3}/
    ip = data.match(ipRegex)[0];
  });
  $("#choosefile").click( function(event){
      //var form = new FormData()
      connect()
      //form.append('file', file)
      //event.preventDefault()
      /*$.ajax({
          type: "POST",
          url: "/csv",
          data: form,
          contentType: false,
          cache: false,
          processData: false,
          success: function (msg) { 
            download(file.name.substring(0, file.name.length - 4) + "-short.csv", msg)
            //csvData = 'data:application/csv;charset=utf-8,' + encodeURIComponent(msg);
            //var link = document.createElement("a");
            //link.setAttribute("href", csvData);
            //link.setAttribute("download", file.name.substring(0, file.name.length - 4) + "-short.csv");
            //document.body.appendChild(link);
            //link.click();
            //document.body.removeChild(link)
          },
          error: function (msg) { console.log("SEND CSV FAIL"); }
      });*/
  });
});

function download(filename, text)  {
  var element = document.createElement('a');
  element.setAttribute('href', 'data:application/csv;charset=utf-8,' + encodeURIComponent(text));
  element.setAttribute('download', filename);  
  element.style.display = 'none';
  document.body.appendChild(element);
  element.click();
  document.body.removeChild(element);
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
    if(data.data == "Connected") {
      sendData()
    } else {
      ws.send("END")
      disconnect()
      download(file.name.substring(0, file.name.length - 4) + "-short.csv", data.data)
    }
  }
}

function disconnect() {
	if (ws != null) {
		ws.close()
	}
}

function sendData() {
  var reader = new FileReader();
  reader.readAsText(file, "UTF-8");
  reader.onload = function (evt) {
    ws.send(evt.target.result + ip)
  }
}
