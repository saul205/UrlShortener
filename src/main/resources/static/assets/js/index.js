$(document).ready( function(){
    $("#top-list").ready(function(){stats()});
    $("#buttonSendUrl").click( function(event){
        event.preventDefault();
        var check = document.getElementById("checkbox").checked
        $.ajax({
            type: "POST",
            url: "/link",
            data: $("#SendUrl").serialize().concat("&qr=" + check),
            success: function (msg,err) { resLink(msg, err); },
            error: function (msg,err) { resLink(msg, err); }
        });
    });
    $("#search").submit(search);
});

function resLink(msg, st){
    if (st === "success"){
        var link = window.location.origin + "/sh.html?id=" + msg.hash
        $("#result").html(
            "<a target='_blank' href='"
            + link + "'>" + link + "</a>"
        );
    } else
    if (st === "timeout" || st ===  "error" | st === "abort" || st === "parsererror"){
        $("#result").html(
            "<a href=''> - ERROR -</a>"
        );
    }
}

function fechafuera(date){
    let str = ""+date;
    return str.slice(0,16)
}

const truncateText = (text) => {
  var truncated = text;
  if (text.length > 30) {
      truncated = truncated.substr(0,30) + '...';
  }
  return truncated;
}

function stats(){
    $.ajax({
        type: "GET",
        url: "/actuator/data",
        data: $(this).serialize(),
        success: function (msg) {
            topl(msg.top);
            $("#urls-int").html(msg.urls);
            $("#clicks-int").html(msg.clicks);
            $("#history-list").html(historial(msg.historial));
        },
        error: function () {
            $("#urls-int").html("err");
            $("#clicks-int").html("err");
        }
    });
}
function topl(urls){
    let list = "";
    console.log(urls)
    for(var element in urls){
        console.log(element)
        let str = "<li value=" + element + "> \(" + urls[element].count + " uses \) ";
        let a = "<a href="+urls[element].target+">"+truncateText(urls[element].target)+"</a>";
        list = list + str + a + "</li>"
    }

    $("#top-list").html(list);
}

function historial(urls){
    let list = "";
    console.log(urls)
    for(var element in urls){
        console.log(element)
        let str = "<li value=" + element + ">" + urls[element].target + "<br>";
        let a = "<a href="+window.location.href + "/" + urls[element].hash+">"+truncateText(window.location.href + "/" + urls[element].hash)+"</a><br>";
        list = list + str + a +  urls[element].created + "</li>"
    }

    $("#history-list").html(list);
}

function search(event){
    event.preventDefault();
    console.log($("#searchField").val())
    $.ajax({
        type: "POST",
        url: "/actuator/data",
        dataType : "json",
        data: "{\"target\": \"" + $("#searchField").val()+"\"}",
        headers: {"Content-Type": "application/json"},
        success: function(result){
            $("#searchResult").html(
                "<div>"
                + result.target
                + "<br>"
                + result.count
                + "</div>"
        )},
        error: function () {
            $("#searchResult").html(
                "<div>"
                + result.target
                + "<br>"
                + result.count
                + "</div>"
        )
        }
    });
}