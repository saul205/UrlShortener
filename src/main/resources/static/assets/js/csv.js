//dropArea = document.getElementById("files") || null;
//if (dropArea !== null) dropArea.addEventListener('drop', null, false);
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
    if(file) connect()
    original()
  });
});

function download(filename, text) {
  var element = document.createElement('a');
  element.setAttribute('href', 'data:application/csv;charset=utf-8,' + encodeURIComponent(text));
  element.setAttribute('download', filename);  
  element.style.display = 'none';
  document.body.appendChild(element);
  element.click();
  document.body.removeChild(element);
}

function handleClick() {
  var type = document.getElementById("choosefile")
  if(type.innerHTML.localeCompare("Upload file") != 0) {
    var input = document.getElementById("files")
    if(input.files.item(0).name.endsWith(".csv")) {
      document.getElementById("dropfile").innerHTML = input.files.item(0).name
      file = input.files.item(0)
      change()
    } else {
      alert("Tipo de archivo no permitido (diferente a .csv)")
    }
  }
}

function change() {
  document.getElementById("orfile").innerHTML = ""
  document.getElementById("choosefile").innerHTML = "Upload file"
  document.getElementById("choosefile").style.zIndex = "10000"
  document.getElementById("choosefile").style.position = "relative"
  document.getElementById("choosefile").style.cursor = "pointer"
}

function original() {
  document.getElementById("orfile").innerHTML = "or"
  document.getElementById("choosefile").innerHTML = "Choose file"
  document.getElementById("dropfile").innerHTML = "Drop your .csv file"
  document.getElementById("choosefile").style.zIndex = ""
  document.getElementById("choosefile").style.position = ""
  document.getElementById("choosefile").style.cursor = ""
}

function connect() {
  ws = new WebSocket('ws://localhost:8080/csvws')
	ws.onmessage = function(data) {
    if(data.data == "Connected") {
      sendData()
    } else {
      if(data.data == "") {
        alert("Fichero vacío")
      } else if(data.data == "INVALID") {
        alert("Fichero con todas las URLs inválidas")
      } else {
        download(file.name.substring(0, file.name.length - 4) + "-short.csv", data.data)
      }
      disconnect()
    }
  }
}

function disconnect() {
	if(ws != null) {
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
