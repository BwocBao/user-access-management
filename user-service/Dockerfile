# Stage 1: build
#Pull image maven và jdk 17 về và Đặt tên stage là builder
FROM maven:3.9.6-eclipse-temurin-17-alpine as builder
#Tạo thư mục /app trong container Và cd /app (mọi lệnh sau chạy trong đây)
WORKDIR /app
#Copy pom vào thư mục hiện tại(Thư mục hiện tại là thư mục app vì đã di chuyển tới nó)
COPY pom.xml .
#-B (batch): Chạy không cần tương tác (bắt buộc trong Docker). Nếu dependency cần tải → tải luôn. Không có prompt hỏi
#Dùng cơ chế cache tải về trước dependency lưu vào ~/.m2/repository nếu trong quá trình code có thay đổi src
#thì khi build lại nó sẽ ko cần tải lại dependency giúp tiết kiệm tời gian
RUN mvn -B dependency:go-offline
#Copy src vào trong thư mục src của thư mục hiện tại (app)
COPY src ./src
#Đóng gói thành file jar
RUN mvn -B package -DskipTests

# Stage 2: run
#chỉ cần pull jre
FROM eclipse-temurin:17-jre-alpine
#tạo group và user riêng
RUN addgroup -S userservicegroup
RUN adduser -S userserviceuser -G userservicegroup
#tạo thư mục /app code sẽ chạy trong đây
WORKDIR /app
#copy file jar từ stage 1(builder) qua thư mục app đặt tên là auth-service.jar
COPY --from=builder /app/target/*.jar user-service.jar
#đổi qua user authserviceuser để chạy app đảm bảo security ko sài root
RUN chown -R userserviceuser:userservicegroup /app
USER userserviceuser
#port mà app chạy(tượng trưng)
EXPOSE 8082
#lệnh chạy app
ENTRYPOINT ["java","-jar","user-service.jar"]