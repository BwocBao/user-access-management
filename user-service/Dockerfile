##Cách 1: Docker tự build
## Stage 1: build
##Pull image maven và jdk 17 về và Đặt tên stage là builder
#FROM maven:3.9.6-eclipse-temurin-17-alpine as builder
##Tạo thư mục /app trong container Và cd /app (mọi lệnh sau chạy trong đây)
#WORKDIR /app
##Copy pom vào thư mục hiện tại(Thư mục hiện tại là thư mục app vì đã di chuyển tới nó)
## copy pom trước để cache dependency
#COPY pom.xml .
#COPY core/pom.xml core/pom.xml
#COPY auth-service/pom.xml auth-service/pom.xml
#COPY user-service/pom.xml user-service/pom.xml
##-B (batch): Chạy không cần tương tác (bắt buộc trong Docker). Nếu dependency cần tải → tải luôn. Không có prompt hỏi
##Dùng cơ chế cache tải về trước dependency lưu vào ~/.m2/repository nếu trong quá trình code có thay đổi src
##thì khi build lại nó sẽ ko cần tải lại dependency giúp tiết kiệm tời gian
#RUN mvn -B dependency:go-offline
##Copy src vào trong thư mục src của thư mục hiện tại (app)
## copy source
#COPY core core
#COPY auth-service auth-service
#COPY user-service user-service
#
## chỉ build user-service + dependency (đóng gói thành jar)
##-pl user-service   :chỉ build module user-service
##-am   :nhưng build luôn các module nó phụ thuộc ví dụ core và bỏ qua auth-service
#RUN mvn -pl user-service -am -DskipTests package
#
## Stage 2: run
##chỉ cần pull jre
#FROM eclipse-temurin:17-jre-alpine
##tạo group và user riêng
#RUN addgroup -S userservicegroup
#RUN adduser -S userserviceuser -G userservicegroup
##tạo thư mục /app code sẽ chạy trong đây
#WORKDIR /app
##copy file jar từ stage 1(builder) qua thư mục app đặt tên là user-service.jar
#COPY --from=builder /app/user-service/target/*.jar user-service.jar
##đổi qua user userserviceuser để chạy app đảm bảo security ko sài root
#RUN chown -R userserviceuser:userservicegroup /app
#USER userserviceuser
##port mà app chạy(tượng trưng)
#EXPOSE 8082
##lệnh chạy app
#ENTRYPOINT ["java","-jar","user-service.jar"]
#



##Cách 2: Docker chỉ copy jar đc build sẵn từ CI
FROM eclipse-temurin:17-jre-alpine

RUN addgroup -S usergroup && adduser -S useruser -G usergroup

WORKDIR /app

COPY user-service/target/app.jar user-service.jar

RUN chown -R useruser:usergroup /app

USER useruser

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "user-service.jar"]






#Cách 2:
#FROM eclipse-temurin:17-jdk-jammy
#
#WORKDIR /app
#COPY target/*.jar user-service.jar
#
## Expose the port the app runs on
#EXPOSE 8082
## Run the jar file
#ENTRYPOINT ["java", "-jar", "user-service.jar"]
