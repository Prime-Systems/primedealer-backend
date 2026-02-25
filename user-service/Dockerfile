FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="Prime Dev Team"
LABEL description="User Service - Identity and credential management"

# Create non-root user for security
RUN addgroup -g 1001 -S recs && \
    adduser -u 1001 -S recs -G recs

# Set working directory
WORKDIR /app

# Copy the JAR file
COPY target/*.jar app.jar

# Set ownership
RUN chown -R recs:recs /app

# Switch to non-root user
USER recs

# JVM optimization for containers
ENV JAVA_OPTS="-XX:+UseZGC \
    -XX:+ZGenerational \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+UseContainerSupport \
    -Djava.security.egd=file:/dev/./urandom"

# Expose port
EXPOSE 8081

# Health check
HEALTHCHECK --interval=30s --timeout=10s --retries=3 \
    CMD wget -q --spider http://localhost:8081/actuator/health || exit 1

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
