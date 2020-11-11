$(document).ready( function(){
    $("#buttonSendUrl").click( function(event){
        console.log("BOTON!");
        event.preventDefault();
        $.ajax({
            type: "POST",
            url: "/link",
            data: $("#SendUrl").serialize(),
            success: function (msg,err) { resLink(msg, err); },
            error: function (msg,err) { resLink(msg, err); }
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
            "<a href='\"\"'> - ERROR -</a>"
        );
    }
}
