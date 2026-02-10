
# Java 21을 사용하는 이미지
FROM eclipse-temurin:21-jre-alpine

# 빌드한 jar파일을 컨테이너에 복사
COPY build/libs/*.jar app.jar

# 도커에게 컨테이너가 8080 포트를 외부에 노출할 것이라고 알려주는 명령어
EXPOSE 8080

# 컨테이너가 시작되면 실행할 명령어 마지막 명령어는 application-aws.yml 같은 파일이 따로 없으면 안해도 됨
ENTRYPOINT ["java", "-jar", "app.jar"]