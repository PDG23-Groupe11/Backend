# Use an official OpenJDK image as the base image
FROM openjdk:8-jre-alpine

# Set the working directory inside the container
WORKDIR /app

# Copy the distribution files from the build directory to the container
COPY build/install/Grocerypal-backend .

# Expose the port your Ktor app is listening on (adjust if needed)
EXPOSE 8080

# Command to run your Ktor application
CMD ["bin/Grocerypal-backend"]
