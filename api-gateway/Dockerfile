FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Prime Dev Team"
LABEL description="API Gateway - Single entry point for microservices"

# Create non-root user for security
RUN addgroup -g 1001 -S prime && \
    adduser -u 1001 -S prime -G prime

# Set working directory
WORKDIR /app

# Copy the JAR file
COPY target/*.jar app.jar

# Set ownership
RUN chown -R prime:prime /app

# Switch to non-root user
USER prime

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseZGC \
    -XX:+ZGenerational \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseContainerSupport \
    -Djava.security.egd=file:/dev/./urandom \
    -Dreactor.netty.pool.leasingStrategy=lifo"

# Expose port
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget -q --spider http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
