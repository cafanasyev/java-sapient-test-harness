IMAGE := cafanasyev/java-sapient-test-harness:latest

.PHONY: package image push publish

# Build the application JAR (skipping tests)
package:
	./mvnw clean package -DskipTests

# Build the Docker image
image: package
	docker build -t $(IMAGE) .

# Push the Docker image to Docker Hub
push:
	docker push $(IMAGE)

# Build and publish the image
publish: image push