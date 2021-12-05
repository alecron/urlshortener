$(document).ready(
    function() {
        $("#shortener").submit(
            function(event) {
                event.preventDefault();
                var withQR = $("#qr").is(":checked")
                $.ajax({
                    type : "POST",
                    url : "/api/link",
                    data : {
                        url: $("#url").val(),
                        qr: withQR
                    } ,
                    success : function(data) {
                        $("#result").html(
                            "<div class='alert alert-success lead'><a target='_blank' href='"
                            + data.url
                            + "'>"
                            + data.url
                            + "</a></div>");

                        if (data.qr != null){
                            $("#result").prepend(
                                "<div class='alert alert-success lead'><a target='_blank' href='"
                                + data.qr
                                + "'>"
                                + data.qr
                                + "</a></div>");
                        }
                    },
                    error : function(xhr) {
                        let err = JSON.parse(xhr.responseText)
                        $("#result").html(
                            "<div class='alert alert-danger lead'>" + err.message + "</div>");
                    }
                });
            });

        $("#csvShortener").submit(
            function(event) {
                event.preventDefault();
                var withQR = $("#qrCSV").is(":checked")
                const fd = new FormData();
                const files = $('#file')[0].files;
                // Check file selected or not
                if(files.length > 0 ) {
                    fd.append('file', files[0]);
                    fd.append('qr', withQR )
                    $.ajax({
                        type: "POST",
                        url: "/csv",
                        processData: false,
                        contentType: false,
                        data: fd,
                        success: function (response, status, xhr) {
                            $("#result").html(
                                "<div class='alert alert-success lead'><a target='_blank' href='"
                                + xhr.getResponseHeader('Location')
                                + "'>"
                                + xhr.getResponseHeader('Location')
                                + "</a></div>");
                            var filename = "";
                            var disposition = xhr.getResponseHeader('Content-Disposition');
                            if (disposition && disposition.indexOf('attachment') !== -1) {
                                var filenameRegex = /filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/;
                                var matches = filenameRegex.exec(disposition);
                                if (matches != null && matches[1]) filename = matches[1].replace(/['"]/g, '');
                            }

                            //Fuente: https://www.iteramos.com/pregunta/21268/manejar-la-descarga-de-archivos-desde-el-puesto-de-ajax
                            var type = xhr.getResponseHeader('content-type');
                            var blob = new Blob([response], {type: type});

                            if(typeof window.navigator.msSaveBlob !== 'undefined' ){
                                window.navigator.msSaveBlob(blob, filename);
                            } else {
                               var URL = window.URL || window.webkitURL
                               var downloadUrl = URL.createObjectURL(blob)

                                if(filename){
                                    var a = document.createElement("a");

                                    if(typeof a.download === 'undefined'){
                                        window.location = downloadUrl;
                                    } else {
                                        a.href = downloadUrl;
                                        a.download = filename;
                                        document.body.appendChild(a);
                                        a.click();
                                    }
                                } else {
                                    window.location = downloadUrl;
                                }
                                setTimeout(function () { URL.revokeObjectURL(downloadUrl); }, 100);

                                // Fuente: https://www.iteramos.com/pregunta/21268/manejar-la-descarga-de-archivos-desde-el-puesto-de-ajax
                            }
                        },
                        error: function () {
                            $("#result").html(
                                "<div class='alert alert-danger lead'>ERROR</div>");
                        }
                    });
                }
            });
    });