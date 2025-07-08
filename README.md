# MeliShortUrlApi

## Overview
MeliShortUrlApi is a Spring Boot application that provides a simple API for creating and managing short URLs. The application allows users to shorten long URLs and retrieve the original URLs using the generated short links.

## Features
- Create short URLs from long URLs
- Retrieve original URLs using short links
- RESTful API design

## Project Structure
```
MeliShortUrlApi
├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── melishorturlapi
│   │   │           ├── MeliShortUrlApiApplication.java
│   │   │           ├── controller
│   │   │           ├── service
│   │   │           └── model
│   │   └── resources
│   │       ├── application.properties
│   │       └── static
│   └── test
│       └── java
│           └── com
│               └── melishorturlapi
│                   └── MeliShortUrlApiApplicationTests.java
├── pom.xml
└── README.md
```

## Getting Started

### Starting DynamoDB
```
docker run --rm -p 8000:8000 amazon/dynamodb-local:latest -jar DynamoDBLocal.jar -sharedDb
```

### Start Redis
```
brew services start redis
brew services info redis
```

### Start Grafana
```
docker run -d -p 3000:3000 grafana/grafana
```

### Prerequisites
- Java 11 or higher
- Maven

### Installation
1. Clone the repository:
   ```
   git clone <repository-url>
   ```
2. Navigate to the project directory:
   ```
   cd MeliShortUrlApi
   ```
3. Build the project using Maven:
   ```
   mvn clean install
   ```

### Running the Application

## Running locally 
Start services
```
docker-compose up otel-collector prometheus grafana jaeger loki
```

To run the application, use the following command:
```
mvn spring-boot:run
```

### Debug 
```
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
```

## Run dockerized
All services will run in docker context
```
docker-compose up --build 
```

### API Endpoints
- **POST /urls**: Create a short URL
- **GET /urls/{shortUrl}**: Retrieve the original URL
- **GET /{shortUrl}**: Navigate to a short URL (redirect to original URL)

## Contributing
Contributions are welcome! Please open an issue or submit a pull request for any enhancements or bug fixes.

## License
This project is licensed under the MIT License. See the LICENSE file for details.