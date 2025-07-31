# Используем JDK (включает JRE + компилятор)
FROM openjdk:21-jdk-slim

# Устанавливаем рабочую директорию
WORKDIR /app

# Копируем файлы сборки
COPY pom.xml .
COPY core/pom.xml core/
COPY service-api/pom.xml service-api/
COPY service/pom.xml service/
COPY dao/pom.xml dao/

# Устанавливаем Maven и загружаем зависимости
RUN apt update && \
    apt install -y maven && \
    mvn dependency:go-offline -B

# Копируем исходный код
COPY . .

# Собираем JAR
RUN mvn clean package -DskipTests

# Открываем порт
EXPOSE 8080

# Запускаем приложение
CMD ["java", "-jar", "core/target/core.jar"]
