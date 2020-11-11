$(document).ready( function(){
    if(window.location.pathname === "/sh.html"){
       $.ajax({
            type: "GET",
            url: "/stid",
            data: window.location.search.slice(1),
            success: function (msg,err) { resSTID(msg, err); },
            error: function (msg,err) { resSTID(msg, err); }
        });
    }
    $("#qr").click( function(event){
        event.preventDefault();
        $('#qr').attr('src', "/qr?hashUS=" + window.location.search.slice(4))
    });
    $("#redir").click( function(event){
        event.preventDefault();
        window.location.href = window.location.origin + '/' + window.location.search.slice(4);
    });
});

function resSTID(msg, st){
    console.log(st);
    if (st === "success"){
        console.log(msg)
        if(!msg.segura) $("#JAKE").addClass("bad");
        if(msg.alcanzable === 0){
            $("#rble-i").css( "color", "#444444" );
            $("#rble-s").html("URL availability unknown");
        } else if(msg.alcanzable !== 1) {
            $("#rble-i").css( "color", "#8e3838" );
            $("#rble-s").html("URL not available");
        }
    } else
    if (st === "timeout" || st ===  "error" | st === "abort" || st === "parsererror"){
        window.location.replace(window.location.origin);
    }
}
