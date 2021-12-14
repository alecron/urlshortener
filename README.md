# Getting Started with the URL Shortener project

## Overall structure

The structure of this project is heavily influenced by 
[the clean architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html):

* A `core` module where we define the domain entities and the functionalities
  (also known as uses cases, business rules, etc.). They do not know that this application 
  has a web interface or that data is stored in relational databases.
* A `repositories` module that knows how to store domain entities in a relational database.
* A `delivery` module that knows how to expose in the Web the functionalities. 
* An `app` module that contains the main, the configuration (i.e. it links `core`, `delivery` and `repositories`), 
  and the static assets (i.e. html files, JavaScript files, etc. )

Usually, if you plan to add a new feature, usually:

* You will add a new use case to the `core` module.
* If required, you will modify the persistence model in the `repositories` module.
* You will implement a web-oriented solution to expose to clients in the `delivery` module.

Sometimes, your feature will not be as simple, and it would require:

* To connect a third party (e.g. an external server). 
  In this case you will add a new module named `gateway` responsible for such task.
* An additional application.  
  In this case you can create a new application module (e.g. `app2`) with the appropriate configuration to run this second server.

Features that require the connection to a third party or having more than a single app will be rewarded. 

## Run

The application can be run as follows:

```shell
./gradlew :app:bootRun
```

Now you have a shortener service running at port 8080. You can test that it works as follows:

```shell
$ curl -v -d "url=http://www.unizar.es/" http://localhost:8080/api/link
*   Trying ::1:8080...
* Connected to localhost (::1) port 8080 (#0)
> POST /api/link HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.71.1
> Accept: */*
> Content-Length: 25
> Content-Type: application/x-www-form-urlencoded
> 
* upload completely sent off: 25 out of 25 bytes
* Mark bundle as not supporting multiuse
< HTTP/1.1 201 
< Location: http://localhost:8080/tiny-6bb9db44
< Content-Type: application/json
< Transfer-Encoding: chunked
< Date: Tue, 28 Sep 2021 17:06:01 GMT
< 
* Connection #0 to host localhost left intact
{"url":"http://localhost:8080/tiny-6bb9db44","properties":{"safe":true}}%   
```

And now, we can navigate to the shortened URL.

```shell
$ curl -v http://localhost:8080/tiny-6bb9db44
*   Trying ::1:8080...
* Connected to localhost (::1) port 8080 (#0)
> GET /tiny-6bb9db44 HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.71.1
> Accept: */*
> 
* Mark bundle as not supporting multiuse
< HTTP/1.1 307 
< Location: http://www.unizar.es/
< Content-Length: 0
< Date: Tue, 28 Sep 2021 17:07:34 GMT
< 
* Connection #0 to host localhost left intact
```

## Build and Run

The uberjar can be built and then run with:

```shell
./gradlew build
java -jar app/build/libs/app.jar
```

## Functionalities

The project offers a minimum set of functionalities:

* **Create a short URL**. 
  See in `core` the use case `CreateShortUrlUseCase` and in `delivery` the REST controller `UrlShortenerController`.

* **Redirect to a URL**.
  See in `core` the use case `RedirectUseCase` and in `delivery` the REST controller `UrlShortenerController`.

* **Log redirects**.
  See in `core` the use case `LogClickUseCase` and in `delivery` the REST controller `UrlShortenerController`.

The objects in the domain are:

* `ShortUrl`: the minimum information about a short url
* `Redirection`:  the remote URI and the redirection mode
* `ShortUrlProperties`: a handy way to extend data about a short url
* `Click`: the minimum data captured when a redirection is logged
* `ClickProperties`: a handy way to extend data about a click

## Delivery

The above functionality is available through a simple API:

* `POST /api/link` which creates a short URL from data send by a form.
* `GET /tiny-{id}` where `id` identifies the short url, deals with redirects, and logs use (i.e. clicks).

In addition, `GET /` returns the landing page of the system. 

## F1. Service that given a shortened URL returns a QR.

### Funcionality
  The funcionality of F1 is R3.
  
  R3: The shortened URI can be obtained encoded in a QR code

### POST /link

* POST /link will support an optional parameter (qr) by which it will be indicated if there will or won't be a QR code representation of the shortened URI. If it isn't not present, it will be understood that there won't be a QR code. The http response will contain a property in the JSON that will have the complete URI address of the QR code.

#### Example without qr parameter:

```shell
$ curl -v -d "url=http://www.unizar.es/" http://localhost:8080/api/link
*   Trying 127.0.0.1...
* TCP_NODELAY set
* Connected to localhost (127.0.0.1) port 8080 (#0)
> POST /api/link HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.58.0
> Accept: */*
> Content-Length: 25
> Content-Type: application/x-www-form-urlencoded  
>
* upload completely sent off: 25 out of 25 bytes   
< HTTP/1.1 201
< Location: http://localhost:8080/tiny-6bb9db44
< Content-Type: application/json
< Transfer-Encoding: chunked
< Date: Tue, 09 Nov 2021 18:06:31 GMT
<
* Connection #0 to host localhost left intact
{"url":"http://localhost:8080/tiny-6bb9db44","qr":null,"properties":{"safe":true}}
```
If you don't add de qr parameter, the http response will be the same as getting a shortened url

#### Example with qr parameter and without format parameters:

```shell
$ curl -v -d "url=http://www.unizar.es/&qr=true" http://localhost:8080/api/link
*   Trying 127.0.0.1...
* TCP_NODELAY set
* Connected to localhost (127.0.0.1) port 8080 (#0)
> POST /api/link HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.58.0
> Accept: */*
> Content-Length: 33
> Content-Type: application/x-www-form-urlencoded  
>
* upload completely sent off: 33 out of 33 bytes   
< HTTP/1.1 201
< Location: http://localhost:8080/tiny-6bb9db44
< Content-Type: application/json
< Transfer-Encoding: chunked
< Date: Tue, 09 Nov 2021 17:59:29 GMT
<
* Connection #0 to host localhost left intact
{"url":"http://localhost:8080/tiny-6bb9db44","qr":"http://localhost:8080/qr/6bb9db44","properties":{"safe":true}}
```
If only the parameter qr = true appears and it doesn't appear format parameters, then the generated QR code will have the default format (height=500, width=500, color=black, background=white, typeImage=PNG, errorCorrectionLevel=L).

If you click on the qr URI address, you can see the QR code. 
In this case, the QR code generated has the default format and is the following one:
![alt text](QRCode6bb9db44.png)

#### Example with qr parameter and with format parameters:

```shell
curl -v -d "url=http://www.unizar.es/&qr=true&qrColor=0xFFFF6666&qrBackground=0xFFFFCCCC" http://localhost:8080/api/link
*   Trying 127.0.0.1...
* TCP_NODELAY set
* Connected to localhost (127.0.0.1) port 8080 (#0)
> POST /api/link HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.58.0
> Accept: */*
> Content-Length: 52
> Content-Type: application/x-www-form-urlencoded
>
* upload completely sent off: 52 out of 52 bytes
< HTTP/1.1 201
< Location: http://localhost:8080/tiny-6bb9db44
< Content-Type: application/json
< Transfer-Encoding: chunked
< Date: Tue, 14 Dec 2021 00:07:14 GMT
<
* Connection #0 to host localhost left intact
{"url":"http://localhost:8080/tiny-6bb9db44","qr":"http://localhost:8080/qr/6bb9db44","properties":{"safe":true}}
```
If the parameter qr = true appears and also any parameter relative to the qr code format appears, then the code will be generated with the format specified on that parameters.

If you click on the qr URI address, you can see the QR code.
In this case, using that parameters, the QR code has a specified format and is the following one:
![alt text](QRCode6bb9db44WithFormat.png)

### GET /qr/{hash}

* GET /qr/{hash} returns an image with the correct content type; it must correspond to the URI obtained in the previous method. If the hash does not exist in the database or cannot be done a redirect with that hash for whatever reason (for example, not validated), the appropriate 400 type error will be returned.

```shell
$ curl -v http://localhost:8080/qr/6bb9db44 --output QRCode6bb9db44.png
  % Total    % Received % Xferd  Average Speed   Time    Time 
    Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
  0     0    0     0    0     0      0      0 --:--:-- --:--:-- --:--:--     0*   Trying 127.0.0.1...
* TCP_NODELAY set
* Connected to localhost (127.0.0.1) port 8080 (#0)
> GET /qr/6bb9db44 HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.58.0
> Accept: */*
>
< HTTP/1.1 200 
< Content-Type: image/png
< Content-Length: 11722
< Date: Tue, 14 Dec 2021 00:17:31 GMT
<
{ [11722 bytes data]
100 11722  100 11722    0     0   121k      0 --:--:-- --:--:-- --:--:--  124k
* Connection #0 to host localhost left intact
```
If the previous POST request has already been processed at the moment of the GET request, then the QR code will have the format which has been specified on that previous POST request. Otherwise, the QR code will be generated at the moment of the GET request and will have the default format (height=500, width=500, color=black, background=white, typeImage=PNG, errorCorrectionLevel=L).

The output of the GET request has been output to the file QRCode6bb9db44.png.
If you open this file, you can see the QR code.

### FALTA DE HACER
* Cambiar tipo de dato de rabbit por el de QRCode y quitar QRCode2.
* Añadir validatorService al hacer la peticion de post
* Añadir ESTADO en el mensaje guardado en bd para poder coger el formato cuando no se ha consumido el mensaje por rabbit y autogenerarlo con dicho formato.
* Hacer tests para todos los casos:
  * Comprobando que genera el codigo qr por defecto cuando no ha sido generado aun por rabbit y quitar wait.
  * Comprobar peticion POST sin parametro qr
  * Comprobar peticion POST con qr = true, GET debe devolver un qr con el formato por defecto
  * Comprobar peticion POST con qr = true y un formato especifico, GET debe devolver un qr con dicho formato
  * Comprobar: Si alguna de las dos peticiones anteriores devuelve errores de tipo 400 por más de un motivo deberá devolver un objeto JSON especificando el motivo concreto del error el usuario. 

    Ejemplo:
    {
     “error”: “URI de destino no validada todavía”
    }

## Repositories

All the data is stored in a relational database. 
There are only two tables.

* **shorturl** that represents short url and encodes in each row `ShortUrl` related data 
* **click** that represents clicks and encodes in each row `Click` related data

## Reference Documentation

For further reference, please consider the following sections:

* [Official Gradle documentation](https://docs.gradle.org)
* [Spring Boot Gradle Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.5.5/gradle-plugin/reference/html/)
* [Spring Web](https://docs.spring.io/spring-boot/docs/2.5.5/reference/htmlsingle/#boot-features-developing-web-applications)
* [Spring Data JPA](https://docs.spring.io/spring-boot/docs/2.5.5/reference/htmlsingle/#boot-features-jpa-and-spring-data)

## Guides

The following guides illustrate how to use some features concretely:

* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/bookmarks/)
* [Accessing Data with JPA](https://spring.io/guides/gs/accessing-data-jpa/)

