#
# Nginx Dockerfile
#

# Pull base image.
FROM ubuntu:14.04

# Install Nginx.
RUN \
apt-get update && \
apt-get install -y software-properties-common python-software-properties && \
add-apt-repository -y ppa:nginx/stable && \
apt-get update && \
apt-get install -y nginx && \
rm -rf /var/lib/apt/lists/* && \
echo "\ndaemon off;" >> /etc/nginx/nginx.conf && \
chown -R www-data:www-data /var/lib/nginx

# Define working directory.
WORKDIR /etc/nginx

# env var
ARG NGINX_SERVER_ADDR
ENV NGINX_SERVER_ADDR $NGINX_SERVER_ADDR

# move over files
ADD target/build/ /var/www/witan-ui
ADD log-format.conf /etc/nginx/conf.d/log-format.conf
ADD start-nginx.sh /start-nginx

# have the nginx log be piped to stdout
RUN ln -sf /dev/stdout /var/log/nginx/access.log \
    && ln -sf /dev/stderr /var/log/nginx/error.log

# Define default command.
CMD ["/bin/bash","/start-nginx"]

# Expose ports.
EXPOSE 80
EXPOSE 443
