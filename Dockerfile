FROM gradle:jdk21
WORKDIR /home/products-management
COPY . .
RUN gradle build -x test

# taking build result to opt folder where the third party software usually hosted on.
WORKDIR /home/products-management/build/libs
RUN cp $(ls | grep -v "plain") /opt/backend.jar
RUN rm -fr /home/products-management
WORKDIR /opt
CMD ["java", "-jar", "backend.jar"]