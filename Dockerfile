# 从 sapmachine:17.0.10-jre-ubuntu-jammy 镜像开始构建
FROM sapmachine:17.0.10-jre-ubuntu-jammy

# 指定维护者的名字
LABEL maintainer="Yanyutin753"

# 切换到 root 用户
USER root

# 设置环境变量，确保字符编码为UTF-8
ENV LANG C.UTF-8

# 复制JAR文件到容器的/app目录下
COPY refresh-gpt-chat-0.3.0.jar /app/app.jar

# 切换到/app目录
WORKDIR /app

# 声明服务运行在8081端口
EXPOSE 8081

# 指定docker容器启动时运行jar包，适当减少JVM内存分配
ENTRYPOINT exec java $JAVA_OPTS -Dfile.encoding=UTF-8 -jar app.jar $@
