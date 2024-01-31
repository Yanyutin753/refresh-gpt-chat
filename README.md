# refresh-gpt-chat

![Docker Image Size (tag)](https://img.shields.io/docker/image-size/yangclivia/refresh-gpt-chat/latest)![Docker Pulls](https://img.shields.io/docker/pulls/yangclivia/refresh-gpt-chat)[![GitHub Repo stars](https://img.shields.io/github/stars/Yanyutin753/refresh-gpt-chat?style=social)](https://github.com/Yanyutin753/refresh-gpt-chat/stargazers)

### 不许白嫖，请给我免费的star⭐吧，十分感谢！

## 简介 
#### [refresh-gpt-chat](https://github.com/Yanyutin753/refresh-gpt-chat) 中转ninja或者pandoratoai的/v1/chat/completions接口，把refresh_token当key使用，内含hashmap,自动更新access_token,完美继承pandoraNext留下的refresh_token,支持基本所有的模型，小白也能快速使用！
#### [refresh-gpt-chat](https://github.com/Yanyutin753/refresh-gpt-chat) Intercept the /v1/chat/completions interface of ninja or pandoratoai, use the refresh_token as the key, which contains a hashmap, automatically update the access_token, perfectly inherit the refresh_token left by pandoraNext, support almost all models, even beginners can use it quickly!

-----

> ## 功能特性
> 
> * **通过refresh_token自动更新access_token**：方便使用
>   
> * **通过refresh_token作为key进行使用**：更好放入one-api里面
>   
> * **可适用于ninja、pandoratoapi项目**：反代服务，直接使用
>   
> * **自定义后缀**：防止url被滥用
>   
> * **个人部署**：保障隐私安全
> 

------------

### **环境变量**
- **启动端口号**：server.port=8081
- **URL自定义后缀(选填)**：server.servlet.context-path=/tokensTool
    * 记住前面必须加上/，例如/tokensTool,/tool等
- **refresh_token转access_token的地址**：getAccessTokenUrl=http(s)://ip+port/url/auth/refresh_token
- **反代的v1/chat路径（可以为pandora/ninja/pandoratoapi）**：chatUrl=http(s)://ip+port/url/v1/chat/completions
### **java部署详情**

```
# 先拿到管理员权限
sudo su -
# 提示你输入密码进行确认。输入密码并按照提示完成验证。
```

```
# 安装 OpenJDK 17：
sudo apt install openjdk-17-jdk
# 安装完成后，可以通过运行以下命令来验证 JDK 安装：
java -version
```

```
# 填写下面路径
cd （你的jar包的位置）
```

##### 运行程序
```
# 例如
nohup java -jar refresh-gpt-chat-0.0.1-SNAPSHOT.jar --server.port=8081 --server.servlet.context-path=/ --getAccessTokenUrl=http(s)://ip+port/url/auth/refresh_token --chatUrl=http(s)://ip+port/url/v1/chat/completions > myput.log 2>&1 &

# 等待一会 放行8081端口即可运行（自行调整）
```

### **docker部署详情**
```
# 先拉取镜像
docker pull yangclivia/refresh-gpt-chat:latest
```
#### **1.部署PandoraNext启动命令**
```
docker run -d \
  --restart=always \
  -u root \
  --name refresh-gpt-chat \
  --net=host \
  --pid=host \
  --privileged=true \
  -e JAVA_OPTS="-XX:+UseParallelGC -Xms128m -Xmx128m -XX:MaxMetaspaceSize=128m" \ # 设置JVM参数（可适当调节，用copilot可以适当调大点，具体可问gpt了解）
  yangclivia/refresh-gpt-chat:latest \
  --log=info
  --server.port=8081 \
  --server.servlet.context-path=/ 
  --getAccessTokenUrl=http(s)://ip+port/url/auth/refresh_token
  --chatUrl=http(s)://ip+port/url/v1/chat/completions

```
----------
### **Docker Compose部署详情**
#### **代码模板**
```
version: '3'
services:
  refresh-gpt-chat:
    image: yangclivia/refresh-gpt-chat:latest
    # Java 的环境变量 （可适当调节，用copilot可以适当调大点，具体可问gpt了解）
    environment:  
      - JAVA_OPTS=-XX:+UseParallelGC -Xms128m -Xmx128m -XX:MaxMetaspaceSize=128m  
    container_name: refresh-gpt-chat
    restart: always
    user: root
    network_mode: host
    pid: host
    privileged: true
    command:
      - --log=info
      - --server.port=8081
      - --server.servlet.context-path=/
      - --getAccessTokenUrl=http(s)://ip+port/url/auth/refresh_token
      - --chatUrl=http(s)://ip+port/url/v1/chat/completions
```

##### 启动refresh-gpt-chat
```
cd (你的docker-compose.yml位置)

docker-compose up -d
```

##### 更新refresh-gpt-chat项目代码
```
cd (你的docker-compose.yml位置)

docker-compose pull

docker-compose up -d
```
--------

> [!important]
>
> * 本项目只提供转发接口🥰
> * 开源项目不易，请点个星星吧！！！
