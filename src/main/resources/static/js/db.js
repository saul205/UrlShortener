function db() {
    $.ajax({
        type: "GET",
        url: "/db",
        data: $(this).serialize(),
        success: function (msg) {
            var a = "Top páginas buscadas:";
            for(var key in msg.top){
                for(var key2 in msg.top[key]){
                    a = a + "<br/>" + key2  + " " + msg.top[key][key2];
                }
            }
            $("#db").html(
                "<div>"
                + "Clicks: " + msg.clicks 
                + "<br/>"
                + "Urls: " + msg.urls
                + "<br/><br/>"
                + a
                + "</div>");
        },
        error: function () {
            $("#db").html(
                "<div class='alert alert-danger lead'>ERROR</div>");
        }
    });
};


$(document).ready(
    function () {
        db();
        $("#shortener").submit(
            function (event) {
                event.preventDefault();
                db();
            });
    });


$(document).ready(
    function(){
        $("#search").submit(
            function (event) {
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
            });
    });