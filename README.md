# refresh-gpt-chat

![Docker Image Size (tag)](https://img.shields.io/docker/image-size/yangclivia/refresh-gpt-chat/latest)![Docker Pulls](https://img.shields.io/docker/pulls/yangclivia/refresh-gpt-chat)[![GitHub Repo stars](https://img.shields.io/github/stars/Yanyutin753/refresh-gpt-chat?style=social)](https://github.com/Yanyutin753/refresh-gpt-chat/stargazers)

### 不许白嫖，请给我免费的star⭐吧，十分感谢！

## 简介 
#### [refresh-gpt-chat](https://github.com/Yanyutin753/refresh-gpt-chat) 中转ninja或者PandoraToV1Api的/v1/chat/completions和v1/images/generations接口，把refresh_token当key使用，内含hashmap,自动更新access_token,完美继承pandoraNext留下的refresh_token,支持基本所有的模型，小白也能快速使用！
#### [refresh-gpt-chat](https://github.com/Yanyutin753/refresh-gpt-chat) Intercept the /v1/chat/completions and v1/images/generations interface of ninja or PandoraToV1Api, use the refresh_token as the key, which contains a hashmap, automatically update the access_token, perfectly inherit the refresh_token left by pandoraNext, support almost all models, even beginners can use it quickly!

-----

> ## 功能特性
> 
> * **通过refresh_token自动更新access_token**：方便使用
>   
> * **通过refresh_token作为key进行使用**：更好放入one-api里面
>
> * **支持反代v1/images/generations接口**：调用dall-e-3画图更出色
>   
> * **可适用于ninja、PandoraToV1Api项目**：反代服务，直接使用
>   
> * **自定义后缀**：防止url被滥用
>
> * **支持base64识图**：能转发识图接口
> 
> * **回复打字机处理**：回复更流畅，减少卡顿
> 
> * **个人部署**：保障隐私安全
>

## [✨点击查看文档站](https://apifox.com/apidoc/shared-4b9a7517-3f80-47a1-84fc-fcf78827a04a)

<details>
<summary>

     简略文档，请点击上面连接跳转详细使用部署文档站
</summary>

### **环境变量**
- **启动端口号**：server.port=8081
- **URL自定义后缀(选填)**：server.servlet.context-path=/tokensTool
    * 记住前面必须加上/，例如/tokensTool,/tool等
- **refresh_token转access_token的地址**：getAccessTokenUrl=http(s)://ip+port或者域名/auth/refresh_token
- **自定义的/v1/chat/completions接口**（可以为**ninja**/**PandoraToV1Api**/复活的**pandora**等能够通过access_token进行对话的url接口）：
     - **chatUrl**=http(s)://ip+port或者域名/v1/chat/completions
- **ninja的/v1/chat/completions接口**（可以为**ninja**/**PandoraToV1Api**/复活的**pandora**等能够通过access_token进行对话的url接口）
     - **ninja_chatUrl**=http(s)://ip+port或者域名/v1/chat/completions

- ⚠**chatUrl和ninja_chatUrl都是可以通过access_token直接使用的/v1/chat/completions接口**
    - 1.写两个的目的是为了反代多个，而不是单单一个，你可以选择ninja_chatUrl反代ninja的/v1/chat/completions，chatUrl反代PandoraToV1Api的/v1/chat/completions。
    - 2.他们唯一的区别就是chatUrl在你部署的ninja_chatUrl服务的/v1/chat/completions接口请求，而ninja_chatUrl在你部署的ninja_chatUrl服务的ninja/v1/chat/completions端口请求
      
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
nohup java -jar refresh-gpt-chat-0.0.1-SNAPSHOT.jar --server.port=8081 --server.servlet.context-path=/ --getAccessTokenUrl=http(s)://ip+port/url/auth/refresh_token --chatUrl=http(s)://ip+port或者域名/v1/chat/completions --ninja_chatUrl=http(s)://ip+port或者域名/v1/chat/completions（选填）> myput.log 2>&1 &

# 等待一会 放行8081端口即可运行（自行调整）
```

### **docker部署详情**
```
# 先拉取镜像
docker pull yangclivia/refresh-gpt-chat:latest
```
#### **1.部署refresh-gpt-chat启动命令**
```
docker run -d \
  --restart=always \
  -u root \
  --name refresh-gpt-chat \
  --net=host \
  --pid=host \
  --privileged=true \
  -e JAVA_OPTS="-XX:+UseParallelGC -Xms128m -Xmx128m -XX:MaxMetaspaceSize=128m" \ # 设置JVM参数（可适当调节，并发高可以适当调大点，具体可问gpt了解）
  yangclivia/refresh-gpt-chat:latest \
  --log=info
  --server.port=8081 \
  --server.servlet.context-path=/ 
  --getAccessTokenUrl=http(s)://ip+port/url/auth/refresh_token
  --chatUrl=http(s)://ip+port或者域名/v1/chat/completions
  --ninja_chatUrl=http(s)://ip+port或者域名/v1/chat/completions（选填）

```
----------
### **Docker Compose部署详情**
#### **代码模板**
```
version: '3'
services:
  refresh-gpt-chat:
    image: yangclivia/refresh-gpt-chat:latest
    # Java 的环境变量 （可适当调节，并发高可以适当调大点，具体可问gpt了解）
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
      - --chatUrl=http(s)://ip+port或者域名/v1/chat/completions
      - --ninja_chatUrl=http(s)://ip+port或者域名/v1/chat/completions（选填）
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
</details>

> [!important]
>
> * 本项目只提供转发接口🥰
> * 开源项目不易，请点个星星吧！！！

### 新增群聊，点了⭐️可以进群讨论部署，我把你们拉进群，无广，广子踢掉
<img src="https://github.com/Yanyutin753/PandoraNext-TokensTool/assets/132346501/6544e8ed-6673-48f9-95a6-c13255acbab1" width="300" height="300">

### 请给我一个免费的⭐吧！！！

## Star History

[![Star History Chart](https://api.star-history.com/svg?repos=Yanyutin753/refresh-gpt-chat&type=Date)](https://star-history.com/#Yanyutin753/refresh-gpt-chat&Date)
