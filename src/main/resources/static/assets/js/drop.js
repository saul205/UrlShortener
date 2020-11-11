dropArea = document.getElementById("files");
dropArea.addEventListener('drop', changeType, false);

function handleClick() {
  var type = document.getElementById("choosefile");
  if(type.innerHTML.localeCompare("Upload file") == 0) {
      //crea peticion
  } else {
      var input = document.getElementById("files");
      document.getElementById("dropfile").innerHTML = input.files.item(0).name;
      change();
  }

}

function changeType(e) {
  var dt = event.dataTransfer
  var files = dt.files
  document.getElementById("dropfile").innerHTML = files[0].name
  change()
}

function change() {
  document.getElementById("orfile").innerHTML = "";
  document.getElementById("choosefile").innerHTML = "Upload file";
  document.getElementById("choosefile").style.zIndex = "10000";
  document.getElementById("choosefile").style.position = "relative";
  document.getElementById("choosefile").style.cursor = "pointer";
}