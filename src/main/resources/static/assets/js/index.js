$(document).ready( function(){
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
    $("#search").submit(function(e){search(e)});
    $("#top-list").ready(function(){stats()});
    $("#history-list").ready(function(){
        $.ajax({
            type: "GET",
            url: "/db/history",
            data: null,
            success: function (msg,err) { historial(msg, err); },
            error: function (msg,err) { historial(msg, err); }
        });
    });
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

function historial(msg, st){
    if (st === "success"){
        if (Object.keys(msg).length === 0){
            st = "abort"
        } else {
            let list = "";
            for(var key in msg){
                for(var key2 in msg[key]){
                    let str = "<li value="+key+"> \(" + fechafuera(msg[key][key2]) + "\) ";
                    let a = "<a href="+key2+">"+truncateText(key2)+"</a>";
                    list = list + str + a + "</li>"
                }
            }
            $("#history-list").html(list);
        }
    }
    if (st === "timeout" || st ===  "error" | st === "abort" || st === "parsererror"){
        $("#history").parent().replaceWith("");
    }
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
        url: "/db",
        data: $(this).serialize(),
        success: function (msg) {
            topl(msg.top);
            $("#urls-int").html(msg.urls);
            $("#clicks-int").html(msg.clicks);
        },
        error: function () {
            $("#urls-int").html("err");
            $("#clicks-int").html("err");
        }
    });
}
function topl(urls){
    let list = "";
    for(var key in urls){
        for(var key2 in urls[key]){
            let str = "<li value=" + key + "> \(" + urls[key][key2] + " uses \) ";
            let a = "<a href="+key2+">"+truncateText(key2)+"</a>";
            list = list + str + a + "</li>"
        }
    }
    $("#top-list").html(list);
}

function search(event){
    event.preventDefault();
    $.post(
        "/db/search",
        {url: $("#searchField").val()},
        function(result){
            $("#searchResult").html(
                "<div>"
                + result.target
                + "<br>"
                + result.count
                + "</div>"
            );
        }
    )
}