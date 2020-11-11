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
});

function resSTID(msg, st){
    if (st === "success"){
        console.log(msg)
        if(!msg.segura) $("#JAKE").addClass("bad");
        if(msg.alcanzable !== 1){
            $("#rble-i").css( "color", "#8e3838" );
            $("#rble-s").html("URL not available");
        }
        getQR();
    } else
    if (st === "timeout" || st ===  "error" | st === "abort" || st === "parsererror"){
        window.location.replace(window.location.origin);
    }
}

function getQR(){
    
}