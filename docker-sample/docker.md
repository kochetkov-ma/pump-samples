Допустим вы хотите протестировать приложение `docker/getting-started` и у вас машина без chromedriver и браузера Chrome
и вообще без интерфейса, но установлен Docker и docker-compose.

##### В файле `docker-compose.yml` описывается стенд приложение + браузер

```yaml
services:
  browser:
    image: selenium/standalone-chrome:85.0
    ports:
      - 4444:4444

  testing-application:
    image: docker/getting-started
    ports:
      - 80:80
```

##### Запускаете этот файл `docker-compose -f docker-compose.yml up -d`

##### Вы можете зайти в приложение по `http://localhost:80/`, а к браузеру можете подключиться через RemoteWebDriver по `http://localhost:4444/wd/hub`

##### Между контейнерами в сети docker-compose DNS имена используются сервисов, то есть чтобы из контейнера браузера попасть на `docker/getting-started` нужно вводить в браузере `http://testing-application:80`

Пример, правда на
котлин [github.com/kochetkov-ma/pump-samples/tree/master/docker-sample](https://github.com/kochetkov-ma/pump-samples/tree/master/docker-sample)  
Достаточно просто запустить `gradle docker-sample:test` и если у вас установлен Docker + Docker-Compose и есть доступ до
Docker Hub, то TestContainer все сам скачает и запустит `docker-compose.yml`, а потом убьет


